package com.ksyun.campus.client;

import com.ksyun.campus.client.domain.ClusterInfo;
import com.ksyun.campus.client.domain.MetaServerMsg;
import com.ksyun.campus.client.domain.StatInfo;
import com.ksyun.campus.client.server.FileSystemService;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class testZuoye {
    public static void main(String[] args) throws Exception {
//        System.out.println("========test mkdir===========");
//        testmkdir("/a/s/d/f");
////
//        System.out.println("========test create===========");
//        testcreat("/a/test.txt");
//        System.out.println("========test create===========");
//        testcreat("/a/b/test1.txt");
//        System.out.println("========test create===========");
//        testcreat("/a/c/test.txt");
//
//        System.out.println("========test open===========");
//        testopen("/a/test.txt");
//
//        System.out.println("========test getFileStats===========");
//        testgetFileStats(args[0]);
//
//        System.out.println("========test listFileStats===========");
//        testlistFileStats("/");
//
//        System.out.println("========test delete===========");
//        testdelete("/a");
//
//        System.out.println("========test getClusterInfo===========");
//        testgetClusterInfo();
//
        System.out.println("========test fsck===========");
        testfsck();
    }

    public static void testmkdir(String path) throws Exception {
        EFileSystem efs = new EFileSystem("lzf");
        if(efs.mkdir(path)){
            System.out.println("dir "+path+" create success!");
        }
    }

    public static void testfsck() throws Exception {
        FileSystemService fileSystemService = new FileSystemService();
        MetaServerMsg metaServer = fileSystemService.getMetaServer();
        String url = "http://" + metaServer.getHost() + ":" + metaServer.getPort()  + "/testFsck" ;
        System.out.println(url);
        URL initUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) initUrl.openConnection();
        connection.setRequestMethod("GET");

        // 添加自定义头部
        connection.setRequestProperty("Content-Type", "application/json");
        // 读取响应
        int responseCode2 = connection.getResponseCode();
        connection.disconnect();
    }
    public static void testopen(String path) throws Exception {
        EFileSystem efs = new EFileSystem("lzf");
        FSInputStream open = efs.open(path);
        if (open == null) {
            return;
        }
        byte[] buf = new byte[100];
        open.read(buf, 0, buf.length);
        open.close();
        System.out.println(new String(buf));
    }
    public static void testcreat(String path) throws Exception {
        EFileSystem efs = new EFileSystem("lzf");
        FSOutputStream fs=efs.create(path);
        if (fs ==null)
        {
            return;
        }
        String data="hello world!";
        byte[] input = data.getBytes(StandardCharsets.UTF_8);
        String newData=new String(input, StandardCharsets.UTF_8);
        System.out.println("write data :"+newData+"  length  "+input.length);
        fs.write(input);
        fs.close();
        System.out.println("write success!");
    }
    public static void testdelete(String path) throws Exception {
        EFileSystem efs = new EFileSystem("lzf");
        if(efs.delete(path)){
            System.out.println(path+" delete success!");
        }

    }

    public static void testgetFileStats(String path) throws Exception {
        EFileSystem efs = new EFileSystem("lzf");
        StatInfo statInfo=efs.getFileStats(path);
        if(statInfo==null)
        {
            return;
        }
        System.out.println(statInfo.toString());
    }

    public static void testlistFileStats(String path) throws Exception {
        EFileSystem efs = new EFileSystem("lzf");
        List<StatInfo> listFileStats=efs.listFileStats(path);
        if(listFileStats==null)
        {
            return;
        }
        for (StatInfo statInfo : listFileStats) {
            System.out.println(statInfo.toString());
        }
//        System.out.println(listFileStats.toString());
    }
    public static void testgetClusterInfo() throws Exception {
        EFileSystem efs = new EFileSystem("lzf");
        ClusterInfo clusterInfo=efs.getClusterInfo();
        System.out.println(clusterInfo.toString());
    }
}
