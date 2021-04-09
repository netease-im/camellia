package com.netease.nim.camellia.redis.proxy.command.async.sentinel;

import com.netease.nim.camellia.redis.proxy.command.async.HostAndPort;

/**
 *
 * Created by caojiajun on 2021/4/9
 */
public class RedisSentinelMasterResponse {
    private final HostAndPort master;
    private final boolean sentinelAvailable;

    public RedisSentinelMasterResponse(HostAndPort master, boolean sentinelAvailable) {
        this.master = master;
        this.sentinelAvailable = sentinelAvailable;
    }

    public HostAndPort getMaster() {
        return master;
    }

    public boolean isSentinelAvailable() {
        return sentinelAvailable;
    }
}
