package com.netease.nim.camellia.redis.proxy.sentinel;


/**
 * Created by caojiajun on 2023/12/26
 */
public class SentinelModeStatus {
    private static SentinelModeStatus.Status status = SentinelModeStatus.Status.ONLINE;

    public enum Status {
        ONLINE(1),
        OFFLINE(2),
        ;
        private final int value;

        Status(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static SentinelModeStatus.Status getByValue(int value) {
            for (SentinelModeStatus.Status status : SentinelModeStatus.Status.values()) {
                if (status.value == value) {
                    return status;
                }
            }
            return null;
        }
    }

    public static SentinelModeStatus.Status getStatus() {
        return status;
    }

    public static void setStatus(SentinelModeStatus.Status status) {
        SentinelModeStatus.status = status;
    }
}
