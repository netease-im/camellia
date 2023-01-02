package com.netease.nim.camellia.redis.zk.discovery.springboot;

import com.netease.nim.camellia.redis.proxy.discovery.zk.ZkConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * Created by caojiajun on 2020/8/12
 */
@ConfigurationProperties(prefix = "camellia-redis-zk-discovery")
public class CamelliaRedisZkDiscoveryProperties {

    /**
     * zk的地址，例子：127.0.0.1:2181,127.0.0.2.2181
     */
    private String zkUrl;

    /**
     * zk的一些连接配置
     */
    private int sessionTimeoutMs = ZkConstants.sessionTimeoutMs;
    private int connectionTimeoutMs = ZkConstants.connectionTimeoutMs;
    private int baseSleepTimeMs = ZkConstants.baseSleepTimeMs;
    private int maxRetries = ZkConstants.maxRetries;
    private int reloadIntervalSeconds = ZkConstants.reloadIntervalSeconds;
    private boolean sideCarFirst = ZkConstants.sideCarFirst;
    private String regionResolveConf = "";//例子 10.189.0.0/20:region1,10.189.208.0/21:region2
    private String defaultRegion = "default";
    private boolean jedisPoolLazyInit = true;
    private int jedisPoolInitialSize = 16;

    /**
     * 注册到zk时的根路径
     */
    private String basePath = ZkConstants.basePath;

    public String getZkUrl() {
        return zkUrl;
    }

    public void setZkUrl(String zkUrl) {
        this.zkUrl = zkUrl;
    }

    public int getSessionTimeoutMs() {
        return sessionTimeoutMs;
    }

    public void setSessionTimeoutMs(int sessionTimeoutMs) {
        this.sessionTimeoutMs = sessionTimeoutMs;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public int getBaseSleepTimeMs() {
        return baseSleepTimeMs;
    }

    public void setBaseSleepTimeMs(int baseSleepTimeMs) {
        this.baseSleepTimeMs = baseSleepTimeMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public boolean isSideCarFirst() {
        return sideCarFirst;
    }

    public void setSideCarFirst(boolean sideCarFirst) {
        this.sideCarFirst = sideCarFirst;
    }

    public int getReloadIntervalSeconds() {
        return reloadIntervalSeconds;
    }

    public void setReloadIntervalSeconds(int reloadIntervalSeconds) {
        this.reloadIntervalSeconds = reloadIntervalSeconds;
    }

    public String getRegionResolveConf() {
        return regionResolveConf;
    }

    public void setRegionResolveConf(String regionResolveConf) {
        this.regionResolveConf = regionResolveConf;
    }

    public String getDefaultRegion() {
        return defaultRegion;
    }

    public void setDefaultRegion(String defaultRegion) {
        this.defaultRegion = defaultRegion;
    }

    public boolean isJedisPoolLazyInit() {
        return jedisPoolLazyInit;
    }

    public void setJedisPoolLazyInit(boolean jedisPoolLazyInit) {
        this.jedisPoolLazyInit = jedisPoolLazyInit;
    }

    public int getJedisPoolInitialSize() {
        return jedisPoolInitialSize;
    }

    public void setJedisPoolInitialSize(int jedisPoolInitialSize) {
        this.jedisPoolInitialSize = jedisPoolInitialSize;
    }

}
