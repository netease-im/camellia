package com.netease.nim.camellia.mq.isolation.controller.conf;

/**
 * Created by caojiajun on 2024/2/20
 */
public class MqIsolationControllerConfig {

    private String camelliaConfigUrl;
    private String camelliaConfigNamespace;
    private int camelliaConfigReloadIntervalSeconds = 5;


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
}
