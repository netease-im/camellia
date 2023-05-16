package com.netease.nim.camellia.hot.key.common.netty.pack;

/**
 * Created by caojiajun on 2023/5/16
 */
public class HotKeyCacheStats {

    private String namespace;
    private String key;
    private long hitCount;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }
}
