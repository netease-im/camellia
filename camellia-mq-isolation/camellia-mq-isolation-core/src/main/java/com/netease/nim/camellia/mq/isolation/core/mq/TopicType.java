package com.netease.nim.camellia.mq.isolation.core.mq;

/**
 *
 * 根据统计数据，会动态路由到FAST、SLOW、FAST_ERROR、SLOW_ERROR
 * <p>
 * 如果在FAST、SLOW、FAST_ERROR、SLOW_ERROR中执行失败了，并且期望重试，则会到RETRY_LEVEL_0
 * <p>
 * 如果重试次数过多，则会进入RETRY_LEVEL_1
 * <p>
 * 如果短时间内某个bizId的请求过多（可能是消费端发现，也可能生产端发现），则会路由到AUTO_ISOLATION_LEVEL_0
 * 有两种路由链路：
 * 1）生产端直接到AUTO_ISOLATION_LEVEL_0
 * 2）消费端拿到消息后，不做处理，直接重定向到AUTO_ISOLATION_LEVEL_0
 * <p>
 * 如果AUTO_ISOLATION_LEVEL_0里某个bizId占比过大，则会进一步路由到AUTO_ISOLATION_LEVEL_1
 * <p>
 * 可以针对某些bizId，手动路由到MANUAL_ISOLATION，此时会失去动态路由的功能
 * <p>
 * Created by caojiajun on 2024/2/4
 */
public enum TopicType {
    FAST(true),//快的
    SLOW(true),//慢的
    FAST_ERROR(true),//快的，但是有错误
    SLOW_ERROR(true),//慢的，而且有错误
    RETRY_LEVEL_0(false),//重试，到这里的都是至少已经执行过1次的任务了
    RETRY_LEVEL_1(false),//重试，到这里的都是至少已经执行过5次的任务了
    AUTO_ISOLATION_LEVEL_0(true),//自动隔离，主要是应对突发流量的情况
    AUTO_ISOLATION_LEVEL_1(false),//自动隔离，主要是应对突发流量的情况
    MANUAL_ISOLATION(false),//手动隔离
    ;

    private final boolean autoIsolation;//是否要触发自动隔离（也就是自动转移到其他topic）

    TopicType(boolean autoIsolation) {
        this.autoIsolation = autoIsolation;
    }

    public boolean isAutoIsolation() {
        return autoIsolation;
    }
}
