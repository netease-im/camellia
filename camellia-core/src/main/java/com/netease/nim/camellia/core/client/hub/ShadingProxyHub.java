package com.netease.nim.camellia.core.client.hub;


import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.util.MathUtil;

import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/5/16.
 */
public class ShadingProxyHub<T> implements IProxyHub<T> {

    private int bucketSize;
    private Object[] proxyArray;
    private boolean is2Power;
    private ProxyEnv env;

    public ShadingProxyHub(int bucketSize, Map<Integer, T> proxyMap, ProxyEnv env) {
        if (env == null) {
            throw new IllegalArgumentException("env is null");
        }
        if (env.getShadingFunc() == null) {
            throw new IllegalArgumentException("shadingFunc is null");
        }
        if (proxyMap == null) {
            throw new IllegalArgumentException("proxyMap is null");
        }
        if (bucketSize != proxyMap.size()) {
            throw new IllegalArgumentException("shadingTable/proxyMap not match");
        }
        proxyArray = new Object[bucketSize];
        for (int i=0; i<bucketSize; i++) {
            T proxy = proxyMap.get(i);
            if (proxy == null) {
                throw new IllegalArgumentException("missing proxy index = " + i);
            }
            proxyArray[i] = proxy;
        }
        this.bucketSize = bucketSize;
        this.is2Power = MathUtil.is2Power(bucketSize);
        this.env = env;
    }

    @Override
    public T chooseProxy(byte[]... key) {
        int code = env.getShadingFunc().shadingCode(key);
        if (code < 0) {
            code = -code;
        }
        int index = MathUtil.mod(is2Power, code, bucketSize);
        return (T) proxyArray[index];
    }

}
