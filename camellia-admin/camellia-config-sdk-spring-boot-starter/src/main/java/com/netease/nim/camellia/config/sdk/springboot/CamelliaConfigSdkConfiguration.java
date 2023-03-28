package com.netease.nim.camellia.config.sdk.springboot;

import com.netease.nim.camellia.core.conf.ApiBasedCamelliaConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by caojiajun on 2023/3/28
 */
@Configuration
@EnableConfigurationProperties({CamelliaConfigSdkProperties.class})
public class CamelliaConfigSdkConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaConfigSdkConfiguration.class);

    @Bean
    public ApiBasedCamelliaConfigFactory camelliaConfigFactory(CamelliaConfigSdkProperties properties) {
        String url = System.getProperty("camellia.config.url", properties.getUrl());
        int intervalSeconds = Integer.parseInt(System.getProperty("camellia.config.reload.interval.seconds", String.valueOf(properties.getIntervalSeconds())));
        if (url == null) {
            throw new IllegalStateException("missing url for ApiBasedCamelliaConfigFactory");
        }
        ApiBasedCamelliaConfigFactory factory = new ApiBasedCamelliaConfigFactory(url, intervalSeconds);
        logger.info("ApiBasedCamelliaConfigFactory init success, url = {}, intervalSeconds = {}", url, intervalSeconds);
        return factory;
    }
}
