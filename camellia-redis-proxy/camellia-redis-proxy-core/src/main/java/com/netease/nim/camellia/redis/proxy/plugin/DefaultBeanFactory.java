package com.netease.nim.camellia.redis.proxy.plugin;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Get the instance by reflection.
 * Created by caojiajun on 2022/9/16
 */
public class DefaultBeanFactory implements ProxyBeanFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultBeanFactory.class);

    /**
     * 饿汉单例
     */
    public static final DefaultBeanFactory INSTANCE = new DefaultBeanFactory();

    /**
     * 反射的类实例缓存，和DCL配合使用
     */
    private final ConcurrentHashMap<Class<?>, Object> map = new ConcurrentHashMap<>();

    /**
     * DCL保证返回的单例，也就是说多次调用返回的是同一个对象，内部通过反射做的
     * @param requiredType class对象
     * @param <T> 目标类
     * @return 目标类对象
     */
    @Override
    public <T> T getBean(Class<T> requiredType) {
        try {
            Object o = map.get(requiredType);
            if (o == null) {
                synchronized (map) {
                    o = map.get(requiredType);
                    if (o == null) {
                        logger.info("try init {}", requiredType.getName());
                        o = requiredType.getConstructor().newInstance();
                        logger.info("init {} success", requiredType.getName());
                        map.put(requiredType, o);
                    }
                }
            }
            return (T) o;
        } catch (Throwable e) {
            logger.error("init {} error", requiredType.getName(), e);
            throw new IllegalArgumentException(e);
        }
    }

}
