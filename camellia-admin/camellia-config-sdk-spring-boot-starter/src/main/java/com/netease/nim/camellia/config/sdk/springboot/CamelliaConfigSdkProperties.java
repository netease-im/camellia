package com.netease.nim.camellia.config.sdk.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by caojiajun on 2023/3/28
 */
@ConfigurationProperties(prefix = "camellia-config-sdk")
public class CamelliaConfigSdkProperties {
    private String url;
    private int intervalSeconds = 5;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }
}
