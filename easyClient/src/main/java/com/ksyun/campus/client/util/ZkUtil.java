package com.ksyun.campus.client.util;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZkUtil {
    private String zookeeperAddr = "127.0.0.1:2181";
    private HashMap<String, String> metaServerMap = new HashMap<>();

    public String getZookeeperAddr() {
        return zookeeperAddr;
    }

    public void setZookeeperAddr(String zookeeperAddr) {
        this.zookeeperAddr = zookeeperAddr;
    }

    public HashMap<String, String> getMetaServerMap() {
        return metaServerMap;
    }

    public void setMetaServerMap(HashMap<String, String> metaServerMap) {
        this.metaServerMap = metaServerMap;
    }

    public ZkUtil() {
        init();
    }

    private void init() {
        try {
            postCons();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void postCons() throws Exception {
        // todo 初始化，与zk建立连接，注册监听路径，当配置有变化随时更新
        // 建立与 ZooKeeper 的连接
        ZooKeeper zooKeeper = new ZooKeeper(zookeeperAddr, 1000, new Watcher() {
            public void process(WatchedEvent event) {
//                System.out.println("Event received: " + event.getType());
            }
        });

        // 等待连接建立完成
        while (zooKeeper.getState() != ZooKeeper.States.CONNECTED) {
            Thread.sleep(100);
        }

        // 注册子节点的 Watcher
        zooKeeper.getChildren("/registry/metaService", new Watcher() {
            public void process(WatchedEvent event) {
//                System.out.println("Event received: " + event.getType());
                // 子节点发生变化时更新子节点集合
                if (event.getType() == Event.EventType.NodeChildrenChanged && event.getPath().equals("/metaServers")) {
                    try {
                        updateChildNodes(zooKeeper);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        //初始化metaServerMap
        updateChildNodes(zooKeeper);
    }

    // 更新子节点集合方法
    private void updateChildNodes(ZooKeeper zooKeeper) throws Exception {
        List<String> childNodes = zooKeeper.getChildren("/registry/metaService", true);
        // 遍历子节点集合
        for (String childNode : childNodes) {
            // 获取子节点的路径
            String childNodePath = "/registry/metaService/" + childNode;
            // 获取子节点的数据
            byte[] nodeData = zooKeeper.getData(childNodePath, false, null);
            String nodeValue = new String(nodeData);
            metaServerMap.put(childNode, nodeValue);
        }
    }
}
