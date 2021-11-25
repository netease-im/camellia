package com.netease.nim.camellia.id.gen.sdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by caojiajun on 2021/9/29
 */
public abstract class AbstractIdGenServerDiscovery implements IdGenServerDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(AbstractIdGenServerDiscovery.class);
    private final Set<Callback> callbackSet = new HashSet<>();

    public void invokeAddCallback(IdGenServer server) {
        for (Callback callback : callbackSet) {
            try {
                callback.add(server);
            } catch (Exception e) {
                logger.error("callback add id gen server error, server = {}", server, e);
            }
        }
    }

    public void invokeRemoveCallback(IdGenServer server) {
        for (Callback callback : callbackSet) {
            try {
                callback.remove(server);
            } catch (Exception e) {
                logger.error("callback remove id gen server error, server = {}", server, e);
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
