package com.netease.nim.camellia.core.util;

import com.netease.nim.camellia.core.client.annotation.Retry;
import com.netease.nim.camellia.core.client.annotation.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by caojiajun on 2024/4/2
 */
public class RetryInfoCache {

    private static final Logger logger = LoggerFactory.getLogger(RetryInfoCache.class);

    private final Class<?> clazz;
    private final RetryInfo retryInfo;
    private final Map<Method, RetryInfo> map = new HashMap<>();

    public RetryInfoCache(Class<?> clazz, int retry, RetryPolicy retryPolicy) {
        this.clazz = clazz;
        this.retryInfo = new RetryInfo(retry, retryPolicy);
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            getRetryInfo(method);
        }
    }

    public RetryInfo getRetryInfo(Method method) {
        RetryInfo retryInfo = map.get(method);
        if (retryInfo != null) {
            return retryInfo;
        }
        Retry annotation = method.getAnnotation(Retry.class);
        if (annotation != null) {
            try {
                int retry = annotation.retry();
                Class<? extends RetryPolicy> policy = annotation.retryPolicy();
                RetryPolicy retryPolicy = policy.getConstructor().newInstance();
                retryInfo = new RetryInfo(retry, retryPolicy);
            } catch (Throwable e) {
                retryInfo = this.retryInfo;
                logger.error("getRetryInfo error in @Retry annotation, use default, class = {}, method = {}", clazz.getName(), method.getName(), e);
            }
        } else {
            retryInfo = this.retryInfo;
        }
        map.put(method, retryInfo);
        return retryInfo;
    }

    public static class RetryInfo {
        private final int retry;
        private final RetryPolicy retryPolicy;

        public RetryInfo(int retry, RetryPolicy retryPolicy) {
            this.retry = retry;
            this.retryPolicy = retryPolicy;
        }

        public int getRetry() {
            return retry;
        }

        public RetryPolicy getRetryPolicy() {
            return retryPolicy;
        }
    }
}
