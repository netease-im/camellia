package com.netease.nim.camellia.redis.proxy.discovery.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2019/11/27.
 */
public abstract class ProxyDiscovery implements IProxyDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(ProxyDiscovery.class);
    private final Set<Callback> callbackSet = new HashSet<>();

    public void invokeAddProxyCallback(Proxy proxy) {
        for (Callback callback : callbackSet) {
            try {
                callback.add(proxy);
            } catch (Exception e) {
                logger.error("callback add proxy error, proxy = {}", proxy, e);
            }
        }
    }

    public void invokeRemoveProxyCallback(Proxy proxy) {
        for (Callback callback : callbackSet) {
            try {
                callback.remove(proxy);
            } catch (Exception e) {
                logger.error("callback remove proxy error, proxy = {}", proxy, e);
            }
        }
    }

    @Override
    public final void setCallback(Callback callback) {
        callbackSet.add(callback);
    }

    @Override
    public final void clearCallback(Callback callback) {
        callbackSet.remove(callback);
    }
}
