package com.ksyun.campus.client.server;

import com.ksyun.campus.client.domain.MetaServerMsg;
import com.ksyun.campus.client.util.ZkUtil;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileSystemService {

    //获取指定path下的子节点所存数据的集合，提供给getClusterInfo()使用
    public List<String> getChildrenData(String path) throws Exception {
        String zookeeperAddr = new ZkUtil().getZookeeperAddr();
//        System.out.println("zoo "+zookeeperAddr);
        ZooKeeper zooKeeper = new ZooKeeper(zookeeperAddr, 3000, new Watcher() {
            public void process(WatchedEvent event) {
//                System.out.println("Event received: " + event.getType());
            }
        });

        // 等待连接建立完成
        while (zooKeeper.getState() != ZooKeeper.States.CONNECTED) {
            Thread.sleep(100);
        }

        List<String> childrenData = new ArrayList<>();
        List<String> children = zooKeeper.getChildren(path, false);
        for (String child : children) {
            String childPath = path + "/" + child;
//            System.out.println("child "+childPath);
            byte[] data = zooKeeper.getData(childPath, false, null);
            childrenData.add(new String(data));
        }
        zooKeeper.close();
        return childrenData;
    }

    //返回一个可用的metaServer
    //参数path是创建的文件的路径
    public MetaServerMsg getMetaServer() throws Exception {

//        System.out.println("cd getMetaServer method");
        String metaServerValue = "";
        // 选择一个可用的metaServer
        String masterMetaServerValue = "";
        String slaveMetaServerValue = "";
        HashMap<String, String> metaServerMap = new ZkUtil().getMetaServerMap();

        for (Map.Entry<String, String> entry : metaServerMap.entrySet()) {
            String[] parts = entry.getValue().split(":");
            if(parts.length>2 && parts[2].equalsIgnoreCase("true")){
                masterMetaServerValue=entry.getValue();
            } else {
                slaveMetaServerValue=entry.getValue();
            }
        }
        if (masterMetaServerValue != "") {
            metaServerValue = masterMetaServerValue;
        } else {
            metaServerValue = slaveMetaServerValue;
        }

        if(metaServerValue == ""){
            return null;
        }
        String[] parts = metaServerValue.split(":");
        MetaServerMsg metaServerMsg = new MetaServerMsg(parts[0],parts[1]);
        return metaServerMsg;
    }
}