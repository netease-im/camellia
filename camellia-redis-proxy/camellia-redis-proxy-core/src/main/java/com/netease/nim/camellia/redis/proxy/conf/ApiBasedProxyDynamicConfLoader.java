package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.core.conf.DynamicCamelliaConfig;

import java.util.Map;

/**
 * 走camellia-config获取配置，远程配置会覆盖本地配置
 * Created by caojiajun on 2023/3/17
 */
public class ApiBasedProxyDynamicConfLoader implements ProxyDynamicConfLoader {

    private final FileBasedProxyDynamicConfLoader loader = new FileBasedProxyDynamicConfLoader();

    private final DynamicCamelliaConfig camelliaConfig;

    public ApiBasedProxyDynamicConfLoader() {
        Map<String, String> map = loader.load();
        String url = map.get("camellia.config.url");
        String namespace = map.get("camellia.config.namespace");
        if (url == null) {
            throw new IllegalArgumentException("missing 'camellia.config.url'");
        }
        if (namespace == null) {
            throw new IllegalArgumentException("missing 'camellia.config.namespace'");
        }
        this.camelliaConfig = new DynamicCamelliaConfig(url, namespace);
    }

    @Override
    public Map<String, String> load() {
        Map<String, String> map = loader.load();
        Map<String, String> conf = camelliaConfig.getConf();
        map.putAll(conf);
        return conf;
    }

    @Override
    public void updateInitConf(Map<String, String> initConf) {
        loader.updateInitConf(initConf);
    }
}
