package com.netease.nim.camellia.cache.core.boot;

import org.aopalliance.intercept.MethodInvocation;

public class CamelliaCacheThreadLocal {
    public static ThreadLocal<MethodInvocation> invocationThreadLocal = new ThreadLocal<>();
}
