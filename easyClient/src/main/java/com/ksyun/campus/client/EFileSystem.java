package com.ksyun.campus.client;

import com.ksyun.campus.client.domain.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.campus.client.server.FileSystemService;

import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EFileSystem extends FileSystem {

    private FileSystemService fileSystemService = new FileSystemService();

    //    private String fileName="default";
    public EFileSystem() {
        this("default");
    }


    public EFileSystem(String fileSystemName) {
        this.defaultFileSystemName = fileSystemName;
    }

    public FSInputStream open(String path) throws Exception {
        if (!isValidPath(path)) {
            System.out.println("path " + path + " is not valid");
            return null;
        }
        MetaServerMsg metaServer = fileSystemService.getMetaServer();
        String metaServerUrl = "http://" + metaServer.getHost() + ":" + metaServer.getPort() + "/open?path=" + path;
        String statInfo = callRemote("get", metaServerUrl, null);
        if(statInfo == null || statInfo.isEmpty())
        {
            System.out.println("file or dir not exit!");
            return null;
        }
        StatusResponse statusResponse = new ObjectMapper().readValue(statInfo, StatusResponse.class);

        if(statusResponse.getCode()!=200)
        {
            System.out.println("file or dir not exit!");
            return null;
        }

        StatInfoWithMD5 statInfo1 = statusResponse.getData();
        List<ReplicaData> replicaData = statInfo1.getReplicaData();
        if(replicaData == null || replicaData.isEmpty())
        {
            System.out.println("replicaData is empty!");
            return null;
        }
        FSInputStream fsInputStream = new FSInputStream(replicaData, this.defaultFileSystemName, statInfo1.getSize(), statInfo1.getMd5());

        return fsInputStream;
    }

    public FSOutputStream create(String path) throws Exception {
        if (!isValidPath(path)) {
            System.out.println("path " + path + " is not valid");
            return null;
        }
        return mknew(path, "/create");
    }

    public boolean mkdir(String path) throws Exception {
        if (!isValidPath(path)) {
            System.out.println("path " + path + " is not valid");
            return false;
        }
        FSOutputStream fsOutputStream = mknew(path, "/mkdir");


        return true;
    }

    public FSOutputStream mknew(String path, String oprtype) throws Exception {
        MetaServerMsg metaServer = fileSystemService.getMetaServer();
        String metaServerUrl = "http://" + metaServer.getHost() + ":" + metaServer.getPort() + oprtype + "?path=" + path;

        String dataServerListJson = callRemote("get", metaServerUrl, null);
        if(dataServerListJson == null || dataServerListJson.isEmpty())
        {
            System.out.println("file or dir exist!");
            return null;
        }
        if (oprtype.equalsIgnoreCase("/mkdir")) {
            return null;
        }
        // 将 JSON 字符串转换为 DataServerMsg 对象的 List 集合
        ObjectMapper objectMapper = new ObjectMapper();
        CreateResponse createResponse = objectMapper.readValue(dataServerListJson, CreateResponse.class);

        if(createResponse.getCode()!=200)
        {
            System.out.println("status code " + createResponse.getCode());
            return null;
        }

        List<ReplicaData> dataServerMsgList = createResponse.getData();
//        System.out.println(dataServerMsgList.toString());
        ReplicaData dataServerMsgBac = null;
        //获取备用的，如果存在的话
        if (dataServerMsgList.size() >= 4) {
            dataServerMsgBac = dataServerMsgList.get(3);
        }

        if(dataServerMsgList == null || dataServerMsgList.isEmpty())
        {
            System.out.println("dataServer not find!");
            return null;
        }

        for (ReplicaData dataServerMsg : dataServerMsgList) {
            String dataurl = "http://" + dataServerMsg.getDsNode() + "/data/mkdir?path=" + dataServerMsg.getPath();
            callRemote("post", dataurl, "null".getBytes(StandardCharsets.UTF_8));
        }

        System.out.println(path + " create successfully");
        FSOutputStream fsOutputStream = new FSOutputStream(this.defaultFileSystemName, path, dataServerMsgList, dataServerMsgBac);
        return fsOutputStream;
    }

    public boolean delete(String path) throws Exception {
        path=pathDeal(path);
        if (!isValidPath(path)) {
            System.out.println("path " + path + " is not valid");
            return false;
        }
        MetaServerMsg metaServer = fileSystemService.getMetaServer();
        String metaServerUrl = "http://" + metaServer.getHost() + ":" + metaServer.getPort() + "/delete?path=" + path;
        String dataServerListJson = callRemote("get", metaServerUrl, null);
        if(dataServerListJson == null || dataServerListJson.isEmpty())
        {
            System.out.println("file not exist!");
            return false;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        DeleteResponse deleteResponse = objectMapper.readValue(dataServerListJson, DeleteResponse.class);
        if(deleteResponse.getCode()!=200)
        {
            System.out.println("file not exist!");
            return false;
        }

        List<StatInfoWithMD5> dataServerMsgList = deleteResponse.getData();

        if (dataServerMsgList.isEmpty()) {
            System.out.println("dataServer not find!");
            return false;
        }
        for(StatInfoWithMD5 statInfoWithMD5:dataServerMsgList){
            List<ReplicaData> replicaData = statInfoWithMD5.getReplicaData();
            for (ReplicaData dataServerMsg : replicaData) {
                String dataurl = "http://" + dataServerMsg.getDsNode() + "/data/delete?path=" + dataServerMsg.getPath();
                callRemote("get", dataurl, null);
            }
        }
//        for (ReplicaData dataServerMsg : dataServerMsgList) {
//            String dataurl = "http://" + dataServerMsg.getDsNode() + "/data/delete?path=" + dataServerMsg.getPath();
//            callRemote("get", dataurl, null);
//        }
        System.out.println(path + " delete successfully");
        return true;
    }

    public StatInfo getFileStats(String path) throws Exception {
        path=pathDeal(path);
        if (!isValidPath(path)) {
            System.out.println("path " + path + " is not valid");
            return null;
        }

        MetaServerMsg metaServer = fileSystemService.getMetaServer();
        String metaServerUrl = "http://" + metaServer.getHost() + ":" + metaServer.getPort() + "/stats?path=" + path;
        String statInfoJson = callRemote("get", metaServerUrl, null);
        if(statInfoJson == null || statInfoJson.isEmpty())
        {
            System.out.println("dataServer not find!" );
            return null;
        }
        StatusResponse writeResponse = new ObjectMapper().readValue(statInfoJson, StatusResponse.class);
        if(writeResponse.getCode()!=200)
        {
            System.out.println("file not exist!");
            return null;
        }

        StatInfoWithMD5 statInfo = writeResponse.getData();
        StatInfo newStatInfo = new StatInfo(statInfo.getPath(), statInfo.getSize(), statInfo.getMtime(), statInfo.getType(), statInfo.getReplicaData());
        return newStatInfo;
    }

    public List<StatInfo> listFileStats(String path) throws Exception {
        path=pathDeal(path);
        if (!isValidPath(path)) {
            System.out.println("path " + path + " is not valid");
            return null;
        }

        MetaServerMsg metaServer = fileSystemService.getMetaServer();
        String metaServerUrl = "http://" + metaServer.getHost() + ":" + metaServer.getPort() + "/listdir?path=" + path;
        String statInfosJson = callRemote("get", metaServerUrl, null);
        if(statInfosJson == null || statInfosJson.isEmpty())
        {
            System.out.println("dataServer not find!");
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        ListDirResponse listDirResponse = objectMapper.readValue(statInfosJson, ListDirResponse.class);

        if(listDirResponse.getCode()!=200)
        {
            System.out.println("status code " + listDirResponse.getCode()+"file not find");
            return null;
        }

        List<StatInfoWithMD5> statInfomd5List = listDirResponse.getData();
        List<StatInfo> statInfoList = new ArrayList<StatInfo>();
        for (StatInfoWithMD5 sim5 : statInfomd5List) {
            StatInfo temp = new StatInfo(sim5.getPath(), sim5.getSize(), sim5.getMtime(), sim5.getType(), sim5.getReplicaData());
            statInfoList.add(temp);
        }

        return statInfoList;
    }

    public ClusterInfo getClusterInfo() throws Exception {

        String masterJson = null;
        String slaveJson = null;
        List<String> dataServersChildrenMsg = fileSystemService.getChildrenData("/registry/dataService");
        List<String> metaServersChildrenMsg = fileSystemService.getChildrenData("/registry/metaService");

        ObjectMapper objectMapper = new ObjectMapper();
        ClusterInfo clusterInfo = new ClusterInfo();
        masterJson = metaServersChildrenMsg.get(0);
        String[] masterArray = masterJson.split(":");
        clusterInfo.setMasterMetaServer(new MetaServerMsg(masterArray[0], masterArray[1]));
        if (metaServersChildrenMsg.size() > 1) {
            slaveJson = metaServersChildrenMsg.get(1);
            String[] slaveArray = slaveJson.split(":");
            clusterInfo.setSlaveMetaServer(new MetaServerMsg(slaveArray[0], slaveArray[1]));
        }
        List<DataServerInfo> dataServerInfos = objectMapper.readValue(objectMapper.writeValueAsString(dataServersChildrenMsg), new TypeReference<List<DataServerInfo>>() {});

        List<DataServerMsg> dataServerMsgs = new ArrayList<DataServerMsg>();
        for (DataServerInfo dsi : dataServerInfos) {
            DataServerMsg temp = new DataServerMsg(dsi.getIp(), dsi.getPort(), dsi.getFileTotal(), 1024 * 1024*1024, dsi.getUseCapacity());
            dataServerMsgs.add(temp);
        }
        clusterInfo.setDataServer(dataServerMsgs);
        return clusterInfo;
    }
    public static String pathDeal(String path){
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
//        while (path.startsWith("/")) {
//            path = path.substring(1, path.length());
//        }
        return path;
    }

    public static boolean isValidPath(String path) {
        try {
            Paths.get(path);
            return true;
        } catch (InvalidPathException e) {
            return false;
        }
    }
}
