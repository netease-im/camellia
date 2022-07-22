package com.netease.nim.camellia.delayqueue.sdk.springboot;

import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayMsgListener;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayMsgListenerConfig;
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
        sdkConfig.setConnectTimeoutMillis(properties.getConnectTimeoutMillis());
        sdkConfig.setReadTimeoutMillis(properties.getReadTimeoutMillis());
        sdkConfig.setWriteTimeoutMillis(properties.getWriteTimeoutMillis());
        sdkConfig.setMaxIdleConnections(properties.getMaxIdleConnections());
        sdkConfig.setMaxRequests(properties.getMaxRequests());
        sdkConfig.setMaxRequestsPerHost(properties.getMaxRequestsPerHost());
        sdkConfig.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        Map<String, CamelliaDelayMsgListener> beans = applicationContext.getBeansOfType(CamelliaDelayMsgListener.class);
        CamelliaDelayQueueSdk sdk = new CamelliaDelayQueueSdk(sdkConfig);

        for (CamelliaDelayMsgListener listener : beans.values()) {
            com.netease.nim.camellia.delayqueue.sdk.springboot.CamelliaDelayMsgListenerConfig config = listener.getClass().getAnnotation(com.netease.nim.camellia.delayqueue.sdk.springboot.CamelliaDelayMsgListenerConfig.class);
            if (config != null) {
                CamelliaDelayMsgListenerConfig listenerConfig = new CamelliaDelayMsgListenerConfig();
                listenerConfig.setAckTimeoutMillis(config.ackTimeoutMillis());
                listenerConfig.setPullBatch(config.pullBatch());
                listenerConfig.setPullThreads(config.pullThreads());
                listenerConfig.setPullIntervalTimeMillis(config.pullIntervalTimeMillis());
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
