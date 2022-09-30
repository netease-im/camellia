package com.netease.nim.camellia.redis.proxy.cluster;


import com.netease.nim.camellia.redis.proxy.netty.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2022/9/30
 */
public class ClusterModeStatus {

    private static final Logger logger = LoggerFactory.getLogger(ClusterModeStatus.class);

    private static ClusterModeStatus.Status status = Status.OFFLINE;

    public enum Status {
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
}
