package com.netease.nim.camellia.mq.isolation.stats.model;


import com.alibaba.fastjson.JSONObject;

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

    private String _string = null;

    @Override
    public String toString() {
        if (_string != null) {
            return _string;
        }
        JSONObject json = new JSONObject(true);
        json.put("namespace", namespace);
        json.put("bidId", bidId);
        _string = json.toJSONString();
        return _string;
    }

    public static BizKey byString(String json) {
        JSONObject jsonObject = JSONObject.parseObject(json);
        String namespace = jsonObject.getString("namespace");
        String bidId = jsonObject.getString("bidId");
        return new BizKey(namespace, bidId);
    }
}
