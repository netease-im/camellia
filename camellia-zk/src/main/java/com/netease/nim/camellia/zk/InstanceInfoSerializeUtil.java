package com.netease.nim.camellia.zk;

import com.alibaba.fastjson.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 *
 * Created by caojiajun on 2020/8/12
 */
public class InstanceInfoSerializeUtil {

    public static <T> byte[] serialize(InstanceInfo<T> instanceInfo) {
        if (instanceInfo == null) return null;
        String string = JSONObject.toJSONString(instanceInfo);
        return string.getBytes(StandardCharsets.UTF_8);
    }

    public static <T> InstanceInfo<T> deserialize(byte[] data, Class<T> clazz) {
        if (data == null || data.length == 0) return null;
        JSONObject json = JSONObject.parseObject(new String(data, StandardCharsets.UTF_8));
        InstanceInfo<T> instanceInfo = new InstanceInfo<>();
        instanceInfo.setRegisterTime(json.getLong("registerTime"));
        T instance = json.getObject("instance", clazz);
        instanceInfo.setInstance(instance);
        return instanceInfo;
    }
}
