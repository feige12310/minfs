package com.ksyun.campus.dataserver.controller;
import com.ksyun.campus.dataserver.domain.*;
import com.ksyun.campus.dataserver.services.DataService;
import com.ksyun.campus.dataserver.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/data")
@Slf4j
public class DataController {

    @Autowired
    private DataService dataService;
    /**
     * 1、读取request content内容并保存在本地磁盘下的文件内
     * 2、同步调用其他ds服务的write，完成另外2副本的写入
     * 3、返回写成功的结果及三副本的位置
     * @param fileSystemName
     * @param path
     * @param offset
     * @param length
     * @return
     */
    @PostMapping("/write")
    public Result<DataServerInfoVO> writeFile(@RequestHeader String fileSystemName, @RequestParam String path, @RequestParam int offset, @RequestParam int length,
                                            @RequestBody byte[] content) throws IOException {
        // 总共有四个节点 根据不同的节点去存内容
        log.info("调用写操作方法");
        while(path.startsWith("/")){
            path = path.substring(1);
        }
        DataServerInfoVO dataServerInfo = dataService.write(fileSystemName, path, offset, length, content);
        int code = dataServerInfo.getFileType().getCode();
        int fileSize = dataServerInfo.getFileSize();
        if(code==3){
            // code==3为目录 存目录成功
            return Result.success(dataServerInfo);
        }
        if(code==2 && fileSize>=0){
            // code=2为文件，创建并存入成功
            log.info("Controller：写入文件成功");
            dataService.upToZK(); // 更新zookeeper
            return Result.success(dataServerInfo);
        }
        return Result.error("写入文件失败！");
    }

    /**
     * 创建目录
     * @param fileSystemName
     * @param path
     * @return
     * @throws Exception
     */
    @PostMapping("/mkdir")
    public Result<?> mkdir(@RequestHeader String fileSystemName, @RequestParam String path) throws Exception {
        while(path.startsWith("/")){
            path = path.substring(1);
        }
        boolean boo = dataService.mkdir(fileSystemName, path);
        if(boo){
            return Result.success();
        }else{
            return Result.error("创建失败");
        }
    }

    /**
     * 在指定本地磁盘路径下，读取指定大小的内容后返回
     * @param fileSystemName
     * @param path
     * @param offset
     * @param length
     * @return
     */
    @GetMapping("/read")
    public ResponseEntity<byte[]> readFile(@RequestHeader String fileSystemName, @RequestParam String path, @RequestParam int offset,
                                               @RequestParam int length) throws UnknownHostException, NoSuchAlgorithmException {
        // 文件路径是path，从offset开始读，读length长度的内容
        while(path.startsWith("/")){
            path = path.substring(1);
        }
        byte[] content = dataService.read(fileSystemName,path, offset, length);
        log.info("读取内容：{}",content);
        if(content!=null){
            // 创建带有内容的响应实体
            return new ResponseEntity<>(content,HttpStatus.OK);
        }else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    @GetMapping("/delete")
    public Result deleteFile(@RequestHeader String fileSystemName, @RequestParam String path) throws IOException {
        while(path.startsWith("/")){
            path = path.substring(1);
        }
        boolean boo = dataService.delete(fileSystemName, path);
        if(boo){
            log.info("文件删除成功");
            dataService.upToZK(); // 更新zookeeper
            return Result.success();
        }else{
            log.info("文件删除失败");
            return Result.error("删除失败");
        }
    }

    /**
     * path中是否有数据存在？
     */
    @PostMapping("/exist")
    public Result exist(@RequestHeader String fileSystemName, @RequestParam String path){
        while(path.startsWith("/")){
            path = path.substring(1);
        }
        boolean boo = dataService.exist(fileSystemName,path);
        if(boo){
            log.info("该路径下存在文件");
            return Result.success();
        }else{
            log.info("该路径下不存在文件");
            return Result.error("该路径不存在数据");
        }
    }

    /**
     * 将path路径下的内容存储到pathList下的所有副本中
     * @param fileSystemName
     * @param
     * @param path
     * @return
     */
    @PostMapping("/copyReplicaData")
    public Result copyReplicaData(@RequestBody CopyReplicaDataDTO copyReplicaDataDTO ,
                                  @RequestHeader String fileSystemName, @RequestParam String path) throws IOException {
        while(path.startsWith("/")){
            path = path.substring(1);
        }
        boolean boo = dataService.copyReplicaData(copyReplicaDataDTO, fileSystemName, path);
        if(boo){
            log.info("副本文件复制成功");
            return Result.success();
        }else{
            log.info("副本文件复制失败");
            return Result.error("副本文件复制失败");
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
