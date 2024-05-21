package com.netease.nim.camellia.redis.proxy.monitor.model;

/**
 * Created by caojiajun on 2024/5/21
 */
public class KVCacheStats {
    private String namespace;
    private String operation;
    private long local;
    private long redis;
    private long store;
    private double localCacheHit;
    private double redisCacheHit;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public long getLocal() {
        return local;
    }

    public void setLocal(long local) {
        this.local = local;
    }

    public long getRedis() {
        return redis;
    }

    public void setRedis(long redis) {
        this.redis = redis;
    }

    public long getStore() {
        return store;
    }

    public void setStore(long store) {
        this.store = store;
    }

    public double getLocalCacheHit() {
        return localCacheHit;
    }

    public void setLocalCacheHit(double localCacheHit) {
        this.localCacheHit = localCacheHit;
    }

    public double getRedisCacheHit() {
        return redisCacheHit;
    }

    public void setRedisCacheHit(double redisCacheHit) {
        this.redisCacheHit = redisCacheHit;
    }
}
