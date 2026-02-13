package com.netease.nim.camellia.delayqueue.sdk.springboot;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.CamelliaDiscoveryFactory;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayQueueSdk;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayQueueSdkConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by caojiajun on 2022/7/21
 */
@Configuration
@EnableConfigurationProperties({CamelliaDelayQueueSdkProperties.class})
public class CamelliaDelayQueueSdkConfiguration {

    @Autowired(required = false)
    private CamelliaDiscoveryFactory discoveryFactory;

    @Bean
    public CamelliaDelayQueueSdk camelliaDelayQueueSdk(CamelliaDelayQueueSdkProperties properties) {
        CamelliaDelayQueueSdkConfig sdkConfig = new CamelliaDelayQueueSdkConfig();
        if (discoveryFactory != null && properties.getServiceName() != null) {
            sdkConfig.setDiscovery(discoveryFactory.getDiscovery(properties.getServiceName()));
        }
        sdkConfig.setUrl(properties.getUrl());
        sdkConfig.setListenerConfig(properties.getListenerConfig());
        sdkConfig.setDiscoveryReloadIntervalSeconds(properties.getDiscoveryReloadIntervalSeconds());
        sdkConfig.getHttpConfig().setConnectTimeoutMillis(properties.getHttpConfig().getConnectTimeoutMillis());
        sdkConfig.getHttpConfig().setReadTimeoutMillis(properties.getHttpConfig().getReadTimeoutMillis());
        sdkConfig.getHttpConfig().setWriteTimeoutMillis(properties.getHttpConfig().getWriteTimeoutMillis());
        sdkConfig.getHttpConfig().setMaxIdleConnections(properties.getHttpConfig().getMaxIdleConnections());
        sdkConfig.getHttpConfig().setMaxRequests(properties.getHttpConfig().getMaxRequests());
        sdkConfig.getHttpConfig().setMaxRequestsPerHost(properties.getHttpConfig().getMaxRequestsPerHost());
        sdkConfig.getHttpConfig().setKeepAliveSeconds(properties.getHttpConfig().getKeepAliveSeconds());
        return new CamelliaDelayQueueSdk(sdkConfig);
    }

    @Bean
    public CamelliaDelayMsgListenerBeanPostProcessor camelliaDelayMsgListenerBeanPostProcessor() {
        return new CamelliaDelayMsgListenerBeanPostProcessor();
    }
}
