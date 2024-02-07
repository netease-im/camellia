package com.netease.nim.camellia.mq.isolation.core.stats.model;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.mq.isolation.core.executor.MsgHandlerResult;

import java.util.Objects;

/**
 * Created by caojiajun on 2024/2/7
 */
public class BizResultKey {
    private final String namespace;
    private final String bidId;
    private final MsgHandlerResult result;

    public BizResultKey(String namespace, String bidId, MsgHandlerResult result) {
        this.namespace = namespace;
        this.bidId = bidId;
        this.result = result;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getBidId() {
        return bidId;
    }

    public MsgHandlerResult getResult() {
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BizResultKey that = (BizResultKey) o;
        return Objects.equals(namespace, that.namespace) && Objects.equals(bidId, that.bidId) && result == that.result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, bidId, result);
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
        json.put("result", result.getValue());
        _string = json.toJSONString();
        return _string;
    }

    public static BizResultKey byString(String json) {
        JSONObject jsonObject = JSONObject.parseObject(json);
        String namespace = jsonObject.getString("namespace");
        String bidId = jsonObject.getString("bidId");
        int resultV = jsonObject.getIntValue("result");
        return new BizResultKey(namespace, bidId, MsgHandlerResult.byValue(resultV));
    }
}
