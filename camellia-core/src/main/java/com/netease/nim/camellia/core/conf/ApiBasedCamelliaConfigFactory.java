package com.netease.nim.camellia.core.conf;

import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2023/3/17
 */
public class ApiBasedCamelliaConfigFactory {

    private final ConcurrentHashMap<String, ApiBasedCamelliaConfig> map = new ConcurrentHashMap<>();

    private final String url;
    private final int intervalSeconds;

    public ApiBasedCamelliaConfigFactory(String url, int intervalSeconds) {
        this.url = url;
        this.intervalSeconds = intervalSeconds;
    }

    public ApiBasedCamelliaConfig get(String namespace) {
        namespace = namespace.toLowerCase(Locale.ROOT);
        return CamelliaMapUtils.computeIfAbsent(map, namespace, str -> new ApiBasedCamelliaConfig(url, str, intervalSeconds));
    }

}
