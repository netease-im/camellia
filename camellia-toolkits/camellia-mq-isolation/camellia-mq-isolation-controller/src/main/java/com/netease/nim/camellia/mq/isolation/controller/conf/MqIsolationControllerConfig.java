package com.netease.nim.camellia.mq.isolation.controller.conf;

/**
 * Created by caojiajun on 2024/2/20
 */
public class MqIsolationControllerConfig {

    private String camelliaConfigUrl;
    private String camelliaConfigNamespace;
    private int camelliaConfigReloadIntervalSeconds = 5;
    private int routeLocalCacheSeconds = 10;
    private int routeRedisCacheSeconds = 15;
    private int routeRedisCacheRefreshSeconds = 10;

    public String getCamelliaConfigUrl() {
        return camelliaConfigUrl;
    }

    public void setCamelliaConfigUrl(String camelliaConfigUrl) {
        this.camelliaConfigUrl = camelliaConfigUrl;
    }

    public String getCamelliaConfigNamespace() {
        return camelliaConfigNamespace;
    }

    public void setCamelliaConfigNamespace(String camelliaConfigNamespace) {
        this.camelliaConfigNamespace = camelliaConfigNamespace;
    }

    public int getCamelliaConfigReloadIntervalSeconds() {
        return camelliaConfigReloadIntervalSeconds;
    }

    public void setCamelliaConfigReloadIntervalSeconds(int camelliaConfigReloadIntervalSeconds) {
        this.camelliaConfigReloadIntervalSeconds = camelliaConfigReloadIntervalSeconds;
    }

    public int getRouteLocalCacheSeconds() {
        return routeLocalCacheSeconds;
    }

    public void setRouteLocalCacheSeconds(int routeLocalCacheSeconds) {
        this.routeLocalCacheSeconds = routeLocalCacheSeconds;
    }

    public int getRouteRedisCacheSeconds() {
        return routeRedisCacheSeconds;
    }

    public void setRouteRedisCacheSeconds(int routeRedisCacheSeconds) {
        this.routeRedisCacheSeconds = routeRedisCacheSeconds;
    }

    public int getRouteRedisCacheRefreshSeconds() {
        return routeRedisCacheRefreshSeconds;
    }

    public void setRouteRedisCacheRefreshSeconds(int routeRedisCacheRefreshSeconds) {
        this.routeRedisCacheRefreshSeconds = routeRedisCacheRefreshSeconds;
    }
}
