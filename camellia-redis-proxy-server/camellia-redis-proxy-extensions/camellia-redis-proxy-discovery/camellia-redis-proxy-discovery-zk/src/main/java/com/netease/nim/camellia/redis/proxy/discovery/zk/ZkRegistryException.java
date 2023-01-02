package com.netease.nim.camellia.redis.proxy.discovery.zk;

/**
 *
 * Created by caojiajun on 2020/8/12
 */
public class ZkRegistryException extends RuntimeException {
    public ZkRegistryException() {
    }

    public ZkRegistryException(String message) {
        super(message);
    }

    public ZkRegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZkRegistryException(Throwable cause) {
        super(cause);
    }

    public ZkRegistryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
