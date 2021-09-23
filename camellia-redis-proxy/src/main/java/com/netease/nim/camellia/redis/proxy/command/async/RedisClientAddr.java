package com.netease.nim.camellia.redis.proxy.command.async;

import io.netty.util.concurrent.FastThreadLocal;

import java.util.Objects;

/**
 *
 * Created by caojiajun on 2020/7/10.
 */
public class RedisClientAddr {
    private final String host;
    private final int port;
    private final String userName;
    private final String password;

    private final String url;

    private final FastThreadLocal<RedisClient> cache = new FastThreadLocal<>();

    public RedisClientAddr(String host, int port, String userName, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.userName = userName;
        StringBuilder builder = new StringBuilder();
        if (userName != null && password != null) {
            builder.append(userName).append(":").append(password);
        } else if (userName == null && password != null) {
            builder.append(password);
        }
        builder.append("@").append(host).append(":").append(port);
        this.url = builder.toString();
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

    public String getUserName() {
        return userName;
    }

    public String getUrl() {
        return url;
    }

    public RedisClient getCache() {
        return cache.get();
    }

    public void setCache(RedisClient cache) {
        this.cache.set(cache);
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

    @Override
    public String toString() {
        return url;
    }
}
