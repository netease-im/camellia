package com.netease.nim.camellia.mq.isolation.controller.service;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.conf.ApiBasedCamelliaConfig;
import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfig;
import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfigUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2024/2/20
 */
public class DefaultConfigService implements ConfigService {

    private final ConcurrentHashMap<String, MqIsolationConfig> cache = new ConcurrentHashMap<>();
    private final ApiBasedCamelliaConfig config;

    public DefaultConfigService(String url, String namespace, int intervalSeconds) {
        config = new ApiBasedCamelliaConfig(url, namespace, intervalSeconds);
    }

    @Override
    public MqIsolationConfig getMqIsolationConfig(String namespace) {
        String string = config.getString(namespace, null);
        if (string == null) {
            throw new IllegalArgumentException("mq isolation config = " + namespace + " not exists");
        }
        MqIsolationConfig mqIsolationConfig;
        try {
            mqIsolationConfig = JSONObject.parseObject(string, MqIsolationConfig.class);
        } catch (Exception e) {
            return cache.get(namespace);
        }
        try {
            MqIsolationConfigUtils.checkValid(mqIsolationConfig);
        } catch (Exception e) {
            return cache.get(namespace);
        }
        cache.put(namespace, mqIsolationConfig);
        return mqIsolationConfig;
    }

}
