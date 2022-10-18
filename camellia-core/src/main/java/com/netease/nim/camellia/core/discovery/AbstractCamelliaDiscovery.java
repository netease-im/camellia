package com.netease.nim.camellia.core.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by caojiajun on 2022/3/2
 */
public abstract class AbstractCamelliaDiscovery<T> implements CamelliaDiscovery<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCamelliaDiscovery.class);
    private final Set<Callback<T>> callbackSet = new HashSet<>();

    public void invokeAddCallback(T server) {
        for (Callback<T> callback : callbackSet) {
            try {
                callback.add(server);
            } catch (Exception e) {
                logger.error("callback add error, server = {}", server, e);
            }
        }
    }

    public void invokeRemoveCallback(T server) {
        for (Callback<T> callback : callbackSet) {
            try {
                callback.remove(server);
            } catch (Exception e) {
                logger.error("callback remove error, server = {}", server, e);
            }
        }
    }

    @Override
    public final void setCallback(Callback<T> callback) {
        synchronized (callbackSet) {
            callbackSet.add(callback);
        }
    }

    @Override
    public final void clearCallback(Callback<T> callback) {
        synchronized (callbackSet) {
            callbackSet.remove(callback);
        }
    }
}
