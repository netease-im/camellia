package com.netease.nim.camellia.redis.proxy.upstream.connection;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.tls.upstream.ProxyUpstreamTlsProvider;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;
import io.netty.channel.EventLoop;

/**
 * Created by caojiajun on 2021/5/14
 */
public class RedisConnectionConfig {

    private String host;
    private int port;
    private String udsPath;
    private String userName;
    private String password;
    private boolean readonly;
    private int db;
    private EventLoop eventLoop;
    private int heartbeatIntervalSeconds;
    private long heartbeatTimeoutMillis;
    private int connectTimeoutMillis;

    private boolean closeIdleConnection;
    private long checkIdleConnectionThresholdSeconds;
    private int closeIdleConnectionDelaySeconds;

    private boolean skipCommandSpendTimeMonitor;//是否跳过统计后端redis响应时间

    private boolean soKeepalive;
    private int soSndbuf;
    private int soRcvbuf;
    private boolean tcpNoDelay;
    private boolean tcpQuickAck;
    private int writeBufferWaterMarkLow;
    private int writeBufferWaterMarkHigh;

    private FastFailStats fastFailStats;

    private ProxyUpstreamTlsProvider proxyUpstreamTlsProvider;

    private IUpstreamClient upstreamClient;

    private Resource resource;

    private UpstreamAddrConverter upstreamHostConverter;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUdsPath() {
        return udsPath;
    }

    public void setUdsPath(String udsPath) {
        this.udsPath = udsPath;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public int getDb() {
        return db;
    }

    public void setDb(int db) {
        this.db = db;
    }

    public EventLoop getEventLoop() {
        return eventLoop;
    }

    public void setEventLoop(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
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

    public boolean isSkipCommandSpendTimeMonitor() {
        return skipCommandSpendTimeMonitor;
    }

    public void setSkipCommandSpendTimeMonitor(boolean skipCommandSpendTimeMonitor) {
        this.skipCommandSpendTimeMonitor = skipCommandSpendTimeMonitor;
    }

    public boolean isSoKeepalive() {
        return soKeepalive;
    }

    public void setSoKeepalive(boolean soKeepalive) {
        this.soKeepalive = soKeepalive;
    }

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

    public boolean isTcpQuickAck() {
        return tcpQuickAck;
    }

    public void setTcpQuickAck(boolean tcpQuickAck) {
        this.tcpQuickAck = tcpQuickAck;
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

    public FastFailStats getFastFailStats() {
        return fastFailStats;
    }

    public void setFastFailStats(FastFailStats fastFailStats) {
        this.fastFailStats = fastFailStats;
    }

    public ProxyUpstreamTlsProvider getProxyUpstreamTlsProvider() {
        return proxyUpstreamTlsProvider;
    }

    public void setProxyUpstreamTlsProvider(ProxyUpstreamTlsProvider proxyUpstreamTlsProvider) {
        this.proxyUpstreamTlsProvider = proxyUpstreamTlsProvider;
    }

    public IUpstreamClient getUpstreamClient() {
        return upstreamClient;
    }

    public void setUpstreamClient(IUpstreamClient upstreamClient) {
        this.upstreamClient = upstreamClient;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public UpstreamAddrConverter getUpstreamHostConverter() {
        return upstreamHostConverter;
    }

    public void setUpstreamHostConverter(UpstreamAddrConverter upstreamHostConverter) {
        this.upstreamHostConverter = upstreamHostConverter;
    }
}
