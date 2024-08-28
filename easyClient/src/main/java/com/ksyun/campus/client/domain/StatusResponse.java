package com.ksyun.campus.client.domain;

public class StatusResponse {
    private int code;
    private String msg;
    private StatInfoWithMD5 data;
    public StatusResponse() {}

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

    public StatInfoWithMD5 getData() {
        return data;
    }

    public void setData(StatInfoWithMD5 data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "StatusResponse{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }

    public StatusResponse(int code, String msg, StatInfoWithMD5 data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
}
