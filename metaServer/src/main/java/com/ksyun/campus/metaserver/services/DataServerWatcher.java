package com.ksyun.campus.metaserver.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.campus.metaserver.domain.DataServerInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
@Order(2)
public class DataServerWatcher implements ApplicationRunner {
    @Autowired
    private CuratorFramework curatorFramework;

    @Value("${data-server.path}")
    private String dataServerPath;

    private final Map<String, DataServerInfo> dataServerMap = new ConcurrentHashMap<>();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 监听 dataServer 的子节点
        PathChildrenCache cache = new PathChildrenCache(curatorFramework, dataServerPath, true);

        cache.getListenable().addListener((client, event) -> {
            switch (event.getType()) {
                case CHILD_ADDED:
                case CHILD_UPDATED:
                    log.info("DataServer node {}: {}", event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED ? "added" : "updated", event.getData().getPath());
                    updateDataServer(event.getData().getPath(), event.getData().getData());
                    break;
                case CHILD_REMOVED:
                    log.info("DataServer node removed: {}", event.getData().getPath());
                    removeDataServer(event.getData().getPath());
                    break;
                default:
                    break;
            }
        });

        // 启动监听器
        cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);

        // 手动初始化现有的子节点
        List<ChildData> currentData = cache.getCurrentData();
        for (ChildData childData : currentData) {
            updateDataServer(childData.getPath(), childData.getData());
        }
    }

    private void updateDataServer(String path, byte[] data) {
        try {
                DataServerInfo dataServerInfo = deserializeDataServerInfo(data);
                String ipPort = dataServerInfo.getIp() + ":" + dataServerInfo.getPort();
                dataServerMap.put(ipPort, dataServerInfo);
                log.info("DataServer updated in map: {}", dataServerInfo);

        } catch (Exception e) {
            log.error("Error updating DataServer in map", e);
        }
    }

    private void removeDataServer(String path) {
        try {
            // 解析路径获得 ip:port 作为 key
            String nodeName = path.substring(path.lastIndexOf('/') + 1);
            dataServerMap.remove(nodeName);
            log.info("DataServer removed from map: {}", nodeName);
        } catch (Exception e) {
            log.error("Error removing DataServer from map", e);
        }
    }

    public List<DataServerInfo> getTopThreeServers() {
        return dataServerMap.values().stream()
                .sorted(Comparator.comparing(DataServerInfo::getUseCapacity))
                .limit(3)
                .collect(Collectors.toList());
    }

    private byte[] serializeDataServerInfo(DataServerInfo dataServerInfo) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsBytes(dataServerInfo);
    }
    private DataServerInfo deserializeDataServerInfo(byte[] data) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(data, DataServerInfo.class);
    }

    public boolean dataServerIsOnline(String ipPort){
        return dataServerMap.containsKey(ipPort);
    }
}

