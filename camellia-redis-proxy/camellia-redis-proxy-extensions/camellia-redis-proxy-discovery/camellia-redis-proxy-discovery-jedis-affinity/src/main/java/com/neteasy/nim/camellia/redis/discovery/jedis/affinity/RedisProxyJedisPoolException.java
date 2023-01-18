package com.neteasy.nim.camellia.redis.discovery.jedis.affinity;

public class RedisProxyJedisPoolException extends RuntimeException {

    public RedisProxyJedisPoolException() {
    }

    public RedisProxyJedisPoolException(String message) {
        super(message);
    }

    public RedisProxyJedisPoolException(String message, Throwable cause) {
        super(message, cause);
    }

    public RedisProxyJedisPoolException(Throwable cause) {
        super(cause);
    }

    public RedisProxyJedisPoolException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
