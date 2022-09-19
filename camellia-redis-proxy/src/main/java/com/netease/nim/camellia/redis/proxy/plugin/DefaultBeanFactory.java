package com.netease.nim.camellia.redis.proxy.plugin;

import com.netease.nim.camellia.redis.proxy.auth.ClientAuthByConfigProvider;

/**
 * Created by caojiajun on 2022/9/16
 */
public class DefaultBeanFactory implements ProxyBeanFactory {

    @Override
    public <T> T getBean(Class<T> requiredType) {
        if (ClientAuthByConfigProvider.class.isAssignableFrom(requiredType)) {

        }
        try {
            return requiredType.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
