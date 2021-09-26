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
}
