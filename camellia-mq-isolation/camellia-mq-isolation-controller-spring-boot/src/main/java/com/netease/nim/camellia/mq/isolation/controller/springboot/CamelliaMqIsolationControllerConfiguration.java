package com.netease.nim.camellia.mq.isolation.controller.springboot;

import com.netease.nim.camellia.mq.isolation.controller.conf.MqIsolationControllerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by caojiajun on 2023/3/28
 */
@Configuration
@EnableConfigurationProperties({CamelliaMqIsolationControllerProperties.class})
public class CamelliaMqIsolationControllerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaMqIsolationControllerConfiguration.class);

    @Bean
    public MqIsolationControllerConfig controllerConfig(CamelliaMqIsolationControllerProperties properties) {
        String url = System.getProperty("camellia.config.url", properties.getCamelliaConfigUrl());
        if (url == null || url.isEmpty()) {
            return new MqIsolationControllerConfig();
        }
        int intervalSeconds = Integer.parseInt(System.getProperty("camellia.config.reload.interval.seconds", String.valueOf(properties.getCamelliaConfigReloadIntervalSeconds())));
        String namespace = System.getProperty("camellia.config.namespace", properties.getCamelliaConfigNamespace());
        MqIsolationControllerConfig config = new MqIsolationControllerConfig();
        config.setCamelliaConfigUrl(url);
        config.setCamelliaConfigReloadIntervalSeconds(intervalSeconds);
        config.setCamelliaConfigNamespace(namespace);
        logger.info("MqIsolationControllerConfig init success, camelliaConfigUrl = {}, camelliaConfigReloadIntervalSeconds = {}, camelliaConfigNamespace = {}",
                url, intervalSeconds, namespace);
        return config;
    }
}
