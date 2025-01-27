package com.ksyun.campus.client.domain;

import java.util.List;

public class StatInfo
{
    public String path;
    public long size;
    public long mtime;
    public FileType type;
    public List<ReplicaData> replicaData;

    public StatInfo() {}

    public StatInfo(String path, long size, long mtime, FileType type, List<ReplicaData> replicaData) {
        this.path = path;
        this.size = size;
        this.mtime = mtime;
        this.type = type;
        this.replicaData = replicaData;
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
        return "StatInfo{" +
                "path='" + path + '\'' +
                ", size=" + size +
                ", mtime=" + mtime +
                ", type=" + type +
                ", replicaData=" + replicaData +
                '}';
    }
}
