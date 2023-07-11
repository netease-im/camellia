package com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream.config;

import com.netease.nim.camellia.tools.base.DynamicValueGetter;

/**
 * Created by caojiajun on 2023/7/7
 */
public class DefaultDynamicUpstreamHealthUriGetter implements DynamicValueGetter<String> {

    private String uri;

    public DefaultDynamicUpstreamHealthUriGetter(String uri) {
        this.uri = uri;
    }

    @Override
    public String get() {
        return uri;
    }

    public void updateHealthUri(String uri) {
        this.uri = uri;
    }
}
