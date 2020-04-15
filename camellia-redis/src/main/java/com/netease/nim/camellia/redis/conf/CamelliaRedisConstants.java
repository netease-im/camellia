package com.netease.nim.camellia.redis.conf;

import com.netease.nim.camellia.core.util.SysUtils;

/**
 *
 * Created by caojiajun on 2020/2/5.
 */
public class CamelliaRedisConstants {

    public static class Jedis {
        public static final int minIdle = 0;
        public static final int maxIdle = SysUtils.getCpuNum() * 8;
        public static final int maxTotal = SysUtils.getCpuNum() * 8;
        public static final int maxWaitMillis = 2000;
        public static final int timeoutMillis = 2000;
    }

    public static class JedisCluster {
        public static final int minIdle = 0;
        public static final int maxIdle = SysUtils.getCpuNum() * 3;
        public static final int maxTotal = SysUtils.getCpuNum() * 3;
        public static final int maxWaitMillis = 2000;
        public static final int connectionTimeout = 2000;
        public static final int soTimeout = 2000;
        public static final int maxAttempts = 5;
    }

    public static class Misc {
        public static final int concurrentExecPoolSize = SysUtils.getCpuNum() * 16;
        public static final int pipelinePoolSize = SysUtils.getCpuNum() * 32;
        public static final int pipelineMaxAttempts = 5;
        public static final boolean pipelineConcurrentEnable = true;
    }
}
