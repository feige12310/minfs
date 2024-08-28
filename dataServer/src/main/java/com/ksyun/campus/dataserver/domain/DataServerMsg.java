package com.ksyun.campus.dataserver.domain;

public class DataServerMsg {
    // 这个是直接注册到zookeeper中的
    private String host;
    private int port;
    private int fileTotal;
    private int capacity = 1024 * 1024 *1024;  // 将每个副本的最大容量设置为1GB
    private int useCapacity;

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
