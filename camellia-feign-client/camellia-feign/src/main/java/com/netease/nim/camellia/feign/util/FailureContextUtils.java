package com.netease.nim.camellia.feign.util;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.feign.CamelliaFeignFailureContext;

import java.nio.charset.StandardCharsets;

/**
 * Created by caojiajun on 2022/7/5
 */
public class FailureContextUtils {

    /**
     * 序列化，exception信息会丢失
     */
    public static byte[] serialize(CamelliaFeignFailureContext failureContext) {
        JSONObject json = new JSONObject();
        json.put("bid", failureContext.getBid());
        json.put("bgroup", failureContext.getBgroup());
        json.put("operationType", failureContext.getOperationType());
        json.put("apiType", failureContext.getApiType().getName());
        json.put("resource", failureContext.getResource().getUrl());
        json.put("loadBalanceKey", failureContext.getLoadBalanceKey());
        json.put("method", failureContext.getMethod());
        json.put("objects", failureContext.getObjects());
        return json.toJSONString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 反序列化
     */
    public static CamelliaFeignFailureContext deserialize(byte[] data) {
        try {
            JSONObject jsonObject = JSONObject.parseObject(new String(data, StandardCharsets.UTF_8));
            long bid = jsonObject.getLongValue("bid");
            String bgroup = jsonObject.getString("bgroup");
            Class<?> apiType = Class.forName(jsonObject.getString("apiType"));
            byte operationType = jsonObject.getByteValue("operationType");
            String resourceUrl = jsonObject.getString("resource");
            Object loadBalanceKey = jsonObject.get("loadBalanceKey");
            String method = jsonObject.getString("method");
            Object[] objects = jsonObject.getObject("objects", Object[].class);
            return new CamelliaFeignFailureContext(bid, bgroup, apiType, operationType, new Resource(resourceUrl), loadBalanceKey, method, objects, null);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
