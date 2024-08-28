package com.ksyun.campus.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.campus.client.domain.MetaServerMsg;
import com.ksyun.campus.client.domain.ReplicaData;
import com.ksyun.campus.client.domain.WriteResponse;
import com.ksyun.campus.client.server.FileSystemService;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FSOutputStream extends OutputStream {
    private String path;
    private String defaultFileSystemName;
    private List<ReplicaData> dataServerMsgList;
    private ReplicaData dataServerMsgBac;
    private List<byte[]> memoryBuffer; //字节数组集合
    private int memoryBufferSize;
    private static final int MEMORY_BUFFER_SIZE = 1024 * 1024*100; // 1MB
    private ExecutorService executorService;
    private int flag = 0; //是否是本次写的最后一次callRemote，1是，0否
    private List<String> statInfoSingles = new ArrayList<>();
    private FileSystemService fileSystemService = new FileSystemService();
    private String md5;

    public FSOutputStream(String defaultFileSystemName,String path, List<ReplicaData> dataServerMsgList, ReplicaData dataServerMsgBac) {
        this.path = path;
        this.defaultFileSystemName=defaultFileSystemName;
        this.dataServerMsgList = dataServerMsgList;
        this.dataServerMsgBac = dataServerMsgBac;
        this.memoryBuffer = new ArrayList<>();
        this.memoryBufferSize = 0;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void write(int b) throws IOException {
        byte[] byteArr = { (byte) b };
        write(byteArr, 0, 1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    //将数据分块写入内存缓冲区 memoryBuffer，以防止一次性写入过大的数据量。当内存缓冲区的大小达到1MB时才进行一次callRemote
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        int remainingBytes = len;
        int offset = off;

        while (remainingBytes > 0) {
            int bytesToWrite = Math.min(remainingBytes, MEMORY_BUFFER_SIZE - memoryBufferSize);

            byte[] buffer = new byte[bytesToWrite];
            System.arraycopy(b, offset, buffer, 0, bytesToWrite);

            memoryBuffer.add(buffer); //字节数组集合
            memoryBufferSize += bytesToWrite;
            offset += bytesToWrite;
            remainingBytes -= bytesToWrite;

            if (memoryBufferSize >= MEMORY_BUFFER_SIZE) {
                System.out.println("memory buffer overflow");
                try {
                    flushMemoryBuffer();
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    //关闭流。将当前对象的 flushRemainingData() 方法作为任务提交给执行器服务

    @Override
    public void close() throws IOException {
        flag=1;
        try {
            flushMemoryBuffer();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
//        executorService.submit(this::flushRemainingData);//通过将任务提交给执行器服务，可以异步地执行，而不会阻塞当前的关闭操作
//        executorService.shutdown(); //优雅地关闭执行器服务，执行器服务将不再接受新的任务，但会继续执行已经提交的任务直至完成
        super.close();
    }

    //将内存缓冲区中的数据发送到dataServer，将缓冲区中的字节数组合并为一个字节数组
    private void flushMemoryBuffer() throws NoSuchAlgorithmException {
        if (!memoryBuffer.isEmpty()) {
            byte[] mergedBuffer = mergeBuffers(memoryBuffer);
            callRemote(mergedBuffer);
            memoryBuffer.clear();
            memoryBufferSize = 0;
        }
    }

    //在流关闭时，将剩余的内存缓冲区数据发送到dataServer并清空缓冲区
    private void flushRemainingData() throws NoSuchAlgorithmException {
        //无论memoryBuffer是否有数据都再发一次请求，做个flag标记
        flag = 1;
        byte[] mergedBuffer = mergeBuffers(memoryBuffer);
        if (mergedBuffer.length>0)
        {
            callRemote(mergedBuffer);
            memoryBuffer.clear();
            memoryBufferSize = 0;
        }
    }

    //将多个字节数组缓冲区合并成一个字节数组
    private byte[] mergeBuffers(List<byte[]> buffers) {
        int totalSize = 0;
        for (byte[] buffer : buffers) {
            totalSize += buffer.length;
        }
        byte[] mergedBuffer = new byte[totalSize];
        int destPos = 0;
        for (byte[] buffer : buffers) {
            System.arraycopy(buffer, 0, mergedBuffer, destPos, buffer.length);
            destPos += buffer.length;
        }
        return mergedBuffer;
    }

    private void callRemote(byte[] data) throws NoSuchAlgorithmException {
        md5=calculateMD5(data);
        for (ReplicaData dataServerMsg : dataServerMsgList) {
//            //测一下挂掉一个dataServer后的反应
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }

            // 构建请求路径
            String newData=new String(data, StandardCharsets.UTF_8);
            String url="http://" + dataServerMsg.getDsNode() + "/data/write?path=" + path+"&offset=0&length="+data.length;
            FileSystem fileSystem = new FileSystem(this.defaultFileSystemName);
            try {
                String responseBody = fileSystem.callRemote("post", url, data);
                if(flag == 1){
                    statInfoSingles.add(responseBody);
                    ObjectMapper objectMapper = new ObjectMapper();
                    WriteResponse dataServerInfo = objectMapper.readValue(responseBody, WriteResponse.class);
                    //保存元信息
                    recallMetaserver(dataServerInfo.getData().getFileSize(),md5);
                }
            } catch (Exception e) {
                System.out.println("ops中发送callRemote出现异常");
            }
        }
    }

    //向metaServer发送请求保存元信息
    public void recallMetaserver(int length,String md5) throws Exception {
        MetaServerMsg metaServer = fileSystemService.getMetaServer();
        String metaServerUrl = "http://" + metaServer.getHost() + ":" + metaServer.getPort() + "/write"
                + "?path=" + path+"&offset=0&length="+length+"&md5="+md5;
        FileSystem fileSystem = new FileSystem(this.defaultFileSystemName);
        String get = fileSystem.callRemote("get", metaServerUrl, null);
    }

    private String calculateMD5(byte[] content) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(content);
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}