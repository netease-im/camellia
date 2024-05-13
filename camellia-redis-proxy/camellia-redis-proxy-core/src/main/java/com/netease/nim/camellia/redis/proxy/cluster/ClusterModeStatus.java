package com.netease.nim.camellia.redis.proxy.cluster;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by caojiajun on 2022/9/30
 */
public class ClusterModeStatus {

    private static final Logger logger = LoggerFactory.getLogger(ClusterModeStatus.class);

    private static ClusterModeStatus.Status status = Status.NOT_INIT;

    public enum Status {
        NOT_INIT(0),
        OFFLINE(1),
        PENDING(2),
        ONLINE(3),
        ;
        private final int value;

        Status(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Status getByValue(int value) {
            for (Status status : Status.values()) {
                if (status.value == value) {
                    return status;
                }
            }
            return null;
        }
    }

    public static ClusterModeStatus.Status getStatus() {
        return status;
    }

    public static void setStatus(ClusterModeStatus.Status status) {
        ClusterModeStatus.status = status;
    }

    private static final Set<Runnable> callbackSet = new HashSet<>();

    public static synchronized void registerClusterModeSlotRefreshCallback(Runnable callback) {
        callbackSet.add(callback);
    }

    public static synchronized void invokeClusterModeSlotRefreshCallback() {
        for (Runnable runnable : callbackSet) {
            try {
                runnable.run();
            } catch (Exception e) {
                logger.error("cluster mode slot refresh callback error", e);
            }
        }
    }

}
