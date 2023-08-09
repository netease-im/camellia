package com.netease.nim.camellia.redis.base.proxy;

import java.util.Objects;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
public final class Proxy {
    private String host;
    private int port;
    private int tlsPort;

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

    public int getTlsPort() {
        return tlsPort;
    }

    public void setTlsPort(int tlsPort) {
        this.tlsPort = tlsPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Proxy proxy = (Proxy) o;
        return port == proxy.port && tlsPort == proxy.tlsPort && Objects.equals(host, proxy.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, tlsPort);
    }

    @Override
    public String toString() {
        return "Proxy[" + host + ":" + port + "]";
    }
}
