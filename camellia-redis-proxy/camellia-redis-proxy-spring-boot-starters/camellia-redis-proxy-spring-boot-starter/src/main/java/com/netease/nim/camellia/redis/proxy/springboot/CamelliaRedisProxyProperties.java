package com.netease.nim.camellia.redis.proxy.springboot;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by caojiajun on 2019/11/13.
 */
@Configuration
@ConfigurationProperties(prefix = "camellia-redis-proxy")
public class CamelliaRedisProxyProperties {

    private String proxyDynamicConfLoaderClassName;
    private Map<String, String> config = new HashMap<>();

    public String getProxyDynamicConfLoaderClassName() {
        return proxyDynamicConfLoaderClassName;
    }

    public void setProxyDynamicConfLoaderClassName(String proxyDynamicConfLoaderClassName) {
        this.proxyDynamicConfLoaderClassName = proxyDynamicConfLoaderClassName;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }
}
