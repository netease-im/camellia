package com.netease.nim.camellia.config.service;

import com.netease.nim.camellia.config.model.ConfigHistory;

/**
 * Created by caojiajun on 2023/3/23
 */
public interface ConfigChangeNotify {

    void notify(ConfigHistory configHistory);
}
