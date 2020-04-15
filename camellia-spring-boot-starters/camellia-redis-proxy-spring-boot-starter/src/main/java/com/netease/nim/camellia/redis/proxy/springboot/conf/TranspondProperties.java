package com.netease.nim.camellia.redis.proxy.springboot.conf;

import com.netease.nim.camellia.redis.proxy.conf.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
@ConfigurationProperties(prefix = "camellia-redis-proxy.transpond")
public class TranspondProperties {

    /**
     * 配置是读取的本地yml文件（静态），还是走远程的dashboard（可动态变更）
     */
    private Type type = Type.LOCAL;

    /**
     * 本地yml配置
     */
    private LocalProperties local = new LocalProperties();

    /**
     * 远程dashboard配置
     */
    private RemoteProperties remote;

    /**
     * 一些配置项
     */
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

        /**
         * 本地配置的类型
         */
        private Type type = Type.SIMPLE;

        /**
         * 简单配置下的后端资源地址
         */
        private String resource;

        /**
         * 复杂配置下的后端资源配置（是一个json文件）
         */
        private String jsonFile;

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getJsonFile() {
            return jsonFile;
        }

        public void setJsonFile(String jsonFile) {
            this.jsonFile = jsonFile;
        }

        public static enum Type {

            /**
             * 简单配置，转发的后端只有一个
             */
            SIMPLE,

            /**
             * 复杂的配置，可以配置分片、多读多写等规则
             */
            COMPLEX,
            ;
        }
    }

    public static class RemoteProperties {

        /**
         * dashboard地址
         */
        private String url;

        /**
         * 默认的bid
         */
        private long bid;

        /**
         * 默认的bgroup
         */
        private String bgroup;

        /**
         * 是否动态判断
         * 如果false，则只取默认bid和默认bgroup的配置
         * 如果是true，则会根据客户端连接传上来的bid和bgroup取配置，若没有传，则走默认bid和默认bgroup的配置
         */
        private boolean dynamic = Constants.Remote.dynamic;

        /**
         * 是否开启监控，若开启，则会把数据上报给dashboard
         */
        private boolean monitor = Constants.Remote.monitorEnable;

        /**
         * dashboard检查变更的时间间隔
         */
        private long checkIntervalMillis = Constants.Remote.checkIntervalMillis;

        /**
         * dashboard的请求connect超时
         */
        private int connectTimeoutMillis = Constants.Remote.connectTimeoutMillis;

        /**
         * dashboard的请求read超时
         */
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

        public boolean isMonitor() {
            return monitor;
        }

        public void setMonitor(boolean monitor) {
            this.monitor = monitor;
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
            private int heartbeatIntervalSeconds = Constants.Async.heartbeatIntervalSeconds;//若小于等于0则不发心跳
            private long heartbeatTimeoutMillis = Constants.Async.heartbeatTimeoutMillis;
            private int commandPipelineFlushThreshold = Constants.Async.commandPipelineFlushThreshold;
            private int connectTimeoutMillis = Constants.Async.connectTimeoutMillis;
            private int failCountThreshold = Constants.Async.failCountThreshold;
            private long failBanMillis = Constants.Async.failBanMillis;

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

            public int getCommandPipelineFlushThreshold() {
                return commandPipelineFlushThreshold;
            }

            public void setCommandPipelineFlushThreshold(int commandPipelineFlushThreshold) {
                this.commandPipelineFlushThreshold = commandPipelineFlushThreshold;
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
