package com.ksyun.campus.metaserver.controller;

import com.ksyun.campus.metaserver.common.R;
import com.ksyun.campus.metaserver.domain.DataServerInfo;
import com.ksyun.campus.metaserver.domain.ReplicaData;
import com.ksyun.campus.metaserver.domain.StatInfoWithMD5;
import com.ksyun.campus.metaserver.services.FsckServices;
import com.ksyun.campus.metaserver.services.MetaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("/")
@Slf4j
public class MetaController {
    @Autowired
    private MetaService metaService;

    @Value("${server.port}")
    private int port;

    private String ip = "127.0.0.1";

    @Autowired
    private FsckServices fsckServices;

    @GetMapping("testFsck")
    public void testFSCK(){
        log.info("controller testFsck");
        fsckServices.fsckTask();
    }



    @GetMapping("test")
    public ResponseEntity getThreeDataServes(){
        return ResponseEntity.ok(metaService.pickDataServer());
    }

    @RequestMapping("stats")
    public R stats(@RequestHeader String fileSystemName,@RequestParam String path){
        String fullPath = "/data/" + ip + ":" + port + "/" + fileSystemName + path;
        try {
            log.info("api-stats success:{}", path);
            return R.success("ok", metaService.getNodeData(fullPath));
        }catch (Exception e) {
            return R.error(400, e.getMessage());
        }

    }
    @RequestMapping("create")
    public R<List> createFile(@RequestHeader String fileSystemName, @RequestParam String path) throws Exception {
        if (!metaService.isLeader()) {
            return R.error(403, "This operation can only be performed on the leader node.");
        }

        if (!metaService.isFile(path))
            return R.error(400, "this path is not file");

        String fullPath = "/data/" + fileSystemName + path;
        if (metaService.nodeExists(fullPath)) {  // 假设 metaService 提供了 nodeExists 方法
            return R.error(409, "File already exists");
        }
        List<DataServerInfo> pickedNode = metaService.pickDataServer();
        fullPath = metaService.createNode(fileSystemName, path, pickedNode);

        // 获取并返回 ReplicaData 列表
        List<ReplicaData> replicaDataList = metaService.getNodeData(fullPath).getReplicaData();
        log.info("api-create success:{}", path);
        return R.success(fullPath, replicaDataList);
    }
    @RequestMapping("mkdir")
    public R mkdir(@RequestHeader String fileSystemName, @RequestParam String path) throws Exception {
        if (!metaService.isLeader()) {
            return R.error(403, "This operation can only be performed on the leader node.");
        }
        if (metaService.isFile(path))
            return R.error(400, "this path is not content");
        String fullPath = "/data/" + fileSystemName + path;
        if (metaService.nodeExists(fullPath)) {  // 假设 metaService 提供了 nodeExists 方法
            return R.error(409, "Dictionary already exists");
        }
        fullPath = metaService.mkdirNode(fileSystemName, path);
        log.info("api-mkdir success:{}", path);
        return R.success(fullPath);
    }
    @RequestMapping("listdir")
    public R<List> listdir(@RequestHeader String fileSystemName,@RequestParam String path) throws Exception {
        if (metaService.isFile(path))
            return R.error(400, "this path is not content");
        log.info("api-listdir success:{}", path);
        return R.success("ok", metaService.listChildren(fileSystemName, path));
    }
    @RequestMapping("delete")
    public R delete(@RequestHeader String fileSystemName, @RequestParam String path) throws Exception {
        if (!metaService.isLeader()) {
            return R.error(403, "This operation can only be performed on the leader node.");
        }
        try {
            List<StatInfoWithMD5> list = metaService.deleteNode(fileSystemName, path);
            log.info("api-delete success:{}", path);
            return R.success("ok", list);
        }catch (Exception e) {
            log.error("delete error {}", e);
            return R.error(400, e.getMessage());
        }

    }
    /**
     * 保存文件写入成功后的元数据信息，包括文件path、size、三副本信息等
     * @param fileSystemName
     * @param path
     * @param offset
     * @param length
     * @return
     */
    @RequestMapping("write")
    public R commitWrite(@RequestHeader String fileSystemName, @RequestParam String path, @RequestParam int offset, @RequestParam int length, @RequestParam String md5) throws Exception {
        if (!metaService.isLeader()) {
            return R.error(403, "This operation can only be performed on the leader node.");
        }
        metaService.writeNode(fileSystemName, path, length, md5);
        log.info("api-commit write success:{}", path);
        return R.success("ok");
    }

    /**
     * 根据文件path查询三副本的位置，返回客户端具体ds、文件分块信息
     * @param fileSystemName
     * @param path
     * @return
     */
    @RequestMapping("open")
    public R open(@RequestHeader String fileSystemName,@RequestParam String path){
        String fullPath = "/data/" + ip + ":" + port + "/" + fileSystemName + path;
        try {
            log.info("api-open success:{}", path);
            return R.success("ok", metaService.getNodeData(fullPath));
        }catch (Exception e) {
            return R.error(400, e.getMessage());
        }
    }

    /**
     * 关闭退出进程
     */
    @RequestMapping("shutdown")
    public void shutdownServer(){
        System.exit(-1);
    }

}
