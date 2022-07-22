package com.netease.nim.camellia.delayqueue.sdk.springboot;

import com.netease.nim.camellia.delayqueue.common.conf.CamelliaDelayQueueConstants;

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

    long ackTimeoutMillis() default CamelliaDelayQueueConstants.ackTimeoutMillis;

    int pullBatch() default CamelliaDelayQueueConstants.pullBatch;

    int pullIntervalTimeMillis() default CamelliaDelayQueueConstants.pullIntervalTimeMillis;//轮询间隔，单位ms，默认100ms

    int pullThreads() default CamelliaDelayQueueConstants.pullThreads;//pull线程池大小，默认1
}
