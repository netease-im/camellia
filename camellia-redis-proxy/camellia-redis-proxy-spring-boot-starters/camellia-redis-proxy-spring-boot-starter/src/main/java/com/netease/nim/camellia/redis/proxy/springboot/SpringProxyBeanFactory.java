package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.redis.proxy.plugin.DefaultBeanFactory;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;


/**
 * Get bean from spring.
 */
public class SpringProxyBeanFactory implements ProxyBeanFactory {

    private static final Logger logger = LoggerFactory.getLogger(SpringProxyBeanFactory.class);

    private final DefaultBeanFactory defaultBeanFactory = DefaultBeanFactory.INSTANCE;
    private final ApplicationContext applicationContext;

    public SpringProxyBeanFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Get bean from the spring container. If {@link ApplicationContext} is not set, {@link DefaultBeanFactory} will be used.
     *
     * @param requiredType target class
     * @param <T>          type
     * @return bean
     */
    @Override
    public <T> T getBean(Class<T> requiredType) {
        try {
            if (applicationContext != null) {
                T bean = applicationContext.getBean(requiredType);
                logger.info("get bean from spring success, requiredType = {}", requiredType.getName());
                return bean;
            }
        } catch (Exception ignore) {
        }
        return defaultBeanFactory.getBean(requiredType);
    }

}
