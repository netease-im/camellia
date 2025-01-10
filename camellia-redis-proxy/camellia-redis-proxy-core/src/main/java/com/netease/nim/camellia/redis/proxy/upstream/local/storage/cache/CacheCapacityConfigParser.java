package com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2025/1/8
 */
public class CacheCapacityConfigParser {

    private static final Logger logger = LoggerFactory.getLogger(CacheCapacityConfigParser.class);

    public static long parse(String configKey, String defaultValue) {
        String string = ProxyDynamicConf.getString(configKey, defaultValue).toUpperCase();
        try {
            long bytes = bytes(string);
            if (bytes < 0) {
                logger.warn("illegal config = {} for config-key = {}, use default config = {}", string, configKey, defaultValue);
            }
            bytes = bytes(defaultValue);
            return bytes;
        } catch (Exception e) {
            logger.warn("error config = {} parse for config-key = {}, use default config = {}", string, configKey, defaultValue);
            return bytes(defaultValue);
        }
    }

    public static String toString(long capacity) {
        return (capacity / 1024 / 1024) + "M";
    }

    private static long bytes(String string) {
        int size = Integer.parseInt(string.substring(0, string.length() - 1));
        if (string.endsWith("M")) {
            return size * 1024 * 1024L;
        } else if (string.endsWith("G")) {
            return size * 1024 * 1024 * 1024L ;
        }
        return -1;
    }
}
