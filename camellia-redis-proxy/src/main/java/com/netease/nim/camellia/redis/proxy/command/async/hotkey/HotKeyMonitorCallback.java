package com.netease.nim.camellia.redis.proxy.command.async.hotkey;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;

import java.util.List;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public interface HotKeyMonitorCallback {

    /**
     * hot key list will callback
     * @param commandContext commandContext, such as bid/bgroup
     * @param hotKeys hot key list
     * @param hotKeyConfig hot key config
     */
    void callback(CommandContext commandContext, List<HotKeyInfo> hotKeys, HotKeyConfig hotKeyConfig);
}
