package com.ksyun.campus.client.domain;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class MetaServerMsg{
    private String host;
    private int port;

    public MetaServerMsg() {
    }

    public MetaServerMsg(String host, int port) {
        this.host = host;
        this.port = port;
    }
    public MetaServerMsg(String host, String port) {
        this.host = host;
        this.port = Integer.parseInt(port);
    }
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "MetaServerMsg{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
