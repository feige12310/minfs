package com.ksyun.campus.dataserver.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.campus.dataserver.domain.DataServerInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.nodes.PersistentNode;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RegistService implements ApplicationRunner {

    @Autowired
    private CuratorFramework curatorFramework;

    @Value("${server.port}")
    private int port;

    @Value("${az.rack}")
    private String rack;

    @Value("${az.zone}")
    private String zone;

    private PersistentNode persistentNode;

    public void registToCenter() {
        // todo 将本实例信息注册至zk中心，包含信息 ip、port、capacity、rack、zone


        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
//            String ip = "127.0.0.1";
            DataServerInfo dataServerInfo = new DataServerInfo(ip, port, 0, rack, zone, 0);
            String path = "/registry/dataService/" + ip + ":" + port;
            byte[] data = serializeDataServerInfo(dataServerInfo);

            // Ensure CuratorFramework is started before attempting registration
            if (curatorFramework.getState() != CuratorFrameworkState.STARTED) {
                curatorFramework.start();
            }

            persistentNode = new PersistentNode(
                    curatorFramework,
                    CreateMode.EPHEMERAL,
                    false,
                    path,
                    data
            );

            persistentNode.start();
            log.info("dataService registered to Zookeeper with path: {}", path);

        } catch (Exception e) {
            log.error("Error registering service to Zookeeper", e);
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        registToCenter();
    }

    @PreDestroy
    public void unregister() throws IOException {
        if (persistentNode != null) {
            persistentNode.close();
            log.info("dataService unregistered from Zookeeper");
        }
    }

    public List<Map<String, Integer>> getDslist() {
        return null;
    }

    private byte[] serializeDataServerInfo(DataServerInfo dataServerInfo) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsBytes(dataServerInfo);
    }
    private DataServerInfo deserializeDataServerInfo(byte[] data) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(data, DataServerInfo.class);
    }

}
