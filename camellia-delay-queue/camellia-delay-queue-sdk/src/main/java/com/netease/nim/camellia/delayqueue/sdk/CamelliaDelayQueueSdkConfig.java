package com.netease.nim.camellia.delayqueue.sdk;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.delayqueue.common.conf.CamelliaDelayQueueConstants;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by caojiajun on 2022/7/7
 */
public class CamelliaDelayQueueSdkConfig {

    //discovery模式下，兜底的reload线程池
    private static final ScheduledExecutorService defaultScheduleThreadPool = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(),
            new CamelliaThreadFactory("camellia-delay-queue-sdk-schedule", true));

    private String url;
    private CamelliaDiscovery discovery;
    private ScheduledExecutorService scheduleThreadPool = defaultScheduleThreadPool;
    private CamelliaDelayMsgListenerConfig listenerConfig = new CamelliaDelayMsgListenerConfig();
    private CamelliaDelayMsgHttpConfig httpConfig = new CamelliaDelayMsgHttpConfig();

    private int discoveryReloadIntervalSeconds = CamelliaDelayQueueConstants.discoveryReloadIntervalSeconds;

    public static class CamelliaDelayMsgHttpConfig {
        private long connectTimeoutMillis = CamelliaDelayQueueConstants.connectTimeoutMillis;
        private long readTimeoutMillis = CamelliaDelayQueueConstants.readTimeoutMillis;
        private long writeTimeoutMillis = CamelliaDelayQueueConstants.writeTimeoutMillis;
        private int maxRequests = CamelliaDelayQueueConstants.maxRequests;
        private int maxRequestsPerHost = CamelliaDelayQueueConstants.maxRequestsPerHost;
        private int maxIdleConnections = CamelliaDelayQueueConstants.maxIdleConnections;
        private int keepAliveSeconds = CamelliaDelayQueueConstants.keepAliveSeconds;

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public CamelliaDiscovery getDiscovery() {
        return discovery;
    }

    public void setDiscovery(CamelliaDiscovery discovery) {
        this.discovery = discovery;
    }

    public ScheduledExecutorService getScheduleThreadPool() {
        return scheduleThreadPool;
    }

    public void setScheduleThreadPool(ScheduledExecutorService scheduleThreadPool) {
        this.scheduleThreadPool = scheduleThreadPool;
    }

    public CamelliaDelayMsgListenerConfig getListenerConfig() {
        return listenerConfig;
    }

    public void setListenerConfig(CamelliaDelayMsgListenerConfig listenerConfig) {
        this.listenerConfig = listenerConfig;
    }

    public CamelliaDelayMsgHttpConfig getHttpConfig() {
        return httpConfig;
    }

    public void setHttpConfig(CamelliaDelayMsgHttpConfig httpConfig) {
        this.httpConfig = httpConfig;
    }

    public int getDiscoveryReloadIntervalSeconds() {
        return discoveryReloadIntervalSeconds;
    }

    public void setDiscoveryReloadIntervalSeconds(int discoveryReloadIntervalSeconds) {
        this.discoveryReloadIntervalSeconds = discoveryReloadIntervalSeconds;
    }
}
