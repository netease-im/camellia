package com.netease.nim.camellia.redis.proxy.command.async;

/**
 *
 * Created by caojiajun on 2020/11/10
 */
public class CommandContext {
    private final Long bid;
    private final String bgroup;

    public CommandContext(Long bid, String bgroup) {
        this.bid = bid;
        this.bgroup = bgroup;
    }

    public Long getBid() {
        return bid;
    }

    public String getBgroup() {
        return bgroup;
    }

    @Override
    public String toString() {
        return "CommandContext{" +
                "bid=" + bid +
                ", bgroup='" + bgroup + '\'' +
                '}';
    }
}
