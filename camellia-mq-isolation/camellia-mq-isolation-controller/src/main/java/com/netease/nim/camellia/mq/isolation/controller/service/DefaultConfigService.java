package com.netease.nim.camellia.mq.isolation.controller.service;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.conf.ApiBasedCamelliaConfig;
import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfig;
import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2024/2/20
 */
public class DefaultConfigService implements ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultConfigService.class);

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
            logger.error("parse MqIsolationConfig error", e);
            return cache.get(namespace);
        }
        try {
            MqIsolationConfigUtils.checkValid(mqIsolationConfig);
        } catch (Exception e) {
            logger.error("MqIsolationConfig checkValid error", e);
            return cache.get(namespace);
        }
        cache.put(namespace, mqIsolationConfig);
        return mqIsolationConfig;
    }

}
