package com.netease.nim.camellia.mq.isolation.stats;


import java.util.Objects;

/**
 * Created by caojiajun on 2024/2/7
 */
public class BizKey {
    private final String namespace;
    private final String bidId;

    public BizKey(String namespace, String bidId) {
        this.namespace = namespace;
        this.bidId = bidId;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getBidId() {
        return bidId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BizKey cacheKey = (BizKey) o;
        return Objects.equals(namespace, cacheKey.namespace) && Objects.equals(bidId, cacheKey.bidId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, bidId);
    }
}
