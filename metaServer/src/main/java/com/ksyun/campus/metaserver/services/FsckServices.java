package com.ksyun.campus.metaserver.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksyun.campus.metaserver.domain.CopyReplicaDataDTO;
import com.ksyun.campus.metaserver.domain.FileType;
import com.ksyun.campus.metaserver.domain.ReplicaData;
import com.ksyun.campus.metaserver.domain.StatInfoWithMD5;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@DependsOn("metaService")
public class FsckServices {

    @Autowired
    private CuratorFramework curatorFramework;

    @Value("${server.port}")
    private int port;

    @Autowired
    private DataServerWatcher dataServerWatcher;

    @Autowired
    private MetaService metaService;

    @Autowired
    private RestTemplate restTemplate;


    //@Scheduled(cron = "0 0 0 * * ?") // 每天 0 点执行
    @Scheduled(fixedRate = 2*60*1000) // 每隔 30 分钟执行一次
    public void fsckTask() {
        // todo 全量扫描文件列表
        // todo 检查文件副本数量是否正常
        // todo 更新文件副本数：3副本、2副本、单副本
        // todo 记录当前检查结果
        int maxRetries = 3; // 最大重试次数
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                if (!metaService.isLeader()) {
                    return; // 如果不是 Leader，退出任务
                }
                break; // 如果成功确认是 Leader，跳出循环
            } catch (Exception e) {
                retryCount++;
                log.warn("Failed to determine if current node is leader, retrying... (Attempt {}/{})", retryCount, maxRetries, e);

                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(3000); // 等待 3 秒后重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); // 恢复中断状态
                        log.error("Thread was interrupted during sleep", ie);
                        return;
                    }
                } else {
                    log.error("Exceeded maximum retry attempts, skipping this scheduled task.");
                    return;
                }
            }
        }

        log.info("Start FSCK !!");
        String ip = "127.0.0.1";
        String rootPath = "/data/" + ip + ":" + port; // 根路径，例如 “/data/127.0.0.1:8000”

        try {
            // 获取根目录下的所有 fileSystemName
            List<String> fileSystemNames = curatorFramework.getChildren().forPath(rootPath);

            for (String fileSystemName : fileSystemNames) {
                String fileSystemPath = rootPath + "/" + fileSystemName;

                // 对每个 fileSystemName 执行检查操作
                traverseZkNodes(fileSystemPath, fileSystemName);
            }
        } catch (Exception e) {
            log.error("Error during fsck", e);
        }
    }




    private void traverseZkNodes(String path, String fileSystemName) throws Exception {
        log.info("check fileSystemName={}", fileSystemName);
        // 获取当前节点的子节点列表
        List<String> children = curatorFramework.getChildren().forPath(path);

        // 递归遍历每个子节点
        for (String child : children) {
            String childPath = path + "/" + child;

            // 获取当前节点的数据
            byte[] data = curatorFramework.getData().forPath(childPath);
            StatInfoWithMD5 statInfoWithMD5 = deserializeStatInfo(data);

            // 在此检查 metadata 中的 filetype 和 replica 信息
            if (statInfoWithMD5.getType() == FileType.File) {
                log.info("check replicas path={}", childPath);
                checkReplicas(statInfoWithMD5.getReplicaData(), fileSystemName);

            }

            // 递归遍历子节点
            traverseZkNodes(childPath, fileSystemName);
        }
    }


    private void checkReplicas(List<ReplicaData> replicaDataLists, String fileSystemName) throws Exception {
        List<String> fileSystemNames = new ArrayList<>();
        List<String> pathList = new ArrayList<>();
        List<Integer> ports = new ArrayList<>();
        List<String> availableIpPort = new ArrayList<>();
        List<String> availablePath = new ArrayList<>();
        for (ReplicaData replica : replicaDataLists) {
            log.info("check replica={}", replica);
            // 检查存放副本的服务器是否在线
            if (isServerOnline(replica)) {
                // 使用 HTTP 接口查询该副本是否存在
                boolean exists = queryReplicaExistence(replica, fileSystemName);
                if (!exists) {
                    log.warn("Replica not found on server: {}", replica);
                    fileSystemNames.add(fileSystemName);
                    pathList.add(replica.path);
                    String ipPort = replica.dsNode;
                    ports.add(Integer.valueOf(ipPort.substring(ipPort.indexOf(':') + 1)));

                }else {
                    availableIpPort.add(replica.dsNode);
                    availablePath.add(replica.path);
                }
            } else {
                log.warn("Replica server offline: {}", replica);
            }
        }
        if (availableIpPort.isEmpty()){
            log.error("No replica on any server");
            return;
        }
        if (!ports.isEmpty())   {
            CopyReplicaDataDTO copyReplicaDataDTO = new CopyReplicaDataDTO(fileSystemNames, pathList, ports);
            if (recoveryReplica(copyReplicaDataDTO, fileSystemName, availablePath.get(0), availableIpPort.get(0))){
                log.info("Recovery replica success");
            }else {
                log.error("Recovery replica failed");
            }
        }

    }

    private boolean isServerOnline(ReplicaData server) {
        // 检查 /registry/dataService 节点，确认 server 是否在线

        return dataServerWatcher.dataServerIsOnline(server.dsNode);
    }

    public boolean queryReplicaExistence(ReplicaData replica, String fileSystemName) {
        try {
            // 构建请求的 URL
            String url = "http://" + replica.dsNode; // replica.dsNode 表示副本的服务器 URL

            // 构建请求头和请求参数
            HttpHeaders headers = new HttpHeaders();
            headers.set("fileSystemName", fileSystemName);

            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url)
                    .path("/data/exist")
                    .queryParam("path", replica.path);

            // 发送 POST 请求
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            // 判断返回的 JSON 中 "code" 是否为 200 且 "msg" 为 "ok"
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && Integer.valueOf(200).equals(responseBody.get("code"))) {
                    return true;
                }
            }

            // 如果返回的 code 不是 200 或者 msg 不是 ok，就表示副本丢失
            return false;
        } catch (Exception e) {
            // 处理请求错误
            log.error("Error querying replica existence", e);
            return false;
        }
    }

    private boolean recoveryReplica(CopyReplicaDataDTO copyReplicaDataDTO, String fileSystemName , String path, String ipPort){
        try {
            // 构建请求的 URL
            String url = "http://" + ipPort; // 使用传入的 ipPort 作为服务器 URL

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("fileSystemName", fileSystemName);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建请求体
            HttpEntity<CopyReplicaDataDTO> requestEntity = new HttpEntity<>(copyReplicaDataDTO, headers);

            // 构建完整的 URL，包括路径和查询参数
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url)
                    .path("/data/copyReplicaData")
                    .queryParam("path", path);

            // 发送 POST 请求
            ResponseEntity<String> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            /// 解析响应体
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseBody = objectMapper.readTree(response.getBody());

            // 检查返回的 code 是否为 200
            if (responseBody.has("code") && responseBody.get("code").asInt() == 200) {
                return true;
            }else {
                return false;
            }
        } catch (Exception e) {
            // 处理请求错误
            log.error("Error recovery replica", e);
            return false;
        }
    }


    private byte[] serializeStatInfo(StatInfoWithMD5 statInfoWithMD5) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsBytes(statInfoWithMD5);
    }
    private StatInfoWithMD5 deserializeStatInfo(byte[] data) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(data, StatInfoWithMD5.class);
    }
}
