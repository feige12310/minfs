package com.ksyun.campus.client.domain;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DataServerInfoVO {
    private String ip;
    private int port;
    private String path;             // 当前副本存入数据的路径
    public long mtime;               // 时间戳
    private int fileSize;            // 本路径存储的文件大小
    private FileType fileType;       // 当前存入的文件类型   应该只需要考虑文件和目录两种
    private ReplicaData replicaData; // 当前副本的id，dsNode（ip+port）和路径

    public DataServerInfoVO() {}

    public DataServerInfoVO(String ip, int port, String path, long mtime, int fileSize, FileType fileType, ReplicaData replicaData) {
        this.ip = ip;
        this.port = port;
        this.path = path;
        this.mtime = mtime;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.replicaData = replicaData;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getMtime() {
        return mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public ReplicaData getReplicaData() {
        return replicaData;
    }

    public void setReplicaData(ReplicaData replicaData) {
        this.replicaData = replicaData;
    }

    @Override
    public String toString() {
        return "DataServerInfo{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", path='" + path + '\'' +
                ", mtime=" + mtime +
                ", fileSize=" + fileSize +
                ", fileType=" + fileType +
                ", replicaData=" + replicaData +
                '}';
    }
}
