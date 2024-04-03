package com.netease.nim.camellia.core.client.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by caojiajun on 2024/4/2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Retry {

    int retry() default -1;//最大重试次数

    Class<? extends RetryPolicy> retryPolicy() default RetryPolicy.NeverRetryPolicy.class;//重试策略
}
