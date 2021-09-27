package com.netease.nim.camellia.id.gen.common;

/**
 * Created by caojiajun on 2021/9/26
 */
public class CamelliaIdGenConstants {

    public static class Snowflake {

        //开始时间戳
        //默认：2021-09-10 00:00:00
        public static final long twepoch = 1631203200000L;

        //单元id所占的位数
        //默认为0，表示不需要单元id
        //如果为4，则表示最多支持16个单元id
        public static final  int regionBits = 0;

        //workerId所占的位数
        //默认10，表示最多支持1024个workerId
        public static final  int workerIdBits = 10;

        //sequence所占的位数
        //默认12，表示每ms最多支持生成4096个序号
        public static final  int sequenceBits = 12;

        public static class RedisWorkerIdGen {
            public static final String namespace = "camellia";

            public static final long lockExpireMillis = 3000;

            public static final long renewIntervalMillis = 1000;

            public static final boolean exitIfRenewFail = false;
        }
    }

    public static class Segment {
        //单元id所占的位数
        //默认为0，表示不需要单元id
        //如果为4，则表示最多支持16个单元id
        public static final int regionBits = 0;

        //每次从idLoader获取的id数
        public static final int step = 1000;

        //每个CamelliaSegmentIdGen包含的tag数，涉及到缓存的初始化
        public static final int tagCount = 1000;

        //maxRetry*retryIntervalMillis至少要比idLoader执行一次耗时更长
        //并发情况下重试的最大次数
        public static final int maxRetry = 100;

        //并发情况下重试间隔
        public static final long retryIntervalMillis = 10;
    }

    public static class Strict {

        //redis的key前缀
        public static final String cacheKeyPrefix = "strict";
        //并发操作时的锁超时时间
        public static final  long lockExpireMillis = 3000;
        //id在redis中的缓存时长
        public static final  int cacheExpireSeconds = 3600*24;
        //最大重试次数
        public static final  int maxRetry = 10;
        //并发情况下重试间隔
        public static final  long retryIntervalMillis = 5;
        //装填redis时的默认步长
        public static final  int defaultStep = 10;
        //装填redis时的最大步长
        public static final  int maxStep = 100;
        //id多久被用完后触发步长调整的阈值，低于该值则扩大步长，大于该值的2倍，则缩小步长
        public static final  int cacheHoldSeconds = 10;

        //单元id所占的位数
        //默认为0，表示不需要单元id
        //如果为4，则表示最多支持16个单元id，会基于数据库生成的id在右边补上4bit的单元id
        public static final  int regionBits = 0;
    }
}
