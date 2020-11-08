package com.netease.nim.camellia.redis.proxy.command.async.hotkey;

import java.util.List;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public interface HotKeyMonitorCallback {

    void callback(List<HotKeyInfo> hotKeys, HotKeyConfig hotKeyConfig);
}
