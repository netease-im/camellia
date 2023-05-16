package com.netease.nim.camellia.hot.key.server.callback;

import com.netease.nim.camellia.hot.key.common.netty.pack.HotKeyCacheStats;

import java.util.List;

/**
 * Created by caojiajun on 2023/5/16
 */
public interface HotKeyCacheStatsCallback {

    /**
     * 缓存命中的统计数据
     * @param statsList statsList
     */
    void newStats(List<HotKeyCacheStats> statsList);
}
