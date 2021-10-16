package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.command.AuthCommandProcessor;
import com.netease.nim.camellia.redis.proxy.command.async.bigkey.BigKeyHunter;
import com.netease.nim.camellia.redis.proxy.command.async.converter.Converters;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.HotKeyHunterManager;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.HotKeyCacheManager;
import com.netease.nim.camellia.redis.proxy.command.async.interceptor.CommandInterceptor;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.CommandSpendTimeConfig;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class CommandInvokeConfig {

    private final CommandSpendTimeConfig commandSpendTimeConfig;
    private final CommandInterceptor commandInterceptor;
    private final HotKeyCacheManager hotKeyCacheManager;
    private final HotKeyHunterManager hotKeyHunterManager;
    private final BigKeyHunter bigKeyHunter;
    private final Converters converters;
    private final AuthCommandProcessor authCommandProcessor;

    public CommandInvokeConfig(AuthCommandProcessor authCommandProcessor, CommandInterceptor commandInterceptor,
                               CommandSpendTimeConfig commandSpendTimeConfig, HotKeyCacheManager hotKeyCacheManager,
                               HotKeyHunterManager hotKeyHunterManager, BigKeyHunter bigKeyHunter, Converters converters) {
        this.authCommandProcessor = authCommandProcessor;
        this.commandInterceptor = commandInterceptor;
        this.commandSpendTimeConfig = commandSpendTimeConfig;
        this.hotKeyCacheManager = hotKeyCacheManager;
        this.hotKeyHunterManager = hotKeyHunterManager;
        this.bigKeyHunter = bigKeyHunter;
        this.converters = converters;
    }

    public AuthCommandProcessor getAuthCommandProcessor() {
        return authCommandProcessor;
    }

    public CommandInterceptor getCommandInterceptor() {
        return commandInterceptor;
    }

    public CommandSpendTimeConfig getCommandSpendTimeConfig() {
        return commandSpendTimeConfig;
    }

    public HotKeyCacheManager getHotKeyCacheManager() {
        return hotKeyCacheManager;
    }

    public HotKeyHunterManager getHotKeyHunterManager() {
        return hotKeyHunterManager;
    }

    public BigKeyHunter getBigKeyHunter() {
        return bigKeyHunter;
    }

    public Converters getConverters() {
        return converters;
    }
}
