package com.netease.nim.camellia.redis.proxy.hbase.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Created by caojiajun on 2020/5/14.
 */
public class DiscoverHolder {

    private static final Logger logger = LoggerFactory.getLogger(DiscoverHolder.class);

    public static InstanceUrlGenerator instanceUrlGenerator = null;

    public static IRedisProxyHBaseRegister redisProxyHBaseRegister = null;

    public static void registerIfNeed() {
        if (redisProxyHBaseRegister != null) {
            redisProxyHBaseRegister.register();
        } else {
            logger.warn("skip register");
        }
    }

    public static void deregisterIfNeed() {
        if (redisProxyHBaseRegister != null) {
            redisProxyHBaseRegister.deregister();
        } else {
            logger.warn("skip deregister");
        }
    }

    public static int instanceCount() {
        if (redisProxyHBaseRegister != null) {
            return redisProxyHBaseRegister.instanceCount();
        } else {
            return 1;
        }
    }
}
