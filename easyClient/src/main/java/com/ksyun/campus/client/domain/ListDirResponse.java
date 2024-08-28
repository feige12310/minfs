package com.ksyun.campus.client.domain;

import java.util.List;

public class ListDirResponse {
    private int code;
    private String msg;
    private List<StatInfoWithMD5> data;

    public ListDirResponse() {}

    public ListDirResponse(int code, String msg, List<StatInfoWithMD5> data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

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


    @Override
    public String toString() {
        return "ListDirResponse{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }

    public List<StatInfoWithMD5> getData() {
        return data;
    }

    public void setData(List<StatInfoWithMD5> data) {
        this.data = data;
    }
}
