package com.ksyun.campus.dataserver.domain;

import java.util.Arrays;

public class ReturnRead {
    String md5;
    byte[] content;

    public ReturnRead() {
    }

    public ReturnRead(String md5, byte[] content) {
        this.md5 = md5;
        this.content = content;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    @Override
    public String toString() {
        return "ReturnRead{" +
                "md5='" + md5 + '\'' +
                ", content=" + Arrays.toString(content) +
                '}';
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

}
