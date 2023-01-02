package com.netease.nim.camellia.redis.proxy.monitor;

import com.netease.nim.camellia.redis.proxy.monitor.model.Stats;

/**
 *
 * Created by caojiajun on 2020/10/23
 */
public interface MonitorCallback {

    void callback(Stats stats);
}
