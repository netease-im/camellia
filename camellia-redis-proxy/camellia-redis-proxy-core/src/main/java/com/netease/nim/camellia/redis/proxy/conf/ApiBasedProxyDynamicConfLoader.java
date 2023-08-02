package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.core.conf.ApiBasedCamelliaConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * 走camellia-config获取配置，远程配置会覆盖本地配置
 * Created by caojiajun on 2023/3/17
 */
public class ApiBasedProxyDynamicConfLoader implements ProxyDynamicConfLoader {

    private ApiBasedCamelliaConfig camelliaConfig;
    private Map<String, String> initConf = new HashMap<>();

    @Override
    public Map<String, String> load() {
        Map<String, String> conf = new HashMap<>(initConf);
        conf.putAll(camelliaConfig.getConf());
        return conf;
    }

    private void init() {
        String url = initConf.get("camellia.config.url");
        String namespace = initConf.get("camellia.config.namespace");
        if (url == null) {
            throw new IllegalArgumentException("missing 'camellia.config.url'");
        }
        if (namespace == null) {
            throw new IllegalArgumentException("missing 'camellia.config.namespace'");
        }
        this.camelliaConfig = new ApiBasedCamelliaConfig(url, namespace);
        this.camelliaConfig.addCallback(ProxyDynamicConf::reload);
    }

    @Override
    public void init(Map<String, String> initConf) {
        this.initConf = new HashMap<>(initConf);
        init();
    }

}
