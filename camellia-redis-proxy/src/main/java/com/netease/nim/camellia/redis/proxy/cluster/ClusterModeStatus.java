package com.netease.nim.camellia.redis.proxy.cluster;


/**
 * Created by caojiajun on 2022/9/30
 */
public class ClusterModeStatus {

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
}
