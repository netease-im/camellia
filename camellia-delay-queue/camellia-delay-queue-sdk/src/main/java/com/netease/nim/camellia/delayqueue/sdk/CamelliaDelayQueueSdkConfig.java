package com.netease.nim.camellia.delayqueue.sdk;

import com.netease.nim.camellia.delayqueue.common.conf.CamelliaDelayQueueConstants;
import com.netease.nim.camellia.delayqueue.sdk.api.DelayQueueServerDiscovery;

/**
 * Created by caojiajun on 2022/7/7
 */
public class CamelliaDelayQueueSdkConfig {

    private String url;
    private DelayQueueServerDiscovery discovery;
    private CamelliaDelayMsgListenerConfig listenerConfig = new CamelliaDelayMsgListenerConfig();

    private int discoveryReloadIntervalSeconds = CamelliaDelayQueueConstants.discoveryReloadIntervalSeconds;

    private long connectTimeoutMillis = CamelliaDelayQueueConstants.connectTimeoutMillis;
    private long readTimeoutMillis = CamelliaDelayQueueConstants.readTimeoutMillis;
    private long writeTimeoutMillis = CamelliaDelayQueueConstants.writeTimeoutMillis;
    private int maxRequests = CamelliaDelayQueueConstants.maxRequests;
    private int maxRequestsPerHost = CamelliaDelayQueueConstants.maxRequestsPerHost;
    private int maxIdleConnections = CamelliaDelayQueueConstants.maxIdleConnections;
    private int keepAliveSeconds = CamelliaDelayQueueConstants.keepAliveSeconds;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public DelayQueueServerDiscovery getDiscovery() {
        return discovery;
    }

    public void setDiscovery(DelayQueueServerDiscovery discovery) {
        this.discovery = discovery;
    }

    public CamelliaDelayMsgListenerConfig getListenerConfig() {
        return listenerConfig;
    }

    public void setListenerConfig(CamelliaDelayMsgListenerConfig listenerConfig) {
        this.listenerConfig = listenerConfig;
    }

    public int getDiscoveryReloadIntervalSeconds() {
        return discoveryReloadIntervalSeconds;
    }

    public void setDiscoveryReloadIntervalSeconds(int discoveryReloadIntervalSeconds) {
        this.discoveryReloadIntervalSeconds = discoveryReloadIntervalSeconds;
    }

    public long getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(long connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public long getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(long readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public long getWriteTimeoutMillis() {
        return writeTimeoutMillis;
    }

    public void setWriteTimeoutMillis(long writeTimeoutMillis) {
        this.writeTimeoutMillis = writeTimeoutMillis;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(int maxRequests) {
        this.maxRequests = maxRequests;
    }

    public int getMaxRequestsPerHost() {
        return maxRequestsPerHost;
    }

    public void setMaxRequestsPerHost(int maxRequestsPerHost) {
        this.maxRequestsPerHost = maxRequestsPerHost;
    }

    public int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public void setMaxIdleConnections(int maxIdleConnections) {
        this.maxIdleConnections = maxIdleConnections;
    }

    public int getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public void setKeepAliveSeconds(int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }
}
