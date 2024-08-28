package com.ksyun.campus.metaserver.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.campus.metaserver.domain.DataServerInfo;
import com.ksyun.campus.metaserver.domain.FileType;
import com.ksyun.campus.metaserver.domain.ReplicaData;
import com.ksyun.campus.metaserver.domain.StatInfoWithMD5;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MetaService {
    @Autowired
    private DataServerWatcher dataServerWatcher;


    @Autowired
    private CuratorFramework curatorFramework;

    @Value("${server.port}")
    private int port;

    private String ip = "127.0.0.1";

    public List<DataServerInfo> pickDataServer(){
        // todo 通过zk内注册的ds列表，选择出来一个ds，用来后续的wirte
        // 需要考虑选择ds的策略？负载
        return dataServerWatcher.getTopThreeServers();

    }

    public boolean isLeader() throws Exception {
        String path = "/registry/metaService/" + ip + ":" + port;
        // 获取节点数据
        byte[] dataBytes = curatorFramework.getData().forPath(path);

        // 将字节数组转换为字符串
        String dataString = new String(dataBytes, StandardCharsets.UTF_8);

        // 假设数据格式为 "IP:port:true" 或 "IP:port:false"
        String[] parts = dataString.split(":");

        // 最后一个部分应该是 "true" 或 "false"
        String statusString = parts[parts.length - 1];

        // 解析为布尔值
        return Boolean.parseBoolean(statusString);
    }



    public String createNode(String fileSystemName, String path, List<DataServerInfo> pickedNode) throws Exception {
        // 拼接完整的 Zookeeper 路径
        String fullPath = "/data/" + ip + ":" + port + "/" + fileSystemName + path;

        // 递归创建父节点并填入元数据
        createParentNodesIfNeeded(fileSystemName, path);

        // 创建当前节点的 StatInfo 元数据
        StatInfoWithMD5 statInfoWithMD5 = new StatInfoWithMD5();
        statInfoWithMD5.setPath(path);
        statInfoWithMD5.setSize(0); // 初始大小为 0，可以在文件写入后更新
        statInfoWithMD5.setMtime(System.currentTimeMillis());
        statInfoWithMD5.setType(FileType.File);
         // 初始化为空列表，可以后续更新
        // 将 pickedNode 转换为 List<ReplicaData>
        List<ReplicaData> replicaDataList = pickedNode.stream().map(ds -> {
            // 生成唯一ID
            String id = UUID.randomUUID().toString();
            // 组合ip和port
            String dsNode = ds.getIp() + ":" + ds.getPort();
            // 构建ReplicaData对象
            ReplicaData replicaData = new ReplicaData(id, dsNode, path);
            return replicaData;
        }).collect(Collectors.toList());
        statInfoWithMD5.setReplicaData(replicaDataList);

        // 将 StatInfo 对象序列化为字节数组
        byte[] data = serializeStatInfo(statInfoWithMD5);

        // 创建节点，并且如果父节点不存在，自动创建父节点
        curatorFramework.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.PERSISTENT)
                .forPath(fullPath, data);

        return fullPath;
    }

    private byte[] serializeStatInfo(StatInfoWithMD5 statInfoWithMD5) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsBytes(statInfoWithMD5);
    }
    private StatInfoWithMD5 deserializeStatInfo(byte[] data) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(data, StatInfoWithMD5.class);
    }


    private void createParentNodesIfNeeded(String fileSystemName, String path) throws Exception {
        // 获取父节点路径
        String parentPath = path.substring(0, path.lastIndexOf('/'));
        String fullParentPath = "/data/" + ip + ":" + port + "/" + fileSystemName  +parentPath;

        if (!parentPath.isEmpty() && curatorFramework.checkExists().forPath(fullParentPath) == null) {
            // 递归创建父节点
            createParentNodesIfNeeded(fileSystemName, parentPath);

            // 创建父节点的 StatInfo 元数据
            StatInfoWithMD5 parentStatInfoWithMD5 = new StatInfoWithMD5();
            parentStatInfoWithMD5.setPath(parentPath);
            parentStatInfoWithMD5.setSize(0);
            parentStatInfoWithMD5.setMtime(System.currentTimeMillis());
            parentStatInfoWithMD5.setType(FileType.Directory); // 父节点为目录类型

            // 将父节点的 StatInfo 对象序列化为字节数组
            byte[] parentData = serializeStatInfo(parentStatInfoWithMD5);

            // 创建父节点
            curatorFramework.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(fullParentPath, parentData);
        }
    }


    public String mkdirNode(String fileSystemName, String path) throws Exception {
        // 拼接完整的 Zookeeper 路径
        String fullPath = "/data/" + ip + ":" + port + "/" + fileSystemName + path;

        // 递归创建父节点并填入元数据
        createParentNodesIfNeeded(fileSystemName, path);

        // 创建当前节点的 StatInfo 元数据
        StatInfoWithMD5 statInfoWithMD5 = new StatInfoWithMD5();
        statInfoWithMD5.setPath(path);
        statInfoWithMD5.setSize(0); // 初始大小为 0，可以在文件写入后更新
        statInfoWithMD5.setMtime(System.currentTimeMillis());
        statInfoWithMD5.setType(FileType.Directory);

        // 将 StatInfo 对象序列化为字节数组
        byte[] data = serializeStatInfo(statInfoWithMD5);

        // 创建节点，并且如果父节点不存在，自动创建父节点
        curatorFramework.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.PERSISTENT)
                .forPath(fullPath, data);

        return fullPath;
    }

    public List<StatInfoWithMD5> deleteNode(String fileSystemName, String path) throws Exception {
        // 获取待删除节点的完整路径
        String fullPath = "/data/" + ip + ":" + port + "/" + fileSystemName + path;
        String stopPath = "/data/" + ip + ":" + port + "/" + fileSystemName;
        List<StatInfoWithMD5> result = new ArrayList<>();

        // 获取待删除节点的元数据
        StatInfoWithMD5 nodeStatInfoWithMD5 = getNodeData(fullPath);
        if (nodeStatInfoWithMD5.getSize()!=0){
            //进行递归查询将该节点以及该节点的子孙节点中type=file的节点statInfoWithMD5加入到List<statInfoWithMD5>中
            collectFileNodes(fullPath, result);
        }

//        log.info("fullPath={}",fullPath);

        // 遍历父节点并更新它们的 size
        String parentPath = fullPath;
        while ((parentPath = parentPath.substring(0, parentPath.lastIndexOf('/'))) != null && !parentPath.isEmpty()) {
            if (stopPath.equals(parentPath)) {
                break;
            }
//            log.info("===============path{}", parentPath);
            StatInfoWithMD5 parentStatInfoWithMD5 = getNodeData(parentPath);
            parentStatInfoWithMD5.setSize(parentStatInfoWithMD5.getSize() - nodeStatInfoWithMD5.getSize());
            parentStatInfoWithMD5.setMtime(System.currentTimeMillis());
            curatorFramework.setData().forPath(parentPath, serializeStatInfo(parentStatInfoWithMD5));
        }

        // 删除节点
        // 删除节点，如果节点不存在会抛出异常
        try {
            curatorFramework.delete()
                    .deletingChildrenIfNeeded()  // 如果节点有子节点，也会一起删除
                    .forPath(fullPath);
        } catch (Exception e) {
            throw new Exception("Failed to delete node at path: " + fullPath, e);
        }
        return result;
    }

    private void collectFileNodes(String path, List<StatInfoWithMD5> result) throws Exception {
        List<String> children = curatorFramework.getChildren().forPath(path);

        for (String child : children) {
            String childPath = path + "/" + child;
            StatInfoWithMD5 childStatInfoWithMD5 = getNodeData(childPath);

            // 如果是 type=file 的节点，加入列表
            if (childStatInfoWithMD5.getType()==FileType.File) {
                result.add(childStatInfoWithMD5);
            }

            // 如果该节点还有子节点，递归处理
            if (childStatInfoWithMD5.getSize() != 0) {
                collectFileNodes(childPath, result);
            }
        }
    }


    public void writeNode(String fileSystemName, String path, int length, String md5) throws Exception {
        String fullPath = "/data/" + ip + ":" + port + "/" + fileSystemName + path;
        String parentPath = fullPath;
        String stopPath = "/data/" + ip + ":" + port+ "/" + fileSystemName;
        StatInfoWithMD5 currentStatInfoWithMD5 = getNodeData(parentPath);
        long delta = length - currentStatInfoWithMD5.getSize();
        currentStatInfoWithMD5.setSize(length);
        currentStatInfoWithMD5.setMtime(System.currentTimeMillis());
        currentStatInfoWithMD5.setMd5(md5);

        curatorFramework.setData().forPath(parentPath, serializeStatInfo(currentStatInfoWithMD5));

        while ((parentPath = parentPath.substring(0, parentPath.lastIndexOf('/'))) != null && !parentPath.isEmpty()) {
            if (stopPath.equals(parentPath)) {
                break;
            }
            // 获取父节点的 StatInfo
            StatInfoWithMD5 parentStatInfoWithMD5 = getNodeData(parentPath);

            // 更新父节点的 size
            parentStatInfoWithMD5.setSize(parentStatInfoWithMD5.getSize() + delta);
            parentStatInfoWithMD5.setMtime(System.currentTimeMillis());

            // 更新父节点的元数据
            curatorFramework.setData().forPath(parentPath, serializeStatInfo(parentStatInfoWithMD5));
        }
    }



    public List<StatInfoWithMD5> listChildren(String fileSystemName, String path) throws Exception {
        String fullPath = "/data/" + ip + ":" + port + "/" + fileSystemName + path;
        List<String> childrens = curatorFramework.getChildren().forPath(fullPath);
        List<StatInfoWithMD5> childrenLists = new ArrayList<>();
        for (String children : childrens) {
            String childrenPath = fullPath + "/" + children;
            childrenLists.add(getNodeData(childrenPath));

        }
        return childrenLists;
    }


    public boolean isFile(String fullPath) {
        // 提取节点名称
        String nodeName = fullPath.substring(fullPath.lastIndexOf('/') + 1);
        // 判断节点名称是否包含文件扩展名
        return nodeName.contains(".");
    }


    public StatInfoWithMD5 getNodeData(String fullPath) throws Exception {
        return deserializeStatInfo(curatorFramework.getData().forPath(fullPath));
    }

    public boolean nodeExists(String fullPath) throws Exception {
        return curatorFramework.checkExists().forPath(fullPath) != null;
    }



}
