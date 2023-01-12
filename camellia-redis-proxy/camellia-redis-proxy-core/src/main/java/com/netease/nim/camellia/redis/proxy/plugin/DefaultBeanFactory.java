package com.netease.nim.camellia.redis.proxy.plugin;


import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2022/9/16
 */
public class DefaultBeanFactory implements ProxyBeanFactory {

    public static final DefaultBeanFactory INSTANCE = new DefaultBeanFactory();

    private final ConcurrentHashMap<Class<?>, Object> map = new ConcurrentHashMap<>();

    @Override
    public <T> T getBean(Class<T> requiredType) {
        try {
            Object o = map.get(requiredType);
            if (o == null) {
                synchronized (map) {
                    o = map.get(requiredType);
                    if (o == null) {
                        o = requiredType.getConstructor().newInstance();
                        map.put(requiredType, o);
                    }
                }
            }
            return (T) o;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

}
