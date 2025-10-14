package com.netease.nim.camellia.redis.proxy.upstream.sentinel;

import com.netease.nim.camellia.redis.proxy.upstream.utils.HostAndPort;

/**
 * Created by caojiajun on 2021/4/9
 */
public record RedisSentinelMasterResponse(HostAndPort master, boolean sentinelAvailable) {
}
