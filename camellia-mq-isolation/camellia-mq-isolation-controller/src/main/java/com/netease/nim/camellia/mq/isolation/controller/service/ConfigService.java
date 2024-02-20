package com.netease.nim.camellia.mq.isolation.controller.service;

import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfig;
import org.springframework.stereotype.Service;

/**
 * Created by caojiajun on 2024/2/19
 */
@Service
public class ConfigService {

    public MqIsolationConfig getMqIsolationConfig(String namespace) {
        return new MqIsolationConfig();
    }

    public MqIsolationConfig createOrUpdate(MqIsolationConfig config) {
        return new MqIsolationConfig();
    }
}
