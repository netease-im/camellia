package com.netease.nim.camellia.redis.proxy.command.async;

import java.util.Objects;

/**
 *
 * Created by caojiajun on 2020/7/10.
 */
public class RedisClientAddr {
    private final String host;
    private final int port;
    private final String password;

    private final String url;

    public RedisClientAddr(String host, int port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.url = (password == null ? "" : password) + "@" + host + ":" + port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RedisClientAddr that = (RedisClientAddr) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}
