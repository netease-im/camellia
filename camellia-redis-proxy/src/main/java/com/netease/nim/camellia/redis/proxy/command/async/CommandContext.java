package com.netease.nim.camellia.redis.proxy.command.async;

import java.net.SocketAddress;

/**
 * 注意各字段的判空
 * Created by caojiajun on 2020/11/10
 */
public class CommandContext {
    private final Long bid;
    private final String bgroup;
    private final SocketAddress clientSocketAddress;

    public CommandContext(Long bid, String bgroup, SocketAddress clientSocketAddress) {
        this.bid = bid;
        this.bgroup = bgroup;
        this.clientSocketAddress = clientSocketAddress;
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
                ", clientSocketAddress=" + clientSocketAddress +
                '}';
    }
}
