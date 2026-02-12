package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.auth.AuthCommandProcessor;
import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeProcessor;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPluginFactory;
import com.netease.nim.camellia.redis.proxy.sentinel.SentinelModeProcessor;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class CommandInvokeConfig {

    private final AuthCommandProcessor authCommandProcessor;
    private final ClusterModeProcessor clusterModeProcessor;
    private final ProxyPluginFactory proxyPluginFactory;
    private final ProxyCommandProcessor proxyCommandProcessor;
    private final SentinelModeProcessor sentinelModeProcessor;

    public CommandInvokeConfig(AuthCommandProcessor authCommandProcessor,
                               ClusterModeProcessor clusterModeProcessor, SentinelModeProcessor sentinelModeProcessor,
                               ProxyPluginFactory proxyPluginFactory, ProxyCommandProcessor proxyCommandProcessor) {
        this.authCommandProcessor = authCommandProcessor;
        this.clusterModeProcessor = clusterModeProcessor;
        this.sentinelModeProcessor = sentinelModeProcessor;
        this.proxyPluginFactory = proxyPluginFactory;
        this.proxyCommandProcessor = proxyCommandProcessor;
    }

    public AuthCommandProcessor getAuthCommandProcessor() {
        return authCommandProcessor;
    }

    public ClusterModeProcessor getClusterModeProcessor() {
        return clusterModeProcessor;
    }

    public ProxyPluginFactory getProxyPluginFactory() {
        return proxyPluginFactory;
    }

    public ProxyCommandProcessor getProxyCommandProcessor() {
        return proxyCommandProcessor;
    }

    public SentinelModeProcessor getSentinelModeProcessor() {
        return sentinelModeProcessor;
    }
}
