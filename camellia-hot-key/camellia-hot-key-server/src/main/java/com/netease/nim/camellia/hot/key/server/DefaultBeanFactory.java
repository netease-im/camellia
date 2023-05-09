package com.netease.nim.camellia.hot.key.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class DefaultBeanFactory implements BeanFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultBeanFactory.class);

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
