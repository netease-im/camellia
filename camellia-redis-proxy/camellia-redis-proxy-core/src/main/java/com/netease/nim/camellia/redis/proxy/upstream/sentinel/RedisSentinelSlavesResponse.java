package com.netease.nim.camellia.redis.proxy.upstream.sentinel;

import com.netease.nim.camellia.redis.proxy.upstream.utils.HostAndPort;

import java.util.List;

/**
 * Created by caojiajun on 2021/4/9
 */
public record RedisSentinelSlavesResponse(List<HostAndPort> slaves, boolean sentinelAvailable) {

}
