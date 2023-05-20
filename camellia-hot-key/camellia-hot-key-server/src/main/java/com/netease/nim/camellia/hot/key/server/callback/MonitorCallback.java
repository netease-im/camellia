package com.netease.nim.camellia.hot.key.server.callback;

import com.netease.nim.camellia.hot.key.server.monitor.HotKeyServerStats;

/**
 *
 * Created by caojiajun on 2020/10/23
 */
public interface MonitorCallback {

    /**
     * 统计数据
     * @param serverStats serverStats
     */
    void serverStats(HotKeyServerStats serverStats);

}
