package com.netease.nim.camellia.redis.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2023/2/20
 */
public class RedisProxiesContext {

    private static final Logger logger = LoggerFactory.getLogger(RedisProxiesContext.class);
    private static RedisProxiesFactory factory = null;

    public static void register(RedisProxiesFactory factory) {
        if (RedisProxiesContext.factory != null) {
            logger.warn("RedisProxiesFactory has registered, old one will be replaced by new one");
        }
        RedisProxiesContext.factory = factory;
    }

    public static RedisProxiesFactory getFactory() {
        return factory;
    }
}
