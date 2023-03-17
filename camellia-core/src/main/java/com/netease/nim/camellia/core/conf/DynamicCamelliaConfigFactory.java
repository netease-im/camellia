package com.netease.nim.camellia.core.conf;

import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2023/3/17
 */
public class DynamicCamelliaConfigFactory {

    private final ConcurrentHashMap<String, DynamicCamelliaConfig> map = new ConcurrentHashMap<>();

    private final String url;
    private final int intervalSeconds;

    public DynamicCamelliaConfigFactory(String url, int intervalSeconds) {
        this.url = url;
        this.intervalSeconds = intervalSeconds;
    }

    public DynamicCamelliaConfig get(String namespace) {
        namespace = namespace.toLowerCase(Locale.ROOT);
        return CamelliaMapUtils.computeIfAbsent(map, namespace, str -> new DynamicCamelliaConfig(url, str, intervalSeconds));
    }

}
