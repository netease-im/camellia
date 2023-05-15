package com.netease.nim.camellia.hot.key.server.conf;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.conf.ApiBasedCamelliaConfig;
import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2023/5/12
 */
public class ApiBasedHotKeyConfigService extends HotKeyConfigService {

    private ApiBasedCamelliaConfig camelliaConfig;
    private final ConcurrentHashMap<String, Boolean> namespaceMap = new ConcurrentHashMap<>();

    private void reload() {
        for (Map.Entry<String, Boolean> entry : namespaceMap.entrySet()) {
            invokeUpdate(entry.getKey());
        }
    }

    @Override
    public HotKeyConfig get(String namespace) {
        String string = camelliaConfig.getString(namespace, null);
        if (string == null) {
            return null;
        }
        namespaceMap.put(namespace, true);
        return JSONObject.parseObject(string, HotKeyConfig.class);
    }

    @Override
    public void init(HotKeyServerProperties properties) {
        Map<String, String> config = properties.getConfig();
        String url = config.get("camellia.config.url");
        String namespace = config.get("camellia.config.namespace");
        if (url == null) {
            throw new IllegalArgumentException("missing 'camellia.config.url'");
        }
        if (namespace == null) {
            throw new IllegalArgumentException("missing 'camellia.config.namespace'");
        }
        this.camelliaConfig = new ApiBasedCamelliaConfig(url, namespace);
        this.camelliaConfig.addCallback(this::reload);
    }
}
