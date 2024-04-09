package com.netease.nim.camellia.mq.isolation.core.stats.model;


import com.alibaba.fastjson.JSONObject;

import java.util.Objects;

/**
 * Created by caojiajun on 2024/2/7
 */
public class BizKey {
    private final String namespace;
    private final String bizId;

    public BizKey(String namespace, String bizId) {
        this.namespace = namespace;
        this.bizId = bizId;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getBizId() {
        return bizId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BizKey cacheKey = (BizKey) o;
        return Objects.equals(namespace, cacheKey.namespace) && Objects.equals(bizId, cacheKey.bizId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, bizId);
    }

    private String _string = null;

    @Override
    public String toString() {
        if (_string != null) {
            return _string;
        }
        JSONObject json = new JSONObject(true);
        json.put("namespace", namespace);
        json.put("bizId", bizId);
        _string = json.toJSONString();
        return _string;
    }

    public static BizKey byString(String json) {
        JSONObject jsonObject = JSONObject.parseObject(json);
        String namespace = jsonObject.getString("namespace");
        String bidId = jsonObject.getString("bizId");
        return new BizKey(namespace, bidId);
    }
}
