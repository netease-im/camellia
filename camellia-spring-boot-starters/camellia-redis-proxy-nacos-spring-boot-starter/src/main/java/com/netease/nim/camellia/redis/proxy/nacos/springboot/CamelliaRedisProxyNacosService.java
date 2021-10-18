package com.netease.nim.camellia.redis.proxy.nacos.springboot;

import com.alibaba.nacos.api.config.ConfigService;

/**
 * Created by caojiajun on 2021/10/18
 */
public class CamelliaRedisProxyNacosService {
    private ConfigService configService;

    public ConfigService getConfigService() {
        return configService;
    }

    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }
}
