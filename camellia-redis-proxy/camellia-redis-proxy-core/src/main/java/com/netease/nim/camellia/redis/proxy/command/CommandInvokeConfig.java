package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.auth.AuthCommandProcessor;
import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeProcessor;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPluginFactory;
import com.netease.nim.camellia.redis.proxy.sentinel.SentinelModeProcessor;

/**
 * Created by caojiajun on 2020/10/22
 */
public record CommandInvokeConfig(AuthCommandProcessor authCommandProcessor, ClusterModeProcessor clusterModeProcessor,
                                  SentinelModeProcessor sentinelModeProcessor, ProxyPluginFactory proxyPluginFactory,
                                  ProxyCommandProcessor proxyCommandProcessor) {

}
