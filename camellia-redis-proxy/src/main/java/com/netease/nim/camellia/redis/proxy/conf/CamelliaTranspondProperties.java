package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.core.model.ResourceTable;

import java.time.Duration;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
public class CamelliaTranspondProperties {

    private Type type = Type.LOCAL;
    private LocalProperties local = new LocalProperties();
    private RemoteProperties remote;
    private RedisConfProperties redisConf = new RedisConfProperties();

    public static enum Type {
        LOCAL,
        REMOTE,
        AUTO,
        ;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public LocalProperties getLocal() {
        return local;
    }

    public void setLocal(LocalProperties local) {
        this.local = local;
    }

    public RemoteProperties getRemote() {
        return remote;
    }

    public void setRemote(RemoteProperties remote) {
        this.remote = remote;
    }

    public RedisConfProperties getRedisConf() {
        return redisConf;
    }

    public void setRedisConf(RedisConfProperties redisConf) {
        this.redisConf = redisConf;
    }

    public static class LocalProperties {
        private ResourceTable resourceTable;

        public ResourceTable getResourceTable() {
            return resourceTable;
        }

        public void setResourceTable(ResourceTable resourceTable) {
            this.resourceTable = resourceTable;
        }
    }

    public static class RemoteProperties {

        private String url;
        private long bid;
        private String bgroup;
        private boolean dynamic = Constants.Remote.dynamic;
        private boolean monitorEnable = Constants.Remote.monitorEnable;
        private long checkIntervalMillis = Constants.Remote.checkIntervalMillis;
        private int connectTimeoutMillis = Constants.Remote.connectTimeoutMillis;
        private int readTimeoutMillis = Constants.Remote.readTimeoutMillis;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public long getBid() {
            return bid;
        }

        public void setBid(long bid) {
            this.bid = bid;
        }

        public String getBgroup() {
            return bgroup;
        }

        public void setBgroup(String bgroup) {
            this.bgroup = bgroup;
        }

        public boolean isDynamic() {
            return dynamic;
        }

        public void setDynamic(boolean dynamic) {
            this.dynamic = dynamic;
        }

        public boolean isMonitorEnable() {
            return monitorEnable;
        }

        public void setMonitorEnable(boolean monitorEnable) {
            this.monitorEnable = monitorEnable;
        }

        public long getCheckIntervalMillis() {
            return checkIntervalMillis;
        }

        public void setCheckIntervalMillis(long checkIntervalMillis) {
            this.checkIntervalMillis = checkIntervalMillis;
        }

        public int getConnectTimeoutMillis() {
            return connectTimeoutMillis;
        }

        public void setConnectTimeoutMillis(int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
        }

        public int getReadTimeoutMillis() {
            return readTimeoutMillis;
        }

        public void setReadTimeoutMillis(int readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
        }
    }

    public static class RedisConfProperties {
        //分片函数
        private String shadingFunc;

        //sync转发时的配置
        private Jedis jedis = new Jedis();
        private JedisCluster jedisCluster = new JedisCluster();
        private int pipelinePoolSize = Constants.Sync.pipelinePoolSize;
        private int concurrentExecPoolSize = Constants.Sync.concurrentExecPoolSize;
        private boolean shadingConcurrentEnable = Constants.Sync.shadingConcurrentEnable;
        private int shadingConcurrentExecPoolSize = Constants.Sync.shadingConcurrentExecPoolSize;
        private boolean multiWriteConcurrentEnable = Constants.Sync.multiWriteConcurrentEnable;
        private int multiWriteConcurrentExecPoolSize = Constants.Sync.multiWriteConcurrentExecPoolSize;

        //async转发时的配置
        private Netty netty = new Netty();

        public static class Netty {
            private int redisClusterMaxAttempts = Constants.Async.redisClusterMaxAttempts;
            private int heartbeatIntervalSeconds = Constants.Async.heartbeatIntervalSeconds;
            private long heartbeatTimeoutMillis = Constants.Async.heartbeatTimeoutMillis;
            private int commandPipelineFlushThreshold = Constants.Async.commandPipelineFlushThreshold;
            private int connectTimeoutMillis = Constants.Async.connectTimeoutMillis;
            private int failCountThreshold = Constants.Async.failCountThreshold;
            private long failBanMillis = Constants.Async.failBanMillis;

            public Netty() {
            }

            public Netty(int redisClusterMaxAttempts, int heartbeatIntervalSeconds,
                         long heartbeatTimeoutMillis, int commandPipelineFlushThreshold,
                         int connectTimeoutMillis, int failCountThreshold, long failBanMillis) {
                this.redisClusterMaxAttempts = redisClusterMaxAttempts;
                this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
                this.heartbeatTimeoutMillis = heartbeatTimeoutMillis;
                this.commandPipelineFlushThreshold = commandPipelineFlushThreshold;
                this.connectTimeoutMillis = connectTimeoutMillis;
                this.failCountThreshold = failCountThreshold;
                this.failBanMillis = failBanMillis;
            }

            public int getCommandPipelineFlushThreshold() {
                return commandPipelineFlushThreshold;
            }

            public void setCommandPipelineFlushThreshold(int commandPipelineFlushThreshold) {
                this.commandPipelineFlushThreshold = commandPipelineFlushThreshold;
            }

            public int getRedisClusterMaxAttempts() {
                return redisClusterMaxAttempts;
            }

            public void setRedisClusterMaxAttempts(int redisClusterMaxAttempts) {
                this.redisClusterMaxAttempts = redisClusterMaxAttempts;
            }

            public int getHeartbeatIntervalSeconds() {
                return heartbeatIntervalSeconds;
            }

            public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
                this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
            }

            public long getHeartbeatTimeoutMillis() {
                return heartbeatTimeoutMillis;
            }

            public void setHeartbeatTimeoutMillis(long heartbeatTimeoutMillis) {
                this.heartbeatTimeoutMillis = heartbeatTimeoutMillis;
            }

            public int getConnectTimeoutMillis() {
                return connectTimeoutMillis;
            }

            public void setConnectTimeoutMillis(int connectTimeoutMillis) {
                this.connectTimeoutMillis = connectTimeoutMillis;
            }

            public int getFailCountThreshold() {
                return failCountThreshold;
            }

            public void setFailCountThreshold(int failCountThreshold) {
                this.failCountThreshold = failCountThreshold;
            }

            public long getFailBanMillis() {
                return failBanMillis;
            }

            public void setFailBanMillis(long failBanMillis) {
                this.failBanMillis = failBanMillis;
            }
        }

        public static class Jedis {
            private int maxIdle = Constants.Sync.jedisPoolMaxIdle;
            private int minIdle = Constants.Sync.jedisPoolMinIdle;
            private int maxActive = Constants.Sync.jedisPoolMaxActive;
            private Duration maxWait = Duration.ofMillis(Constants.Sync.jedisMaxWaitMillis);
            private Duration timeout = Duration.ofMillis(Constants.Sync.jedisTimeoutMillis);

            public Jedis() {
            }

            public Jedis(int maxIdle, int minIdle, int maxActive, Duration maxWait, Duration timeout) {
                this.maxIdle = maxIdle;
                this.minIdle = minIdle;
                this.maxActive = maxActive;
                this.maxWait = maxWait;
                this.timeout = timeout;
            }

            public int getMaxIdle() {
                return maxIdle;
            }

            public void setMaxIdle(int maxIdle) {
                this.maxIdle = maxIdle;
            }

            public int getMinIdle() {
                return minIdle;
            }

            public void setMinIdle(int minIdle) {
                this.minIdle = minIdle;
            }

            public int getMaxActive() {
                return maxActive;
            }

            public void setMaxActive(int maxActive) {
                this.maxActive = maxActive;
            }

            public Duration getMaxWait() {
                return maxWait;
            }

            public void setMaxWait(Duration maxWait) {
                this.maxWait = maxWait;
            }

            public Duration getTimeout() {
                return timeout;
            }

            public void setTimeout(Duration timeout) {
                this.timeout = timeout;
            }
        }

        public static class JedisCluster {
            private int maxIdle = Constants.Sync.jedisClusterPoolMaxIdle;
            private int minIdle = Constants.Sync.jedisClusterPoolMinIdle;
            private int maxActive = Constants.Sync.jedisClusterPoolMaxActive;
            private Duration maxWait = Duration.ofMillis(Constants.Sync.jedisClusterMaxWaitMillis);
            private Duration timeout = Duration.ofMillis(Constants.Sync.jedisClusterTimeoutMillis);
            private int maxAttempts = Constants.Sync.jedisClusterMaxAttempts;

            public JedisCluster() {
            }

            public JedisCluster(int maxIdle, int minIdle, int maxActive, Duration maxWait, Duration timeout, int maxAttempts) {
                this.maxIdle = maxIdle;
                this.minIdle = minIdle;
                this.maxActive = maxActive;
                this.maxWait = maxWait;
                this.timeout = timeout;
                this.maxAttempts = maxAttempts;
            }

            public int getMaxIdle() {
                return maxIdle;
            }

            public void setMaxIdle(int maxIdle) {
                this.maxIdle = maxIdle;
            }

            public int getMinIdle() {
                return minIdle;
            }

            public void setMinIdle(int minIdle) {
                this.minIdle = minIdle;
            }

            public int getMaxActive() {
                return maxActive;
            }

            public void setMaxActive(int maxActive) {
                this.maxActive = maxActive;
            }

            public Duration getMaxWait() {
                return maxWait;
            }

            public void setMaxWait(Duration maxWait) {
                this.maxWait = maxWait;
            }

            public Duration getTimeout() {
                return timeout;
            }

            public void setTimeout(Duration timeout) {
                this.timeout = timeout;
            }

            public int getMaxAttempts() {
                return maxAttempts;
            }

            public void setMaxAttempts(int maxAttempts) {
                this.maxAttempts = maxAttempts;
            }
        }

        public Netty getNetty() {
            return netty;
        }

        public void setNetty(Netty netty) {
            this.netty = netty;
        }

        public Jedis getJedis() {
            return jedis;
        }

        public void setJedis(Jedis jedis) {
            this.jedis = jedis;
        }

        public JedisCluster getJedisCluster() {
            return jedisCluster;
        }

        public void setJedisCluster(JedisCluster jedisCluster) {
            this.jedisCluster = jedisCluster;
        }

        public String getShadingFunc() {
            return shadingFunc;
        }

        public void setShadingFunc(String shadingFunc) {
            this.shadingFunc = shadingFunc;
        }

        public int getPipelinePoolSize() {
            return pipelinePoolSize;
        }

        public void setPipelinePoolSize(int pipelinePoolSize) {
            this.pipelinePoolSize = pipelinePoolSize;
        }

        public int getConcurrentExecPoolSize() {
            return concurrentExecPoolSize;
        }

        public void setConcurrentExecPoolSize(int concurrentExecPoolSize) {
            this.concurrentExecPoolSize = concurrentExecPoolSize;
        }

        public int getShadingConcurrentExecPoolSize() {
            return shadingConcurrentExecPoolSize;
        }

        public void setShadingConcurrentExecPoolSize(int shadingConcurrentExecPoolSize) {
            this.shadingConcurrentExecPoolSize = shadingConcurrentExecPoolSize;
        }

        public int getMultiWriteConcurrentExecPoolSize() {
            return multiWriteConcurrentExecPoolSize;
        }

        public void setMultiWriteConcurrentExecPoolSize(int multiWriteConcurrentExecPoolSize) {
            this.multiWriteConcurrentExecPoolSize = multiWriteConcurrentExecPoolSize;
        }

        public boolean isShadingConcurrentEnable() {
            return shadingConcurrentEnable;
        }

        public void setShadingConcurrentEnable(boolean shadingConcurrentEnable) {
            this.shadingConcurrentEnable = shadingConcurrentEnable;
        }

        public boolean isMultiWriteConcurrentEnable() {
            return multiWriteConcurrentEnable;
        }

        public void setMultiWriteConcurrentEnable(boolean multiWriteConcurrentEnable) {
            this.multiWriteConcurrentEnable = multiWriteConcurrentEnable;
        }
    }
}
