package com.netease.nim.camellia.core.discovery;

/**
 * Created by caojiajun on 2022/3/2
 */
public interface CamelliaServerHealthChecker<T> {

    boolean healthCheck(T server);
}
