package com.netease.nim.camellia.tools.http;


/**
 * Created by caojiajun on 2025/8/13
 */
public class HttpClientConfig {

    private long connectTimeoutMillis = 5000L;
    private long readTimeoutMillis = 5000L;
    private long writeTimeoutMillis = 5000L;
    private int maxRequests = 4096;
    private int maxRequestsPerHost = 256;
    private int keepAliveSeconds = 3;
    private int maxIdleConnections = 256;
    private boolean skipHostNameVerifier = false;

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

    public int getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public void setKeepAliveSeconds(int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    public int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public void setMaxIdleConnections(int maxIdleConnections) {
        this.maxIdleConnections = maxIdleConnections;
    }

    public boolean isSkipHostNameVerifier() {
        return skipHostNameVerifier;
    }

    public void setSkipHostNameVerifier(boolean skipHostNameVerifier) {
        this.skipHostNameVerifier = skipHostNameVerifier;
    }
}
