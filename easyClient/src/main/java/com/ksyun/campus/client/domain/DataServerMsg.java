package com.ksyun.campus.client.domain;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class DataServerMsg{
    private String host;
    private int port;
    private int fileTotal;
    private int capacity;
    private int useCapacity;

    public DataServerMsg(){}

    public DataServerMsg(String host, int port, int fileTotal, int capacity, int useCapacity) {
        this.host = host;
        this.port = port;
        this.fileTotal = fileTotal;
        this.capacity = capacity;
        this.useCapacity = useCapacity;
    }
//    public DataServerMsg(String json) {
//        // 使用 Jackson 将 JSON 字符串转换为对象
//        ObjectMapper mapper = new ObjectMapper();
//        try {
//            DataServerMsg dataServerMsg = mapper.readValue(json, DataServerMsg.class);
//            // 将 dataServerMsg 的属性值赋给当前对象
//            this.host = dataServerMsg.getHost();
//            this.port = dataServerMsg.getPort();
//            this.fileTotal = dataServerMsg.getFileTotal();
//            this.capacity = dataServerMsg.getCapacity();
//            this.useCapacity = dataServerMsg.getUseCapacity();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getFileTotal() {
        return fileTotal;
    }

    public void setFileTotal(int fileTotal) {
        this.fileTotal = fileTotal;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getUseCapacity() {
        return useCapacity;
    }

    public void setUseCapacity(int useCapacity) {
        this.useCapacity = useCapacity;
    }

    @Override
    public String toString() {
        return "DataServerMsg{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", fileTotal=" + fileTotal +
                ", capacity=" + capacity +
                ", useCapacity=" + useCapacity +
                '}';
    }
}
