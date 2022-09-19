package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.redis.proxy.plugin.DefaultBeanFactory;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;


/**
 * Created by caojiajun on 2022/9/19
 */
@Component
public class SpringProxyBeanFactory implements ProxyBeanFactory, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(SpringProxyBeanFactory.class);

    private final DefaultBeanFactory defaultBeanFactory = new DefaultBeanFactory();
    private ApplicationContext applicationContext;

    @Override
    public <T> T getBean(Class<T> requiredType) {
        try {
            if (applicationContext != null) {
                T bean = applicationContext.getBean(requiredType);
                logger.info("get proxy from spring success, requiredType = {}", requiredType.getName());
                return bean;
            }
        } catch (Exception ignore) {
        }
        return defaultBeanFactory.getBean(requiredType);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
