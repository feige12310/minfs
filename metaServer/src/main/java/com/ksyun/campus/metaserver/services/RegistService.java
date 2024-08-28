package com.ksyun.campus.metaserver.services;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.framework.recipes.nodes.PersistentNode;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@Order(1)
public class RegistService implements ApplicationRunner {

    @Autowired
    private CuratorFramework curatorFramework;

    @Value("${server.port}")
    private int port;

    private String ip = "127.0.0.1";

    private PersistentNode persistentNode;
    private LeaderLatch leaderLatch;

    private TreeCache leaderCache;

    private boolean isLeaderWatcherRun = false;


    public void registToCenter() {
        try {
            String path = "/registry/metaService/" + ip + ":" + port;
            String data = ip + ":" + port + ":" + "false";

            // 确保 CuratorFramework 在尝试注册之前已启动
            if (curatorFramework.getState() != CuratorFrameworkState.STARTED) {
                curatorFramework.start();
            }

            // 创建临时节点并注册
            persistentNode = new PersistentNode(
                    curatorFramework,
                    CreateMode.EPHEMERAL,
                    false,
                    path,
                    data.getBytes(StandardCharsets.UTF_8)
            );
            persistentNode.start();

            log.info("metaService registered to Zookeeper with path: {}", path);

            // 开始主从选举
            startLeaderElection("/registry/isLeader", "/registry/metaService", ip + ":" + port );

        } catch (Exception e) {
            log.error("Error registering service to Zookeeper", e);
        }
    }

    private void startLeaderElection(String leaderPath, String metaPath, String ipPort) throws Exception {
        if (curatorFramework.getState() != CuratorFrameworkState.STARTED) {
            curatorFramework.start();
            log.info("CuratorFramework started successfully");
        }
        log.info("start ======== Leader Election");
        leaderLatch = new LeaderLatch(curatorFramework, leaderPath);

        leaderLatch.addListener(new LeaderLatchListener() {
            @Override
            public void isLeader() {
                log.info("This node is now the leader.");
                String path = metaPath + "/" + ipPort;
                String data = ipPort + ":true";
                try {
                    curatorFramework.setData().forPath(path, data.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                startLeaderDuties();
            }

            @Override
            public void notLeader() {
                log.info("This node is no longer the leader.");
                String path = metaPath + "/" + ipPort;
                String data = ipPort + ":false";
                try {
                    curatorFramework.setData().forPath(path, data.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                stopLeaderDuties();
            }
        });
        leaderLatch.start();
    }

    private void startLeaderDuties() {
        // 实现 leader 节点的特定任务，例如处理写操作、数据同步等
        log.info("Leader duties started...");
        if (isLeaderWatcherRun == true) {
            leaderCache.close();
            isLeaderWatcherRun = false;
        }
    }

    private void stopLeaderDuties() {
        // 停止 leader 节点的任务
        log.info("Leader duties stopped.");
    }


    public void syncFromLeader(String metaServicePath, String dataPath, String followerIpPort) throws Exception {
        // 1. 找到 Leader 节点的 IP:port
        String leaderIpPort = findLeader(metaServicePath);
        if (leaderIpPort == null) {
            throw new RuntimeException("No leader found.");
        }

        // 2. 获取 Leader 根节点路径
        String leaderRootPath = dataPath + "/" + leaderIpPort;

        // 3. 开始递归复制节点
        copyNodeRecursively(leaderRootPath, dataPath + "/" + followerIpPort);

        log.info("Data synchronization from leader {} to follower {} completed.", leaderIpPort, followerIpPort);
    }

    private void copyNodeRecursively(String sourcePath, String targetPath) throws Exception {
        // 获取源节点的Stat对象 (包括元数据)
        Stat stat = curatorFramework.checkExists().forPath(sourcePath);
        if (stat == null) {
            log.warn("Source path {} does not exist", sourcePath);
            return;
        }

        // 获取节点数据
        byte[] nodeData = curatorFramework.getData().forPath(sourcePath);

        // 检查目标路径是否存在
        Stat targetStat = curatorFramework.checkExists().forPath(targetPath);
        if (targetStat == null) {
            // 如果目标路径不存在，创建节点
            curatorFramework.create()
                    .creatingParentsIfNeeded()
                    .withMode(stat.getEphemeralOwner() == 0 ? CreateMode.PERSISTENT : CreateMode.EPHEMERAL)
                    .withACL(curatorFramework.getACL().forPath(sourcePath))
                    .forPath(targetPath, nodeData);
        } else {
            log.warn("Target path {} already exists, skipping creation.", targetPath);
        }

        // 获取子节点列表
        List<String> children = curatorFramework.getChildren().forPath(sourcePath);

        // 递归复制每个子节点
        for (String child : children) {
            String childSourcePath = sourcePath + "/" + child;
            String childTargetPath = targetPath + "/" + child;
            copyNodeRecursively(childSourcePath, childTargetPath);
        }
    }


    public void clearNodeData(String path) throws Exception {
        if (curatorFramework.checkExists().forPath(path) != null) {
            curatorFramework.delete().deletingChildrenIfNeeded().forPath(path);
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        registToCenter();
        // 等待 Leader 选举完成
        leaderLatch.await(5, TimeUnit.SECONDS);

        // 如果当前节点不是 leader，执行初始化任务
        if (!leaderLatch.hasLeadership()) {
            log.info("当前节点是 follower，开始从 leader 同步数据...");
            String followerPath = "/registry/metaService/" + ip + ":" + port;
            clearNodeData(followerPath);
            syncFromLeader("/registry/metaService", "/data", ip + ":" + port);
            startLeaderWatcher("/registry/metaService", "/data", ip + ":" + port);
        }
    }

    @PreDestroy
    public void unregister() throws IOException {
        if (persistentNode != null) {
            persistentNode.close();
            log.info("metaService unregistered from Zookeeper.");
        }

        if (leaderLatch != null) {
            leaderLatch.close();
            log.info("LeaderLatch closed.");
        }
        if (leaderCache != null) {
            leaderCache.close();
            log.info("Leader watcher closed.");
        }
    }

    public String findLeader(String metaServicePath) throws Exception {
        // 获取 /registry/metaService 下的所有子节点
        List<String> nodes = curatorFramework.getChildren().forPath(metaServicePath);

        // 遍历每个节点
        for (String node : nodes) {
            // 获取节点的完整路径
            String nodePath = metaServicePath + "/" + node;

            // 获取该节点的元数据
            String nodeData = new String(curatorFramework.getData().forPath(nodePath), StandardCharsets.UTF_8);

            // 检查元数据中是否包含 "true"
            if (nodeData.endsWith("true")) {
                // 如果是 leader，返回该节点的 IP:port
                return node.split(":")[0] + ":" + node.split(":")[1];
            }
        }

        // 如果没有找到 leader，返回 null 或者抛出异常
        return null;
    }

    public void startLeaderWatcher(String metaServicePath, String dataPath, String followerIpPort) throws Exception {
        String leaderIpPort = findLeader(metaServicePath);
        String leaderPath = dataPath + "/" + leaderIpPort;
        leaderCache = new TreeCache(curatorFramework, leaderPath);

        leaderCache.getListenable().addListener((client, event) -> {
            switch (event.getType()) {
                case NODE_ADDED:
                case NODE_UPDATED:
                case NODE_REMOVED:
                    log.info("Leader node changed, synchronizing data...");
                    clearNodeData(dataPath + "/" + followerIpPort);
                    syncFromLeader(metaServicePath, dataPath, followerIpPort);
                    break;
                default:
                    break;
            }
        });

        leaderCache.start();
        isLeaderWatcherRun = true;
//        clearNodeData(dataPath + "/" + followerIpPort);
//        syncFromLeader(metaServicePath, dataPath, followerIpPort);

    }
}
