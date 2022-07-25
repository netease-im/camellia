package com.netease.nim.camellia.delayqueue.sdk.springboot;

import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayMsgListener;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayQueueSdk;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayQueueSdkConfig;
import com.netease.nim.camellia.delayqueue.sdk.api.DelayQueueServerDiscovery;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Created by caojiajun on 2022/7/21
 */
@Configuration
@EnableConfigurationProperties({CamelliaDelayQueueSdkProperties.class})
public class CamelliaDelayQueueSdkConfiguration implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Autowired(required = false)
    private DelayQueueServerDiscovery discovery;

    @Bean
    public CamelliaDelayQueueSdk camelliaDelayQueueSdk(CamelliaDelayQueueSdkProperties properties) {
        CamelliaDelayQueueSdkConfig sdkConfig = new CamelliaDelayQueueSdkConfig();
        sdkConfig.setDiscovery(discovery);
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
        Map<String, CamelliaDelayMsgListener> beans = applicationContext.getBeansOfType(CamelliaDelayMsgListener.class);
        CamelliaDelayQueueSdk sdk = new CamelliaDelayQueueSdk(sdkConfig);

        for (CamelliaDelayMsgListener listener : beans.values()) {
            CamelliaDelayMsgListenerConfig config = listener.getClass().getAnnotation(CamelliaDelayMsgListenerConfig.class);
            if (config != null) {
                com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayMsgListenerConfig listenerConfig = new com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayMsgListenerConfig();
                listenerConfig.setAckTimeoutMillis(config.ackTimeoutMillis() <= 0 ? sdkConfig.getListenerConfig().getAckTimeoutMillis() : config.ackTimeoutMillis());
                listenerConfig.setPullBatch(config.pullBatch() <= 0 ? sdkConfig.getListenerConfig().getPullBatch() : config.pullBatch());
                listenerConfig.setPullThreads(config.pullThreads() <= 0 ? sdkConfig.getListenerConfig().getPullThreads() : config.pullThreads());
                listenerConfig.setPullIntervalTimeMillis(config.pullIntervalTimeMillis() <= 0 ? sdkConfig.getListenerConfig().getPullIntervalTimeMillis() : config.pullIntervalTimeMillis());
                sdk.addMsgListener(config.topic(), listenerConfig, listener);
            }
        }
        return sdk;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        CamelliaDelayQueueSdkConfiguration.applicationContext = applicationContext;
    }

}
