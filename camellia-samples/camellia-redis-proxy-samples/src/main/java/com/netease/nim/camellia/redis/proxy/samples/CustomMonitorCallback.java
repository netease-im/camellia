package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.redis.proxy.monitor.LoggingMonitorCallback;
import com.netease.nim.camellia.redis.proxy.monitor.MonitorCallback;
import com.netease.nim.camellia.redis.proxy.monitor.Stats;
import org.springframework.stereotype.Component;

/**
 * Created by caojiajun on 2021/9/29
 */
@Component
public class CustomMonitorCallback implements MonitorCallback {

    private final LoggingMonitorCallback loggingMonitorCallback = new LoggingMonitorCallback();

    @Override
    public void callback(Stats stats) {
        loggingMonitorCallback.callback(stats);
    }
}
