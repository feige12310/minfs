package com.ksyun.campus.dataserver.domain;
// 注册信息
public class DataServerInfo {
    private String ip;
    private int port;
    private int useCapacity; // 使用 BigDecimal 存储容量
    private String rack;
    private String zone;
    private int fileTotal;

    public DataServerInfo() {
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getUseCapacity() {
        return useCapacity;
    }

    public void setUseCapacity(int useCapacity) {
        this.useCapacity = useCapacity;
    }

    public String getRack() {
        return rack;
    }

    public void setRack(String rack) {
        this.rack = rack;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public int getFileTotal() {
        return fileTotal;
    }

    public void setFileTotal(int fileTotal) {
        this.fileTotal = fileTotal;
    }

    @Override
    public String toString() {
        return "DataServerInfo{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", useCapacity=" + useCapacity +
                ", rack='" + rack + '\'' +
                ", zone='" + zone + '\'' +
                ", fileTotal=" + fileTotal +
                '}';
    }

    public DataServerInfo(String ip, int port, int useCapacity, String rack, String zone, int fileTotal) {
        this.ip = ip;
        this.port = port;
        this.useCapacity = useCapacity;
        this.rack = rack;
        this.zone = zone;
        this.fileTotal = fileTotal;
    }
}
