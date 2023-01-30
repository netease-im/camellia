package com.netease.nim.camellia.redis.proxy.upstream.connection;

import io.netty.channel.EventLoopGroup;

/**
 * Created by caojiajun on 2021/5/14
 */
public class RedisConnectionConfig {

    private String host;
    private int port;
    private String userName;
    private String password;
    private boolean readonly;
    private int db;
    private EventLoopGroup eventLoopGroup;
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

    public EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    public void setEventLoopGroup(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
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
}
