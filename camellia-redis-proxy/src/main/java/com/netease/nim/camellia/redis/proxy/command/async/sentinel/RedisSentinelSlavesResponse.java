package com.netease.nim.camellia.redis.proxy.command.async.sentinel;

import com.netease.nim.camellia.redis.proxy.command.async.HostAndPort;

import java.util.List;

/**
 *
 * Created by caojiajun on 2021/4/9
 */
public class RedisSentinelSlavesResponse {

    private final List<HostAndPort> slaves;
    private final boolean sentinelAvailable;

    public RedisSentinelSlavesResponse(List<HostAndPort> slaves, boolean sentinelAvailable) {
        this.slaves = slaves;
        this.sentinelAvailable = sentinelAvailable;
    }

    public List<HostAndPort> getSlaves() {
        return slaves;
    }

    public boolean isSentinelAvailable() {
        return sentinelAvailable;
    }
}
