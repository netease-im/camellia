package com.netease.nim.camellia.redis.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Created by caojiajun on 2020/3/6.
 */
public class CamelliaRedisProxyContext {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisProxyContext.class);
    private static CamelliaRedisProxyFactory factory = null;

    public static void register(CamelliaRedisProxyFactory factory) {
        if (CamelliaRedisProxyContext.factory != null) {
            logger.warn("CamelliaRedisProxyFactory has registered, old one will be replaced by new one");
        }
        CamelliaRedisProxyContext.factory = factory;
    }

    public static CamelliaRedisProxyFactory getFactory() {
        return factory;
    }
}
