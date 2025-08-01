package com.netease.nim.camellia.redis.intercept;

/**
 * Created by caojiajun on 2023/6/30
 */
public interface RedisInterceptor {

    default void before(InterceptContext context) {

    }

    default void after(InterceptContext context) {

    }
}
