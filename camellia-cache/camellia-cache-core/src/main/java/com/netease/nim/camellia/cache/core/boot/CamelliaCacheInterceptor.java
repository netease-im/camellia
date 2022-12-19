package com.netease.nim.camellia.cache.core.boot;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.cache.interceptor.CacheInterceptor;

/**
 * @see CacheInterceptor
 */
public class CamelliaCacheInterceptor extends CacheInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        CamelliaCacheThreadLocal.invocationThreadLocal.set(invocation);
        return super.invoke(invocation);
    }

}
