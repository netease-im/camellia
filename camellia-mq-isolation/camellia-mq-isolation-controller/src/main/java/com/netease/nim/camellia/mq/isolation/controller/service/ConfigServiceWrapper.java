package com.netease.nim.camellia.mq.isolation.controller.service;

import com.netease.nim.camellia.mq.isolation.controller.conf.MqIsolationControllerConfig;
import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Created by caojiajun on 2024/2/19
 */
@Service
public class ConfigServiceWrapper implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(ConfigServiceWrapper.class);

    @Autowired(required = false)
    private ConfigService service;

    @Autowired
    private MqIsolationControllerConfig config;

    public MqIsolationConfig getMqIsolationConfig(String namespace) {
        return service.getMqIsolationConfig(namespace);
    }

    @Override
    public void afterPropertiesSet() {
        if (service == null) {
            logger.info("Could not found specify ConfigService, try init DefaultConfigService");
            service = new DefaultConfigService(config.getCamelliaConfigUrl(),
                    config.getCamelliaConfigNamespace(), config.getCamelliaConfigReloadIntervalSeconds());
            logger.info("init DefaultConfigService success");
        }
    }
}
