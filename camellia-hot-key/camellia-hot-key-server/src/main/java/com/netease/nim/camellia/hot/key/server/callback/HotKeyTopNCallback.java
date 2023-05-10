package com.netease.nim.camellia.hot.key.server.callback;

import com.netease.nim.camellia.hot.key.server.calculate.TopNStatsResult;

/**
 * Created by caojiajun on 2023/5/10
 */
public interface HotKeyTopNCallback {

    void topN(TopNStatsResult result);
}
