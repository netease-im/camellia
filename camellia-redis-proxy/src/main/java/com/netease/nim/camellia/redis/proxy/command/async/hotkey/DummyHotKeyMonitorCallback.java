package com.netease.nim.camellia.redis.proxy.command.async.hotkey;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;

import java.util.List;

/**
 *
 * Created by caojiajun on 2021/4/25
 */
public class DummyHotKeyMonitorCallback implements HotKeyMonitorCallback {

    @Override
    public void callback(CommandContext commandContext, List<HotKeyInfo> hotKeys, HotKeyConfig hotKeyConfig) {

    }
}
