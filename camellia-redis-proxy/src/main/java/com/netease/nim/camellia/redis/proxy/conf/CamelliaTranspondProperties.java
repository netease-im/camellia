package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.core.model.ResourceTable;

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
        private String resourceTableFilePath;
        private long checkIntervalMillis = Constants.Remote.checkIntervalMillis;

        public ResourceTable getResourceTable() {
            return resourceTable;
        }

        public void setResourceTable(ResourceTable resourceTable) {
            this.resourceTable = resourceTable;
        }

        public String getResourceTableFilePath() {
            return resourceTableFilePath;
        }

        public void setResourceTableFilePath(String resourceTableFilePath) {
            this.resourceTableFilePath = resourceTableFilePath;
        }

        public long getCheckIntervalMillis() {
            return checkIntervalMillis;
        }

        public void setCheckIntervalMillis(long checkIntervalMillis) {
            this.checkIntervalMillis = checkIntervalMillis;
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
        private int redisClusterMaxAttempts = Constants.Transpond.redisClusterMaxAttempts;
        private int heartbeatIntervalSeconds = Constants.Transpond.heartbeatIntervalSeconds;
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
