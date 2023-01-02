package com.netease.nim.camellia.redis.proxy.plugin;

/**
 * Created by caojiajun on 2022/9/16
 */
public interface ProxyBeanFactory {

    <T> T getBean(Class<T> requiredType);
}
