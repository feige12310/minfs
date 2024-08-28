package com.ksyun.campus.metaserver.domain;

import java.util.List;

public class CopyReplicaDataDTO {
    private List<String> fileSystemNames;
    private List<String> pathList;

    private List<Integer> ports;

    public CopyReplicaDataDTO(List<String> fileSystemNames, List<String> pathList, List<Integer> ports) {
        this.fileSystemNames = fileSystemNames;
        this.pathList = pathList;
        this.ports = ports;
    }

    public List<String> getFileSystemNames() {
        return fileSystemNames;
    }

    public void setFileSystemNames(List<String> fileSystemNames) {
        this.fileSystemNames = fileSystemNames;
    }

    public List<String> getPathList() {
        return pathList;
    }

    public void setPathList(List<String> pathList) {
        this.pathList = pathList;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public void setPorts(List<Integer> ports) {
        this.ports = ports;
    }

    @Override
    public String toString() {
        return "CopyReplicaDataDTO{" +
                "fileSystemNames=" + fileSystemNames +
                ", pathList=" + pathList +
                ", ports=" + ports +
                '}';
    }
}

