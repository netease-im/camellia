package com.netease.nim.camellia.redis.proxy.plugin;


/**
 * Created by caojiajun on 2022/9/16
 */
public class DefaultBeanFactory implements ProxyBeanFactory {

    @Override
    public <T> T getBean(Class<T> requiredType) {
        try {
            return requiredType.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
