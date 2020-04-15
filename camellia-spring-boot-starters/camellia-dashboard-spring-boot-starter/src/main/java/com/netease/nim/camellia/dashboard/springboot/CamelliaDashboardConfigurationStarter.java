package com.netease.nim.camellia.dashboard.springboot;

import com.netease.nim.camellia.dashboard.conf.DashboardProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * Created by caojiajun on 2019/11/21.
 */
@Configuration
@EnableConfigurationProperties({CamelliaDashboardConfiguration.class})
public class CamelliaDashboardConfigurationStarter {

    @Bean
    public DashboardProperties dashboardConfiguration(CamelliaDashboardConfiguration configuration) {
        DashboardProperties dashboardProperties = new DashboardProperties();
        dashboardProperties.setLocalCacheExpireSeconds(configuration.getLocalCacheExpireSeconds());
        dashboardProperties.setStatsExpireSeconds(configuration.getStatsExpireSeconds());
        dashboardProperties.setStatsKeyExpireHours(configuration.getStatsKeyExpireHours());
        dashboardProperties.setDaoCacheExpireSeconds(configuration.getDaoCacheExpireSeconds());
        return dashboardProperties;
    }
}
