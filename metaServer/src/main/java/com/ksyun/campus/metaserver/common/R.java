package com.ksyun.campus.metaserver.common;

public class R<T> {

    private int code;
    private String msg;
    private T data;

    // Constructors
    public R() {}

    public R(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // Getters and Setters
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

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    // Static methods for convenience
    public static <T> R<T> success(String msg, T data) {
        return new R<>(200, msg, data);
    }

    public static <T> R<T> success(String msg) {
        return new R<>(200, msg);
    }

    public static <T> R<T> error(int code, String msg) {
        return new R<>(code, msg);
    }

    public static <T> R<T> error(int code, String msg, T data) {
        return new R<>(code, msg, data);
    }
}
