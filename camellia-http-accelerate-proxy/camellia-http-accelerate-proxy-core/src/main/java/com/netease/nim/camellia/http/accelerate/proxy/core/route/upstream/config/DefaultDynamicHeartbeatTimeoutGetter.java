package com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream.config;

import com.netease.nim.camellia.tools.base.DynamicValueGetter;

/**
 * Created by caojiajun on 2023/7/7
 */
public class DefaultDynamicHeartbeatTimeoutGetter implements DynamicValueGetter<Long> {

    private long timeout;

    public DefaultDynamicHeartbeatTimeoutGetter(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public Long get() {
        return timeout;
    }

    public void updateTimeout(long timeout) {
        this.timeout = timeout;
    }
}
