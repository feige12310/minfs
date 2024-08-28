package com.ksyun.campus.dataserver.services;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.campus.dataserver.domain.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.nodes.PersistentNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

//todo 写本地
//todo 调用远程ds服务写接口，同步副本，以达到多副本数量要求
//todo 选择策略，按照 az rack->zone 的方式选取，将三副本均分到不同的az下
//todo 支持重试机制
//todo 返回三副本位置

// todo  保存每个副本的使用容量
@Service
@Slf4j
public class DataService {
    @Autowired
    private CuratorFramework curatorFramework;

    @Value("${server.port}")
    int port;

    @Value("${az.rack}")
    private String rack;

    @Value("${az.zone}")
    private String zone;

    private PersistentNode persistentNode;

    // todo ipAddress等分配了主机之后可以直接配置文件注入
    String ipAddress;
    {
        try {
            ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    String storagePath;
    // 创建OkHttpClient的客户端
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    /**
     * 将数据写入到本地磁盘中，并返回文件信息和当前的datanode节点信息
     * @param fileSystemName
     * @param path
     * @param offset
     * @param length
     * @param data
     * @return
     * @throws IOException
     */
    public DataServerInfoVO write(String fileSystemName,String path,int offset,int length,byte[] data) throws IOException {
        //  创建文件要创建一个空文件
        // 写入的文件默认是不存在的
        ipAddress = ipAddress.replace('.', '_');
        // 获取本项目的绝对路径
        String projectPath = System.getProperty("user.dir");
        storagePath=projectPath+File.separator+ipAddress+"_"+port+File.separator+fileSystemName+File.separator+path;
        log.info("写入本地路径为：{}",storagePath);
        // 创建目标文件的目录（如果不存在）
        File file = new File(storagePath);
        // 返回对象
        DataServerInfoVO dataServerInfo =new DataServerInfoVO();
        if(!storagePath.contains(".")){
            log.info("是个目录");
            // 如果是个目录创建目录
            if(!file.exists()){
                file.mkdirs();
            }
            FileType type = FileType.Directory;
            dataServerInfo.setFileType(type);
        }else{
            // 是个文件,先建父目录
            FileType type = FileType.File;
            dataServerInfo.setFileType(type);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
                log.info("创建不存在的本地路径。");
            }
            // 通过临时文件保证写的时候要么成功要么失败
            File tempFile = new File(storagePath + ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(data, offset, length);
                fos.flush();  // 确保数据完全写入到磁盘中
                // 必须保证临时文件关闭，要不然无法移动临时文件到目标文件
                fos.close();
                // 写操作成功，替换原文件
                try {
                    Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    log.info("写操作成功，替换临时文件");
                } catch (IOException e) {
                    log.error("文件移动失败", e);
                    // 如果移动失败，尝试删除临时文件
                    if (tempFile.exists()) {
                        tempFile.delete();
                        log.info("写操作失败，删除临时文件");
                    }
                    dataServerInfo.setFileSize(-1);
                }
            } catch (IOException e) {
                // 写操作失败，删除临时文件
                if (tempFile.exists()) {
                    tempFile.delete();
                    log.info("写操作失败，删除临时文件");
                }
                dataServerInfo.setFileSize(-1);
            }
        }
        // 返回副本的文件元信息，path,size,mtime,type,ReplicaData中的id,dsNode,path等
        // 创建一个表示文件的Path对象  获取到当前文件的size
        if(dataServerInfo.getFileSize()!=-1 ){
            Path path1 = new File(storagePath).toPath();
            dataServerInfo.setFileSize((int) Files.size(path1));
        }
        String ip = ipAddress.replace('_','.');
        dataServerInfo.setIp(ip);
        dataServerInfo.setPort(port);
        dataServerInfo.setMtime(System.currentTimeMillis());
        dataServerInfo.setPath(storagePath);

        ReplicaData replicaData =new ReplicaData();
        replicaData.setId(UUID.randomUUID().toString());
        replicaData.setDsNode(ip+":"+port);
        replicaData.setPath(storagePath);
        dataServerInfo.setReplicaData(replicaData);
        return dataServerInfo;
    }

    /**
     *
     * @param fileSystemName
     * @param path
     * @param offset
     * @param length
     * @return
     * @throws UnknownHostException
     * @throws NoSuchAlgorithmException
     */
    public byte[] read(String fileSystemName,String path,int offset,int length) throws UnknownHostException, NoSuchAlgorithmException {
        // 获取到当前的副本ip+端口，不同的副本端口存的位置不相同
        byte[] data = new byte[length];
        ipAddress = ipAddress.replace('.', '_');
        // 获取本项目的绝对路径
        String projectPath = System.getProperty("user.dir");
        storagePath=projectPath+File.separator+ipAddress+"_"+port+File.separator+fileSystemName+File.separator+path;
        log.info("读取本地路径为：{}",storagePath);
        File file=new File(storagePath);
        if(file.isDirectory()){
            return data;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            // 移动文件指针到指定的偏移量
            fis.read(data,offset,length);
            log.info("读取文件成功");
        } catch (IOException e) {
            return "null".getBytes();
        }
        return data;
    }

    /**
     * 删除文件
     * @param fileSystemName
     * @param path
     * @return
     */
    public boolean delete(String fileSystemName,String path){
        // 路径下有多文件进行递归删除
        ipAddress = ipAddress.replace('.', '_');

        // 获取本项目的绝对路径
        String projectPath = System.getProperty("user.dir");
        storagePath=projectPath+File.separator+ipAddress+"_"+port+File.separator+fileSystemName+File.separator+path;
        File file =new File(storagePath);
        if(file.isFile()){
            // 文件直接删除
            return file.delete();
        }else if(file.isDirectory()){
            // 目录递归删除
            return deleteDirectory(file);
        }else{
            return false;
        }
    }
    // 递归删除目录及其内容
    private boolean deleteDirectory(File dir) {
        // 获取目录下的所有文件和子目录
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 如果是子目录，递归删除
                    if (!deleteDirectory(file)) {
                        return false;
                    }
                } else {
                    // 如果是文件，直接删除
                    if (!file.delete()) {
                        return false;
                    }
                }
            }
        }
        // 删除空目录
        return dir.delete();
    }

    public boolean mkdir(String fileSystemName, String path) {
        ipAddress = ipAddress.replace('.', '_');
        // 获取本项目的绝对路径
        String projectPath = System.getProperty("user.dir");
        storagePath=projectPath+File.separator+ipAddress+"_"+port+File.separator+fileSystemName+File.separator+path;

        log.info("写入本地路径为：{}",storagePath);
        // 创建目标文件的目录（如果不存在）
        File file = new File(storagePath);
        // 返回对象
        if(!storagePath.contains(".")){
            log.info("是个目录");
            // 如果是个目录创建目录
            if (!file.exists()) {
                if (file.mkdirs()) {
                    log.info("目录创建成功: " + file.getAbsolutePath());
                    return true;
                } else {
                    log.error("目录创建失败: " + file.getAbsolutePath());
                    return false;
                }
            } else {
                log.info("目录已存在: " + file.getAbsolutePath());
                return true;
            }
        }else{
            // 处理文件
            File parentDir = file.getParentFile();
            // 如果文件的父目录不存在，则创建目录
            if (parentDir != null && !parentDir.exists()) {
                if (parentDir.mkdirs()) {
                    log.info("目录创建成功: " + parentDir.getAbsolutePath());
                } else {
                    log.error("目录创建失败: " + parentDir.getAbsolutePath());
                }
            }
            try {
                // 如果文件不存在，则创建空文件
                if (!file.exists()) {
                    if (file.createNewFile()) {
                        log.info("空文件创建成功: " + file.getAbsolutePath());
                        return true;
                    } else {
                        log.error("空文件创建失败: " + file.getAbsolutePath());
                        return false;
                    }
                } else {
                    log.info("文件已存在: " + file.getAbsolutePath());
                    return true;
                }
            } catch (IOException e) {
                log.error("创建文件时发生错误: " + e.getMessage(), e);
                return false;
            }
        }
    }

    public boolean exist(String fileSystemName, String path) {
        ipAddress = ipAddress.replace('.', '_');
        // 获取本项目的绝对路径
        String projectPath = System.getProperty("user.dir");
        storagePath=projectPath+File.separator+ipAddress+"_"+port+File.separator+fileSystemName+File.separator+path;
        File file =new File(storagePath);
        // 判断文件是否存在并且大小是否大于0
        if (file.exists() && file.isFile() && file.length() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean copyReplicaData(CopyReplicaDataDTO copyReplicaDataDTO, String fileSystemName, String path) throws IOException {
        ipAddress = ipAddress.replace('.', '_');
        // 获取本项目的绝对路径
        String projectPath = System.getProperty("user.dir");
        storagePath=projectPath+File.separator+ipAddress+"_"+port+File.separator+fileSystemName+File.separator+path;
        File sourceFile =new File(storagePath);
        byte[] fileContent = Files.readAllBytes(sourceFile.toPath());
        List<String> fileSystemNames = copyReplicaDataDTO.getFileSystemNames();
        List<Integer> ports = copyReplicaDataDTO.getPorts();
        List<String> pathList = copyReplicaDataDTO.getPathList();
        for(int i=0;i<pathList.size();i++){
            String targetFileSystemName=fileSystemNames.get(i);
            Integer targetPort = ports.get(i);
            String targetPath =pathList.get(i);
            String ip =ipAddress.replace("_",".");
            String url ="http://"+ip+":"+targetPort+"/data/write?path="+targetPath+"&offset=0&length="+String.valueOf(fileContent.length);
            // 调用该实例的写方法
            RequestBody requestBody =RequestBody.create(fileContent,MediaType.parse("application/octet-stream"));
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("fileSystemName", targetFileSystemName)
                    .post(requestBody)
                    .build();
            // 发送请求
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.info("Failed to send file content to {}", url);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 更新数据到zookeeper
     * 每20秒检查一次进行更新
     * 如果数据有变动也要更新
     */
    @Scheduled(fixedRate=20000) // 每20秒执行一次定时任务
    public void upToZK() throws IOException {
        log.info("执行一次zookeeper更新任务");
        // 获取当前副本的文件数量和使用容量
        DataServerMsg dataServerMsg =getDataNodeInfo();
        int fileTotal = dataServerMsg.getFileTotal();
        int useCapacity = dataServerMsg.getUseCapacity();
        // 更新到zookeeper
        try {
            String ipAdd=ipAddress.replace("_",".");
            DataServerInfo updatedDataServerInfo = new DataServerInfo(ipAdd, port, useCapacity, rack, zone, fileTotal);
//            ipAddress = ipAddress.replace("_",".");
//            String ip="127.0.0.1";
            String path = "/registry/dataService/" + ipAdd + ":" + port;
            byte[] updatedData = serializeDataServerInfo(updatedDataServerInfo);
            // Ensure CuratorFramework is started before attempting registration
            if (curatorFramework.checkExists().forPath(path) != null) {
                curatorFramework.setData().forPath(path, updatedData);
                log.info("Data updated in Zookeeper for path: {}", path);
            } else {
                log.error("Path {} does not exist in Zookeeper", path);
//                return Result.failure("Path does not exist in Zookeeper");
            }

            log.info("dataService registered to Zookeeper with path: {}", path);
        }catch (Exception e){
            log.error("Error registering service to Zookeeper", e);
        }
    }
    private byte[] serializeDataServerInfo(DataServerInfo dataServerInfo) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsBytes(dataServerInfo);
    }

    /**
     * 获取当前副本的文件数量和使用容量
     * @return
     */
    public DataServerMsg getDataNodeInfo() {
        DataServerMsg dataServerMsg = new DataServerMsg();
        dataServerMsg.setHost(ipAddress);
        dataServerMsg.setPort(port);

        ipAddress = ipAddress.replace('.', '_');
        // 获取本项目的绝对路径
        String projectPath = System.getProperty("user.dir");
        storagePath=projectPath+File.separator+ipAddress+"_"+port+File.separator;
        log.info("当前副本本地路径为：{}",storagePath);
        // 创建目标文件的目录（如果不存在）
        // 获取当前文件数量和使用容量
        // list中第一个传fileNums,第二个传useCapacity
        List<Integer> list =new ArrayList<Integer>();
        list.add(0);
        list.add(0);
        File file = new File(storagePath);
        if(file.exists()){
            recurFile(file,list);
        }
        //获取当前副本的容量大小
        dataServerMsg.setFileTotal(list.get(0));
        dataServerMsg.setUseCapacity(list.get(1));
        return dataServerMsg;
    }
    // 递归计算数量和容量
    private void recurFile(File file, List<Integer> list) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    recurFile(f, list);
                }
            }
        } else {
            list.set(0, list.get(0) + 1); // 更新文件数量
            list.set(1, (int) (list.get(1) +file.length())); // 更新总容量
        }
    }
}
