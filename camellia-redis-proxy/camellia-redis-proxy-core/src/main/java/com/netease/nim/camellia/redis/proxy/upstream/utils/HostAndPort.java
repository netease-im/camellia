package com.netease.nim.camellia.redis.proxy.upstream.utils;

/**
 *
 * Created by caojiajun on 2021/4/8
 */
public class HostAndPort {
    private final String host;
    private final int port;
    private final String url;

    public HostAndPort(String host, int port) {
        this.host = host;
        this.port = port;
        this.url = host + "Utils.COLON" + port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return url;
    }
}

