package com.netease.nim.camellia.config.springboot;

import com.netease.nim.camellia.config.conf.ConfigProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * Created by caojiajun on 2019/11/21.
 */
@Configuration
@EnableConfigurationProperties({CamelliaConfigConfiguration.class})
public class CamelliaConfigConfigurationStarter {

    @Bean
    public ConfigProperties configConfiguration(CamelliaConfigConfiguration configuration) {
        ConfigProperties dashboardProperties = new ConfigProperties();
        dashboardProperties.setLocalCacheExpireSeconds(configuration.getLocalCacheExpireSeconds());
        dashboardProperties.setStatsExpireSeconds(configuration.getStatsExpireSeconds());
        dashboardProperties.setStatsKeyExpireHours(configuration.getStatsKeyExpireHours());
        dashboardProperties.setDaoCacheExpireSeconds(configuration.getDaoCacheExpireSeconds());
        return dashboardProperties;
    }
}
