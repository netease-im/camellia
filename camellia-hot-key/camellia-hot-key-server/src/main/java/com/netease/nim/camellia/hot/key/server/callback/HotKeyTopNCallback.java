package com.netease.nim.camellia.hot.key.server.callback;

import com.netease.nim.camellia.hot.key.server.calculate.TopNStatsResult;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;

/**
 * Created by caojiajun on 2023/5/10
 */
public interface HotKeyTopNCallback {

    /***
     * 回调topN的key数据，注意回调结果是全局的topN，而非单机的topN
     * 一个周期内，一个namespace只会回调一次（集群内会任选一台机器，通过redis锁来实现）
     * @param result 结果
     */
    void topN(TopNStatsResult result);

    /**
     * 初始化方法
     * @param properties properties
     */
    default void init(HotKeyServerProperties properties) {

    }
}
