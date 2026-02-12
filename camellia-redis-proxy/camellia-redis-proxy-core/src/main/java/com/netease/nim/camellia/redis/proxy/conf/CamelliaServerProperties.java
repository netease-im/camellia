package com.netease.nim.camellia.redis.proxy.conf;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/11/11.
 */
public class CamelliaServerProperties {
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
