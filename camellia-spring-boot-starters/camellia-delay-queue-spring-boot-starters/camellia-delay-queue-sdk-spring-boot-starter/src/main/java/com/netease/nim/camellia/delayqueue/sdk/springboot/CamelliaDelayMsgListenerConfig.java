package com.netease.nim.camellia.delayqueue.sdk.springboot;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by caojiajun on 2022/7/21
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface CamelliaDelayMsgListenerConfig {

    String topic();

    long ackTimeoutMillis() default -1;//小于0则使用sdk默认值

    int pullBatch() default -1;//小于0则使用sdk默认值

    int pullIntervalTimeMillis() default -1;//小于0则使用sdk默认值

    int pullThreads() default -1;//小于0则使用sdk默认值
}
