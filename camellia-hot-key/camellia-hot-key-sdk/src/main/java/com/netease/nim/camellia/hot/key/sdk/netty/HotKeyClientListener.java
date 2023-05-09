package com.netease.nim.camellia.hot.key.sdk.netty;

import com.netease.nim.camellia.hot.key.common.model.HotKey;
import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;

/**
 * Created by caojiajun on 2023/5/9
 */
public interface HotKeyClientListener {

    void onHotKey(HotKey hotKey);

    void onHotKeyConfig(HotKeyConfig hotKeyConfig);
}
