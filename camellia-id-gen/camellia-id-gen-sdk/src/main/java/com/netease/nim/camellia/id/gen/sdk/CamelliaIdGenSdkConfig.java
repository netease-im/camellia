package com.netease.nim.camellia.id.gen.sdk;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2021/9/27
 */
public class CamelliaIdGenSdkConfig {

    public static final ThreadPoolExecutor defaultAsyncLoadThreadPool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2,
            Runtime.getRuntime().availableProcessors() * 2, 0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10240), new CamelliaThreadFactory("camellia-id-gen-sdk", true));

    private String url;
    private IdGenServerDiscovery discovery;
    private int discoveryReloadIntervalSeconds = 60;
    private int maxRetry = 2;
    private long connectTimeoutMillis = 5000;
    private long readTimeoutMillis = 5000;
    private long writeTimeoutMillis = 5000;
    private int maxRequests = 1024;
    private int maxRequestsPerHost = 4096;
    private int maxIdleConnections = 1024;

    private SegmentIdGenSdkConfig segmentIdGenSdkConfig = new SegmentIdGenSdkConfig();

    public static class SegmentIdGenSdkConfig {

        private boolean cacheEnable = false;

        private int step = 100;
        private int tagCount = 1000;
        private int maxRetry = 10;
        private long retryIntervalMillis = 10;
        private ExecutorService asyncLoadThreadPool = defaultAsyncLoadThreadPool;

        public boolean isCacheEnable() {
            return cacheEnable;
        }

        public void setCacheEnable(boolean cacheEnable) {
            this.cacheEnable = cacheEnable;
        }

        public int getStep() {
            return step;
        }

        public void setStep(int step) {
            this.step = step;
        }

        public int getTagCount() {
            return tagCount;
        }

        public void setTagCount(int tagCount) {
            this.tagCount = tagCount;
        }

        public ExecutorService getAsyncLoadThreadPool() {
            return asyncLoadThreadPool;
        }

        public void setAsyncLoadThreadPool(ExecutorService asyncLoadThreadPool) {
            this.asyncLoadThreadPool = asyncLoadThreadPool;
        }

        public int getMaxRetry() {
            return maxRetry;
        }

        public void setMaxRetry(int maxRetry) {
            this.maxRetry = maxRetry;
        }

        public long getRetryIntervalMillis() {
            return retryIntervalMillis;
        }

        public void setRetryIntervalMillis(long retryIntervalMillis) {
            this.retryIntervalMillis = retryIntervalMillis;
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public IdGenServerDiscovery getDiscovery() {
        return discovery;
    }

    public void setDiscovery(IdGenServerDiscovery discovery) {
        this.discovery = discovery;
    }

    public int getDiscoveryReloadIntervalSeconds() {
        return discoveryReloadIntervalSeconds;
    }

    public void setDiscoveryReloadIntervalSeconds(int discoveryReloadIntervalSeconds) {
        this.discoveryReloadIntervalSeconds = discoveryReloadIntervalSeconds;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
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

    public SegmentIdGenSdkConfig getSegmentIdGenSdkConfig() {
        return segmentIdGenSdkConfig;
    }

    public void setSegmentIdGenSdkConfig(SegmentIdGenSdkConfig segmentIdGenSdkConfig) {
        this.segmentIdGenSdkConfig = segmentIdGenSdkConfig;
    }
}
