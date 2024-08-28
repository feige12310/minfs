package com.ksyun.campus.client;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 基类，定义了通用的文件系统方法和变量
 * 整体的文件组织结构为以下形式
 * {namespace}/{dir}
 * /{subdir}
 * /{subdir}/file
 * /file
 */
public class FileSystem {

    //文件系统名称，可理解成命名空间，可以存在多个命名空间，多个命名空间下的文件目录结构是独立的
    protected String defaultFileSystemName;
//    private static HttpClient httpClient;

    public FileSystem() {
    }

    public FileSystem(String defaultFileSystemName) {
        this.defaultFileSystemName = defaultFileSystemName;
    }

    //    //远程调用
    protected String callRemote(String option, String url, byte[] data) throws Exception {

        if ("post".equalsIgnoreCase(option)) {
            URL initUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) initUrl.openConnection();

            // 设置请求方法为 POST
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("fileSystemName", this.defaultFileSystemName);

            // 将数据写入请求体
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = data;
                os.write(input, 0, input.length);
            }
            // 获取响应
            int responseCode2 = connection.getResponseCode();
            if (responseCode2 != 200) {
                return "";
            }
            // 读取响应内容
            String responseContent = getResponseContent(connection.getInputStream());
            connection.disconnect();
            return responseContent;
        } else if ("get".equalsIgnoreCase(option)) {
            // 发起 GET 请求
            try {
                URL initUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) initUrl.openConnection();
                connection.setRequestMethod("GET");

                // 添加自定义头部
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("fileSystemName", this.defaultFileSystemName);
                // 读取响应
                int responseCode2 = connection.getResponseCode();
                if (responseCode2 != 200) {
                    return "";
                }
                String responseContent = getResponseContent(connection.getInputStream());
                connection.disconnect();
                return responseContent;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //把输入流转化为字符串
    private String getResponseContent(InputStream content) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(content));
        StringBuilder responseContent = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseContent.append(line);
        }
        reader.close();
        return responseContent.toString();
    }
}
