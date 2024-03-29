package com.netease.nim.camellia.mq.isolation.controller.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by caojiajun on 2023/3/28
 */
@ConfigurationProperties(prefix = "camellia-mq-isolation-controller")
public class CamelliaMqIsolationControllerProperties {
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
