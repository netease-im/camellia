package com.netease.nim.camellia.redis.proxy.upstream.connection;

import io.netty.util.concurrent.FastThreadLocal;

import java.util.Objects;

/**
 *
 * Created by caojiajun on 2020/7/10.
 */
public class RedisConnectionAddr {
    private final String host;
    private final int port;
    private final String udsPath;
    private final String userName;
    private final String password;
    private final boolean readonly;
    private final int db;

    private final String url;

    private final FastThreadLocal<RedisConnection> cache;

    public RedisConnectionAddr(String host, int port, String userName, String password) {
        this(host, port, userName, password, false);
    }

    public RedisConnectionAddr(String host, int port, String userName, String password, int db) {
        this(host, port, userName, password, false, db);
    }

    public RedisConnectionAddr(String host, int port, String userName, String password, boolean readonly) {
        this(host, port, userName, password, readonly, 0);
    }

    public RedisConnectionAddr(String host, int port, String userName, String password, boolean readonly, int db) {
        this(host, port, userName, password, readonly, db, true);
    }

    public RedisConnectionAddr(String udsPath, String userName, String password, boolean readonly, int db, boolean cacheEnable) {
        this.host = null;
        this.port = -1;
        this.udsPath = udsPath;
        this.password = password;
        this.userName = userName;
        this.readonly = readonly;
        this.db = db;
        StringBuilder builder = new StringBuilder();
        if (userName != null && password != null) {
            builder.append(userName).append(":").append(password);
        } else if (userName == null && password != null) {
            builder.append(password);
        }
        boolean withParam = false;
        if (readonly) {
            builder.append("@").append(udsPath).append("?readonly=").append(true);
            withParam = true;
        } else {
            builder.append("@").append(udsPath);
        }
        if (db > 0) {
            if (withParam) {
                builder.append("&db=").append(db);
            } else {
                builder.append("?db=").append(db);
            }
        }
        this.url = builder.toString();
        if (cacheEnable) {
            this.cache = new FastThreadLocal<>();
        } else {
            this.cache = null;
        }
    }

    public RedisConnectionAddr(String host, int port, String userName, String password, boolean readonly, int db, boolean cacheEnable) {
        this.host = host;
        this.port = port;
        this.udsPath = null;
        this.password = password;
        this.userName = userName;
        this.readonly = readonly;
        this.db = db;
        StringBuilder builder = new StringBuilder();
        if (userName != null && password != null) {
            builder.append(userName).append(":").append(password);
        } else if (userName == null && password != null) {
            builder.append(password);
        }
        boolean withParam = false;
        if (readonly) {
            builder.append("@").append(host).append(":").append(port).append("?readonly=").append(true);
            withParam = true;
        } else {
            builder.append("@").append(host).append(":").append(port);
        }
        if (db > 0) {
            if (withParam) {
                builder.append("&db=").append(db);
            } else {
                builder.append("?db=").append(db);
            }
        }
        this.url = builder.toString();
        if (cacheEnable) {
            this.cache = new FastThreadLocal<>();
        } else {
            this.cache = null;
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUdsPath() {
        return udsPath;
    }

    public String getPassword() {
        return password;
    }

    public String getUserName() {
        return userName;
    }

    public int getDb() {
        return db;
    }

    public String getUrl() {
        return url;
    }

    public RedisConnection getCache() {
        if (cache == null) return null;
        return cache.get();
    }

    public void setCache(RedisConnection cache) {
        if (this.cache == null || cache == null) return;
        this.cache.set(cache);
    }

    public boolean isReadonly() {
        return readonly;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RedisConnectionAddr that = (RedisConnectionAddr) o;
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
