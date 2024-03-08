package com.netease.nim.camellia.mq.isolation.controller.service;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.conf.ApiBasedCamelliaConfig;
import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfig;
import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfigUtils;
import com.netease.nim.camellia.mq.isolation.core.config.ReadableMqIsolationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
            logger.warn("ReadableMqIsolationConfig of namespace = {} not found", namespace);
            return null;
        }
        ReadableMqIsolationConfig readableMqIsolationConfig;
        try {
            readableMqIsolationConfig = JSONObject.parseObject(string, ReadableMqIsolationConfig.class);
        } catch (Exception e) {
            logger.error("parse ReadableMqIsolationConfig error, config = {}", string, e);
            return cache.get(namespace);
        }
        MqIsolationConfig mqIsolationConfig = MqIsolationConfigUtils.toMqIsolationConfig(readableMqIsolationConfig);
        try {
            MqIsolationConfigUtils.checkValid(mqIsolationConfig);
        } catch (Exception e) {
            logger.error("MqIsolationConfig checkValid error, config = {}", string, e);
            return cache.get(namespace);
        }
        cache.put(namespace, mqIsolationConfig);
        return mqIsolationConfig;
    }

    @Override
    public List<String> listNamespaces() {
        Set<String> strings = config.getConf().keySet();
        return new ArrayList<>(strings);
    }

}
