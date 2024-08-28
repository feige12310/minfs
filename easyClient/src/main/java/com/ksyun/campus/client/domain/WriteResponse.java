package com.ksyun.campus.client.domain;

public class WriteResponse {
    private int code;
    private String msg;
    private DataServerInfoVO data;

    public WriteResponse() {}

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

    public DataServerInfoVO getData() {
        return data;
    }

    public void setData(DataServerInfoVO data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "WriteResponse{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }

    public WriteResponse(int code, String msg, DataServerInfoVO data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
}
