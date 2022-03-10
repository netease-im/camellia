package com.netease.nim.camellia.core.discovery;

/**
 * Created by caojiajun on 2022/3/2
 */
public class DummyCamelliaServerHealthChecker<T> implements CamelliaServerHealthChecker<T> {
    @Override
    public boolean healthCheck(T server) {
        return true;
    }
}
