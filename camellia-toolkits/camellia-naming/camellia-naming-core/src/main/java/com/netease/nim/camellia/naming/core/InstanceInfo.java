package com.netease.nim.camellia.naming.core;

import java.util.Objects;

/**
 * Created by caojiajun on 2025/11/18
 */
public class InstanceInfo implements Comparable<InstanceInfo> {
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
        if (o == null || getClass() != o.getClass()) return false;
        InstanceInfo that = (InstanceInfo) o;
        return port == that.port && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }

    @Override
    public int compareTo(InstanceInfo o) {
        return toString().compareTo(o.toString());
    }
}
