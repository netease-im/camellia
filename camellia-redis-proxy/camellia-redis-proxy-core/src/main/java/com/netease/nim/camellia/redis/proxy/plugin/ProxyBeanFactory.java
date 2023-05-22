package com.netease.nim.camellia.redis.proxy.plugin;

/**
 * Created by caojiajun on 2022/9/16
 */
public interface ProxyBeanFactory {

    /**
     * Get bean from the Spring container or create bean by reflection. It depends on the implementation.
     */
    <T> T getBean(Class<T> requiredType);
}
