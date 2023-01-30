package com.netease.nim.camellia.redis.base.proxy;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
public final class Proxy {
    private String host;
    private int port;

    public Proxy() {
    }

    public Proxy(String host, int port) {
        this.host = host;
        this.port = port;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Proxy proxy = (Proxy) o;

        if (port != proxy.port) return false;
        return host != null ? host.equals(proxy.host) : proxy.host == null;
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return "Proxy[" + host + ":" + port + "]";
    }
}
