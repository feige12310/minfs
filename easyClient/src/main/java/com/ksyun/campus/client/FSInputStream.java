package com.ksyun.campus.client;

import com.ksyun.campus.client.domain.ReplicaData;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public class FSInputStream extends InputStream {

    private List<ReplicaData> replicaData;
    private int currentReplicaIndex;
    private String defaultFileSystemName;
    private long filesize;
    private String MD5;

    public FSInputStream() {
    }

    public FSInputStream(List<ReplicaData> replicaData, String defaultFileSystemName, long size, String MD5) {
        this.replicaData = replicaData;
        this.defaultFileSystemName = defaultFileSystemName;
        this.currentReplicaIndex = 0;
        this.filesize = size;
        this.MD5 = MD5;
    }

    @Override
    public int read() throws IOException {
        byte[] buffer = new byte[1];
        int bytesRead = read(buffer);
        if (bytesRead == -1) {
            return -1;
        }
        return buffer[0] & 0xFF;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (currentReplicaIndex >= replicaData.size()) {
            return -1; // No more replicas to read from
        }

        ReplicaData currentReplica = replicaData.get(currentReplicaIndex);
        String dsNode = currentReplica.getDsNode();
        String path = currentReplica.getPath();

        if (len > filesize) {
            len = (int) filesize;
        }
        String url = "http://" + dsNode + "/data/read?path=" + path + "&offset=" + off + "&length=" + len;
        FileSystem fileSystem = new FileSystem(defaultFileSystemName);
        try {
            byte[] responseData = fileSystem.callRemote("get", url, null).getBytes();
            if (responseData != null) {
                int bytesRead = responseData.length;
                if (bytesRead > 0) {
                    String newmd5 = calculateMD5(responseData);
                    if (MD5.equals(newmd5)) {
//                        System.arraycopy(responseData, 0, b, off, bytesRead);
                        byte[] temp = Arrays.copyOf(responseData, b.length);
                        for (int i = bytesRead; i < b.length; i++) {
                            temp[i] = (byte)' ';
                        }
                        System.arraycopy(temp, 0, b, off, b.length);
//                        Arrays.fill()
                    } else {
                        String errormsg = "MD5 different, read data error!";
                        System.out.println(errormsg);
                        System.arraycopy(errormsg, 0, b, off, bytesRead);
                    }
                }
                return bytesRead;
            }
        } catch (Exception e) {
            // 出现异常，用下一个副本
            currentReplicaIndex++;
            return read(b, off, len);
        }

        return -1; // 没数据可读
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    private static String calculateMD5(byte[] content) throws NoSuchAlgorithmException {
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