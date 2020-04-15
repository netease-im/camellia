package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.core.util.SysUtils;

/**
 * 一些默认配置项
 * Created by caojiajun on 2020/2/5.
 */
public class Constants {

    public static class Server {
        public static final int severPort = 6379;
        public static final int consolePort = 16379;
        public static final boolean monitorEnable = false;
        public static final int monitorIntervalSeconds = 60;

        public static final int asyncWorkThread = SysUtils.getCpuNum();
        public static final int syncWorkThread = SysUtils.getCpuNum() * 32;
        public static final int commandDecodeMaxBatchSize = 256;
    }

    public static class Sync {
        public static final int jedisPoolMaxActive = Server.syncWorkThread;
        public static final int jedisPoolMaxIdle = Server.syncWorkThread;
        public static final int jedisPoolMinIdle = 0;
        public static final int jedisMaxWaitMillis = 2000;
        public static final int jedisTimeoutMillis = 2000;
        public static final int jedisClusterPoolMaxActive = Server.syncWorkThread;
        public static final int jedisClusterPoolMaxIdle = Server.syncWorkThread;
        public static final int jedisClusterPoolMinIdle = 0;
        public static final int jedisClusterMaxWaitMillis = 2000;
        public static final int jedisClusterTimeoutMillis = 2000;
        public static final int jedisClusterMaxAttempts = 5;
        public static final int pipelinePoolSize = Server.syncWorkThread * 2;
        public static final int concurrentExecPoolSize = Server.syncWorkThread * 2;
        public static final boolean shadingConcurrentEnable = true;
        public static final int shadingConcurrentExecPoolSize = Server.syncWorkThread * 2;
        public static final boolean multiWriteConcurrentEnable = true;
        public static final int multiWriteConcurrentExecPoolSize = Server.syncWorkThread * 2;
    }

    public static class Async {
        public static final int redisClusterMaxAttempts = 5;
        public static final int heartbeatIntervalSeconds = 60;//若小于等于0则不发心跳
        public static final long heartbeatTimeoutMillis = 10000L;
        public static final int commandPipelineFlushThreshold = 1024;
        public static final int connectTimeoutMillis = 500;
        public static final int failCountThreshold = 10;
        public static final long failBanMillis = 5000L;
    }

    public static class Remote {
        public static final boolean dynamic = true;
        public static final boolean monitorEnable = false;
        public static final long checkIntervalMillis = 5000;
        public static final int connectTimeoutMillis = 10000;
        public static final int readTimeoutMillis = 60000;
    }
}
