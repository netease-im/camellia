package com.netease.nim.camellia.feign.discovery;

import java.util.Objects;

/**
 * Created by caojiajun on 2022/3/1
 */
public class FeignServerInfo {
    private String host;
    private int port;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeignServerInfo that = (FeignServerInfo) o;
        return port == that.port && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return "ServerInfo{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
