package com.netease.nim.camellia.redis.proxy.springboot.conf;

import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.conf.MultiWriteMode;
import com.netease.nim.camellia.redis.proxy.conf.QueueType;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
         * 复杂配置下的后端资源配置（是一个json文件），优先当做一个配置文件名，到classpath下找，如果不存在则会尝试作为一个绝对路径去找
         */
        private String jsonFile;

        /**
         * 使用复杂配置下，即使用jsonFile时，是否动态检查配置文件的变更
         */
        private boolean dynamic = false;

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

        public boolean isDynamic() {
            return dynamic;
        }

        public void setDynamic(boolean dynamic) {
            this.dynamic = dynamic;
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
        private int redisClusterMaxAttempts = Constants.Transpond.redisClusterMaxAttempts;
        private int heartbeatIntervalSeconds = Constants.Transpond.heartbeatIntervalSeconds;//若小于等于0则不发心跳
        private long heartbeatTimeoutMillis = Constants.Transpond.heartbeatTimeoutMillis;
        private int connectTimeoutMillis = Constants.Transpond.connectTimeoutMillis;
        private int failCountThreshold = Constants.Transpond.failCountThreshold;
        private long failBanMillis = Constants.Transpond.failBanMillis;
        private int commandPipelineFlushThreshold = Constants.Transpond.commandPipelineFlushThreshold;
        private int defaultTranspondWorkThread = Constants.Transpond.defaultTranspondWorkThread;
        private QueueType queueType = Constants.Transpond.queueType;
        private MultiWriteMode multiWriteMode = Constants.Transpond.multiWriteMode;
        private DisruptorConf disruptorConf;

        public static class DisruptorConf {
            private String waitStrategyClassName;

            public String getWaitStrategyClassName() {
                return waitStrategyClassName;
            }

            public void setWaitStrategyClassName(String waitStrategyClassName) {
                this.waitStrategyClassName = waitStrategyClassName;
            }
        }


        public String getShadingFunc() {
            return shadingFunc;
        }

        public void setShadingFunc(String shadingFunc) {
            this.shadingFunc = shadingFunc;
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

        public int getCommandPipelineFlushThreshold() {
            return commandPipelineFlushThreshold;
        }

        public void setCommandPipelineFlushThreshold(int commandPipelineFlushThreshold) {
            this.commandPipelineFlushThreshold = commandPipelineFlushThreshold;
        }

        public QueueType getQueueType() {
            return queueType;
        }

        public void setQueueType(QueueType queueType) {
            this.queueType = queueType;
        }

        public DisruptorConf getDisruptorConf() {
            return disruptorConf;
        }

        public void setDisruptorConf(DisruptorConf disruptorConf) {
            this.disruptorConf = disruptorConf;
        }

        public int getDefaultTranspondWorkThread() {
            return defaultTranspondWorkThread;
        }

        public void setDefaultTranspondWorkThread(int defaultTranspondWorkThread) {
            this.defaultTranspondWorkThread = defaultTranspondWorkThread;
        }

        public MultiWriteMode getMultiWriteMode() {
            return multiWriteMode;
        }

        public void setMultiWriteMode(MultiWriteMode multiWriteMode) {
            this.multiWriteMode = multiWriteMode;
        }
    }
}
