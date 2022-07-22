package com.netease.nim.camellia.delayqueue.common.conf;

import com.netease.nim.camellia.core.util.SysUtils;

/**
 * Created by caojiajun on 2022/7/14
 */
public class CamelliaDelayQueueConstants {

    //一条delay消息的默认过期时间，默认1h，也就是说当一条消息达到触发时间后，如果1h内没有消费掉，则直接过期掉
    public static final long ttlMillis = 60*60*1000L;

    //一条delay消息的默认重试次数，默认10，也就是说当一条消息达到触发时间后，如果消费了10次都没能成功ack，则直接丢弃掉
    public static final int maxRetry = 10;

    //消息生命周期结束后，消息还缓存多久，默认5分钟
    public static final long endLifeMsgExpireMillis = 5*60*1000L;

    //消息每次消费的超时时间，客户端pull时可以设置，如果没有设置，则走服务器配置，服务器默认30s
    public static final long ackTimeoutMillis = 30*1000L;

    //服务器轮询判断消息是否就绪或者消费超时的轮询周期，单位ms，默认100ms
    public static final long msgScheduleMillis = 100;

    //服务器轮询定时器线程池大小，一般不需要调整
    public static final int scheduleThreadNum = SysUtils.getCpuNum();

    //轮询线程池扫描到topic后，提交给本线程池判断消息
    //服务器检查消息是否就绪的线程池大小
    public static final int checkTriggerThreadNum = SysUtils.getCpuNum() * 4;

    //轮询线程池扫描到topic后，提交给本线程池判断消息
    //服务器检查消息消费是否超时的线程池大小
    public static final int checkTimeoutThreadNum = SysUtils.getCpuNum() * 4;

    //服务器轮询判断topic是否idle的轮询周期，单位s，默认600s，也就是10分钟
    public static final long topicScheduleSeconds = 600;

    //服务器判断一个topic不活跃后，多久回收相关资源，单位ms，默认30分钟
    public static final long topicActiveTagTimeoutMillis = 30*60*1000L;

    //sdk每次pull消息的批量大小，默认1
    public static final int pullBatch = 1;

    //sdk轮询间隔，单位ms，默认100ms
    public static final int pullIntervalTimeMillis = 100;

    //sdk轮询的线程数，默认1
    public static final int pullThreads = 1;

    //sdk走发现模式时兜底的reload间隔，默认60s
    public static final int discoveryReloadIntervalSeconds = 60;


    //sdk请求server的http请求相关配置参数
    public static final long connectTimeoutMillis = 5000;
    public static final long readTimeoutMillis = 5000;
    public static final long writeTimeoutMillis = 5000;
    public static final int maxRequests = 4096;
    public static final int maxRequestsPerHost = 1024;
    public static final int maxIdleConnections = 1024;
    public static final int keepAliveSeconds = 30;
}
