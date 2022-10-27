package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.core.model.ResourceTable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by caojiajun on 2019/11/6.
 */
public class CamelliaTranspondProperties {

    private Type type = Type.LOCAL;
    private LocalProperties local = new LocalProperties();
    private RemoteProperties remote;
    private CustomProperties custom;
    private RedisConfProperties redisConf = new RedisConfProperties();
    private NettyProperties nettyProperties = new NettyProperties();

    public static enum Type {
        LOCAL,
        REMOTE,
        CUSTOM,
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

    public CustomProperties getCustom() {
        return custom;
    }

    public void setCustom(CustomProperties custom) {
        this.custom = custom;
    }

    public RedisConfProperties getRedisConf() {
        return redisConf;
    }

    public void setRedisConf(RedisConfProperties redisConf) {
        this.redisConf = redisConf;
    }

    public NettyProperties getNettyProperties() {
        return nettyProperties;
    }

    public void setNettyProperties(NettyProperties nettyProperties) {
        this.nettyProperties = nettyProperties;
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
        private Map<String, String> headerMap = new HashMap<>();

        public Map<String, String> getHeaderMap() {
            return headerMap;
        }

        public void setHeaderMap(Map<String, String> headerMap) {
            this.headerMap = headerMap;
        }

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

    public static class CustomProperties {
        private long bid;
        private String bgroup;
        private boolean dynamic = Constants.Custom.dynamic;
        private long reloadIntervalMillis = Constants.Custom.reloadIntervalMillis;
        private String proxyRouteConfUpdaterClassName = Constants.Custom.proxyRouteConfUpdaterClassName;

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

        public long getReloadIntervalMillis() {
            return reloadIntervalMillis;
        }

        public void setReloadIntervalMillis(long reloadIntervalMillis) {
            this.reloadIntervalMillis = reloadIntervalMillis;
        }

        public String getProxyRouteConfUpdaterClassName() {
            return proxyRouteConfUpdaterClassName;
        }

        public void setProxyRouteConfUpdaterClassName(String proxyRouteConfUpdaterClassName) {
            this.proxyRouteConfUpdaterClassName = proxyRouteConfUpdaterClassName;
        }
    }

    public static class NettyProperties {
        public int soSndbuf = Constants.Transpond.soSndbuf;
        public int soRcvbuf = Constants.Transpond.soRcvbuf;
        public boolean tcpNoDelay = Constants.Transpond.tcpNoDelay;
        public boolean soKeepalive = Constants.Transpond.soKeepalive;
        public int writeBufferWaterMarkLow = Constants.Transpond.writeBufferWaterMarkLow;
        public int writeBufferWaterMarkHigh = Constants.Transpond.writeBufferWaterMarkHigh;

        public int getSoSndbuf() {
            return soSndbuf;
        }

        public void setSoSndbuf(int soSndbuf) {
            this.soSndbuf = soSndbuf;
        }

        public int getSoRcvbuf() {
            return soRcvbuf;
        }

        public void setSoRcvbuf(int soRcvbuf) {
            this.soRcvbuf = soRcvbuf;
        }

        public boolean isTcpNoDelay() {
            return tcpNoDelay;
        }

        public void setTcpNoDelay(boolean tcpNoDelay) {
            this.tcpNoDelay = tcpNoDelay;
        }

        public boolean isSoKeepalive() {
            return soKeepalive;
        }

        public void setSoKeepalive(boolean soKeepalive) {
            this.soKeepalive = soKeepalive;
        }

        public int getWriteBufferWaterMarkLow() {
            return writeBufferWaterMarkLow;
        }

        public void setWriteBufferWaterMarkLow(int writeBufferWaterMarkLow) {
            this.writeBufferWaterMarkLow = writeBufferWaterMarkLow;
        }

        public int getWriteBufferWaterMarkHigh() {
            return writeBufferWaterMarkHigh;
        }

        public void setWriteBufferWaterMarkHigh(int writeBufferWaterMarkHigh) {
            this.writeBufferWaterMarkHigh = writeBufferWaterMarkHigh;
        }
    }

    public static class RedisConfProperties {
        //分片函数
        private String shardingFunc;
        private int redisClusterMaxAttempts = Constants.Transpond.redisClusterMaxAttempts;
        private int heartbeatIntervalSeconds = Constants.Transpond.heartbeatIntervalSeconds;
        private long heartbeatTimeoutMillis = Constants.Transpond.heartbeatTimeoutMillis;
        private int connectTimeoutMillis = Constants.Transpond.connectTimeoutMillis;
        private int failCountThreshold = Constants.Transpond.failCountThreshold;
        private long failBanMillis = Constants.Transpond.failBanMillis;
        private int defaultTranspondWorkThread = Constants.Transpond.defaultTranspondWorkThread;
        private MultiWriteMode multiWriteMode = Constants.Transpond.multiWriteMode;
        private boolean preheat = Constants.Transpond.preheat;
        public boolean closeIdleConnection = Constants.Transpond.closeIdleConnection;//是否关闭空闲连接（到后端redis的）
        public long checkIdleConnectionThresholdSeconds = Constants.Transpond.checkIdleConnectionThresholdSeconds;//判断一个连接空闲的阈值，单位秒
        public int closeIdleConnectionDelaySeconds = Constants.Transpond.closeIdleConnectionDelaySeconds;//判断一个连接空闲后，再过多少秒去执行关闭操作
        private String proxyDiscoveryFactoryClassName;

        public String getShardingFunc() {
            return shardingFunc;
        }

        public void setShardingFunc(String shardingFunc) {
            this.shardingFunc = shardingFunc;
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

        public boolean isPreheat() {
            return preheat;
        }

        public void setPreheat(boolean preheat) {
            this.preheat = preheat;
        }

        public boolean isCloseIdleConnection() {
            return closeIdleConnection;
        }

        public void setCloseIdleConnection(boolean closeIdleConnection) {
            this.closeIdleConnection = closeIdleConnection;
        }

        public long getCheckIdleConnectionThresholdSeconds() {
            return checkIdleConnectionThresholdSeconds;
        }

        public void setCheckIdleConnectionThresholdSeconds(long checkIdleConnectionThresholdSeconds) {
            this.checkIdleConnectionThresholdSeconds = checkIdleConnectionThresholdSeconds;
        }

        public int getCloseIdleConnectionDelaySeconds() {
            return closeIdleConnectionDelaySeconds;
        }

        public void setCloseIdleConnectionDelaySeconds(int closeIdleConnectionDelaySeconds) {
            this.closeIdleConnectionDelaySeconds = closeIdleConnectionDelaySeconds;
        }

        public String getProxyDiscoveryFactoryClassName() {
            return proxyDiscoveryFactoryClassName;
        }

        public void setProxyDiscoveryFactoryClassName(String proxyDiscoveryFactoryClassName) {
            this.proxyDiscoveryFactoryClassName = proxyDiscoveryFactoryClassName;
        }
    }
}
