package com.netease.nim.camellia.mq.isolation.controller.service;

import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfig;

/**
 * Created by caojiajun on 2024/2/20
 */
public interface ConfigService {

    MqIsolationConfig getMqIsolationConfig(String namespace);

}
