package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.auth.AuthCommandProcessor;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterModeProcessor;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPluginFactory;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class CommandInvokeConfig {

    private final AuthCommandProcessor authCommandProcessor;
    private final ProxyClusterModeProcessor clusterModeProcessor;
    private final ProxyPluginFactory proxyPluginFactory;

    public CommandInvokeConfig(AuthCommandProcessor authCommandProcessor, ProxyClusterModeProcessor clusterModeProcessor, ProxyPluginFactory proxyPluginFactory) {
        this.authCommandProcessor = authCommandProcessor;
        this.clusterModeProcessor = clusterModeProcessor;
        this.proxyPluginFactory = proxyPluginFactory;
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
}
