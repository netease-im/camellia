package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.core.conf.DynamicCamelliaConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 走camellia-config获取配置，远程配置会覆盖本地配置
 * Created by caojiajun on 2023/3/17
 */
public class ApiBasedProxyDynamicConfLoader implements ProxyDynamicConfLoader {

    private final AtomicBoolean initOk = new AtomicBoolean();
    private DynamicCamelliaConfig camelliaConfig;
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
        this.camelliaConfig = new DynamicCamelliaConfig(url, namespace);
    }

    @Override
    public void updateInitConf(Map<String, String> initConf) {
        this.initConf = initConf;
        if (initOk.compareAndSet(false, true)) {
            init();
        }
    }
}
