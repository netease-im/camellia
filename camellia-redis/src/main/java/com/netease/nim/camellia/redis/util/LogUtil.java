package com.netease.nim.camellia.redis.util;

import com.netease.nim.camellia.core.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/8/9.
 */
public class LogUtil {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.class);

    public static boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public static void debugLog(Resource resource, Map<byte[], byte[]> keysvalues) {
        if (logger.isDebugEnabled()) {
            String[] keys = new String[keysvalues.size()];
            int i=0;
            for (Map.Entry<byte[], byte[]> entry : keysvalues.entrySet()) {
                keys[i] = SafeEncoder.encode(entry.getKey());
                i++;
            }
            logger.debug("{}.{}, resource = {}, keys = {}", getClassName(), getMethodName(), resource.getUrl(), keys);
        }
    }

    public static void debugLog(Resource resource, String... keys) {
        if (logger.isDebugEnabled()) {
            logger.debug("{}.{}, resource = {}, keys = {}", getClassName(), getMethodName(), resource.getUrl(), keys);
        }
    }

    public static void debugLog(String className, String methodName, Resource resource, String desc, String... keys) {
        if (logger.isDebugEnabled()) {
            logger.debug("{}.{}, resource = {}, desc = {}, keys = {}", className, methodName, resource.getUrl(), desc, keys);
        }
    }

    public static void debugLog(Resource resource, byte[]... keys) {
        if (logger.isDebugEnabled()) {
            String[] keysStr = new String[keys.length];
            for (int i=0; i<keys.length; i++) {
                keysStr[i] = SafeEncoder.encode(keys[i]);
            }
            logger.debug("{}.{}, resource = {}, keys = {}", getClassName(), getMethodName(), resource.getUrl(), keysStr);
        }
    }

    public static void debugLog(Resource resource, String key) {
        if (logger.isDebugEnabled()) {
            logger.debug("{}.{}, resource = {}, key = {}", getClassName(), getMethodName(), resource.getUrl(), key);
        }
    }

    public static void debugLog(Resource resource, byte[] key) {
        if (logger.isDebugEnabled()) {
            logger.debug("{}.{}, resource = {}, key = {}", getClassName(), getMethodName(), resource.getUrl(), SafeEncoder.encode(key));
        }
    }

    private static String getClassName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 4) {
            String className = stackTrace[3].getClassName();
            if (className.contains("$$")) {
                int index = className.indexOf("$$");
                className = className.substring(0, index);
            }
            int i = className.lastIndexOf(".");
            if (i != -1) {
                return className.substring(i + 1);
            } else {
                return className;
            }
        }
        return "null";
    }

    private static String getMethodName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 4) {
            return stackTrace[3].getMethodName();
        }
        return "null";
    }
}
