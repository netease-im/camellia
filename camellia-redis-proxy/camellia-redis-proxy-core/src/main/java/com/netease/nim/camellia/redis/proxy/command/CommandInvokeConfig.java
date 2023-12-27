package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.auth.AuthCommandProcessor;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterModeProcessor;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPluginFactory;
import com.netease.nim.camellia.redis.proxy.sentinel.ProxySentinelModeProcessor;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class CommandInvokeConfig {

    private final AuthCommandProcessor authCommandProcessor;
    private final ProxyClusterModeProcessor clusterModeProcessor;
    private final ProxyPluginFactory proxyPluginFactory;
    private final ProxyCommandProcessor proxyCommandProcessor;
    private final ProxySentinelModeProcessor sentinelModeProcessor;

    public CommandInvokeConfig(AuthCommandProcessor authCommandProcessor,
                               ProxyClusterModeProcessor clusterModeProcessor, ProxySentinelModeProcessor sentinelModeProcessor,
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

    public ProxyClusterModeProcessor getClusterModeProcessor() {
        return clusterModeProcessor;
    }

    public ProxyPluginFactory getProxyPluginFactory() {
        return proxyPluginFactory;
    }

    public ProxyCommandProcessor getProxyCommandProcessor() {
        return proxyCommandProcessor;
    }

    public ProxySentinelModeProcessor getSentinelModeProcessor() {
        return sentinelModeProcessor;
    }
}
