package com.netease.nim.camellia.delayqueue.sdk.springboot;

import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayMsgListener;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayQueueSdk;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
/**
 * Created by logan2013 on 2025/7/7
 */
public class CamelliaDelayMsgListenerBeanPostProcessor implements BeanPostProcessor, Ordered, BeanFactoryAware {

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof CamelliaDelayMsgListener) {
            CamelliaDelayMsgListener listener = (CamelliaDelayMsgListener) bean;
            CamelliaDelayMsgListenerConfig config = listener.getClass().getAnnotation(CamelliaDelayMsgListenerConfig.class);
            if (config != null) {
                // 获取SDK实例，校验Bean是否存在
                CamelliaDelayQueueSdk sdk;
                CamelliaDelayQueueSdkProperties properties;
                try {
                    sdk = beanFactory.getBean(CamelliaDelayQueueSdk.class);
                } catch (BeansException e) {
                    throw new IllegalStateException("CamelliaDelayQueueSdk bean not found, please check your configuration", e);
                }
                
                try {
                    properties = beanFactory.getBean(CamelliaDelayQueueSdkProperties.class);
                } catch (BeansException e) {
                    throw new IllegalStateException("CamelliaDelayQueueSdkProperties bean not found, please check your configuration", e);
                }
                
                // 创建监听器配置
                com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayMsgListenerConfig listenerConfig = new com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayMsgListenerConfig();
                listenerConfig.setAckTimeoutMillis(config.ackTimeoutMillis() <= 0 ? properties.getListenerConfig().getAckTimeoutMillis() : config.ackTimeoutMillis());
                listenerConfig.setPullBatch(config.pullBatch() <= 0 ? properties.getListenerConfig().getPullBatch() : config.pullBatch());
                listenerConfig.setPullThreads(config.pullThreads() <= 0 ? properties.getListenerConfig().getPullThreads() : config.pullThreads());
                listenerConfig.setPullIntervalTimeMillis(config.pullIntervalTimeMillis() <= 0 ? properties.getListenerConfig().getPullIntervalTimeMillis() : config.pullIntervalTimeMillis());
                listenerConfig.setLongPollingEnable(config.longPollingEnable());
                listenerConfig.setConsumeThreads(config.consumeThreads() <= 0 ? properties.getListenerConfig().getConsumeThreads() : config.consumeThreads());
                listenerConfig.setLongPollingTimeoutMillis(config.longPollingTimeoutMillis() <= 0 ? properties.getListenerConfig().getLongPollingTimeoutMillis() : config.longPollingTimeoutMillis());
                
                // 注册监听器
                sdk.addMsgListener(config.topic(), listenerConfig, listener);
            }
        }
        return bean;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}