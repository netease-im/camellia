package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.redis.proxy.command.async.hotkey.CommandHotKeyMonitorConfig;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.CommandSpendTimeConfig;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class CommandInvokeConfig {
    private final CommandSpendTimeConfig commandSpendTimeConfig;
    private final CommandHotKeyMonitorConfig commandHotKeyMonitorConfig;
    private final CommandInterceptor commandInterceptor;
    private final int commandPipelineFlushThreshold;

    public CommandInvokeConfig(int commandPipelineFlushThreshold, CommandInterceptor commandInterceptor,
                               CommandSpendTimeConfig commandSpendTimeConfig, CommandHotKeyMonitorConfig commandHotKeyMonitorConfig) {
        this.commandSpendTimeConfig = commandSpendTimeConfig;
        this.commandHotKeyMonitorConfig = commandHotKeyMonitorConfig;
        this.commandInterceptor = commandInterceptor;
        this.commandPipelineFlushThreshold = commandPipelineFlushThreshold;
    }

    public CommandSpendTimeConfig getCommandSpendTimeConfig() {
        return commandSpendTimeConfig;
    }

    public CommandHotKeyMonitorConfig getCommandHotKeyMonitorConfig() {
        return commandHotKeyMonitorConfig;
    }

    public CommandInterceptor getCommandInterceptor() {
        return commandInterceptor;
    }

    public int getCommandPipelineFlushThreshold() {
        return commandPipelineFlushThreshold;
    }
}
