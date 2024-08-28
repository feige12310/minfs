package com.ksyun.campus.client.domain;

import java.util.List;

public class StatInfoWithMD5
{
    public String path;
    public long size;
    public long mtime;
    public FileType type;
    private List<ReplicaData> replicaData;
    private String md5;

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public StatInfoWithMD5() {}

    public StatInfoWithMD5(String path, long size, long mtime, FileType type, List<ReplicaData> replicaData, String md5) {
        this.path = path;
        this.size = size;
        this.mtime = mtime;
        this.type = type;
        this.replicaData = replicaData;
        this.md5 = md5;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getMtime() {
        return mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public List<ReplicaData> getReplicaData() {
        return replicaData;
    }

    public void setReplicaData(List<ReplicaData> replicaData) {
        this.replicaData = replicaData;
    }

    @Override
    public String toString() {
        return "StatInfoWithMD5{" +
                "path='" + path + '\'' +
                ", size=" + size +
                ", mtime=" + mtime +
                ", type=" + type +
                ", replicaData=" + replicaData +
                ", md5='" + md5 + '\'' +
                '}';
    }
}
