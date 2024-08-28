package com.ksyun.campus.metaserver.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReplicaData {
    public String id;
    public String dsNode;
    public String path;

    // 无参数构造函数
    public ReplicaData() {
    }

    // 有参数构造函数
    @JsonCreator
    public ReplicaData(
            @JsonProperty("id") String id,
            @JsonProperty("dsNode") String dsNode,
            @JsonProperty("path") String path) {
        this.id = id;
        this.dsNode = dsNode;
        this.path = path;
    }

    @Override
    public String toString() {
        return "ReplicaData{" +
                "id='" + id + '\'' +
                ", dsNode='" + dsNode + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}

