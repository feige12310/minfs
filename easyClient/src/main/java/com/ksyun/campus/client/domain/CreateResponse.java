package com.ksyun.campus.client.domain;

import java.util.List;

public class CreateResponse {
    private int code;
    private String msg;
    private List<ReplicaData> data;

    public CreateResponse() {}

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<ReplicaData> getData() {
        return data;
    }

    public void setData(List<ReplicaData> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "CreateResponse{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }

    public CreateResponse(int code, String msg, List<ReplicaData> data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
}
