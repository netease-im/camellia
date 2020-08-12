package com.netease.nim.camellia.redis.zk.common;

import com.alibaba.fastjson.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 *
 * Created by caojiajun on 2020/8/12
 */
public class InstanceInfoSerializeUtil {

    public static byte[] serialize(InstanceInfo instanceInfo) {
        if (instanceInfo == null) return null;
        String string = JSONObject.toJSONString(instanceInfo);
        return string.getBytes(StandardCharsets.UTF_8);
    }

    public static InstanceInfo deserialize(byte[] data) {
        if (data == null || data.length == 0) return null;
        return JSONObject.parseObject(new String(data, StandardCharsets.UTF_8), InstanceInfo.class);
    }
}
