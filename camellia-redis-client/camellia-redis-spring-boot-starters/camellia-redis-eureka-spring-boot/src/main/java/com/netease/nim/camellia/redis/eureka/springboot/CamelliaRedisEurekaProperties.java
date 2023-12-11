package com.netease.nim.camellia.redis.eureka.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * Created by caojiajun on 2020/4/23.
 */
@ConfigurationProperties(prefix = "camellia-redis-eureka")
public class CamelliaRedisEurekaProperties {

    private int refreshIntervalSeconds = 5;
    private boolean sideCarFirst = false;
    private String regionResolveConf = "";//例子 10.189.0.0/20:region1,10.189.208.0/21:region2
    private String defaultRegion = "default";
    private boolean jedisPoolLazyInit = true;
    private int jedisPoolInitialSize = 16;

    public int getRefreshIntervalSeconds() {
        return refreshIntervalSeconds;
    }

    public void setRefreshIntervalSeconds(int refreshIntervalSeconds) {
        this.refreshIntervalSeconds = refreshIntervalSeconds;
    }

    public boolean isSideCarFirst() {
        return sideCarFirst;
    }

    public void setSideCarFirst(boolean sideCarFirst) {
        this.sideCarFirst = sideCarFirst;
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
