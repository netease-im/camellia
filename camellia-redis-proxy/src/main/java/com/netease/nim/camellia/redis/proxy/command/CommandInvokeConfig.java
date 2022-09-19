package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.auth.AuthCommandProcessor;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPluginFactory;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class CommandInvokeConfig {

    private final AuthCommandProcessor authCommandProcessor;
    private final ProxyPluginFactory proxyPluginFactory;

    public CommandInvokeConfig(AuthCommandProcessor authCommandProcessor, ProxyPluginFactory proxyPluginFactory) {
        this.authCommandProcessor = authCommandProcessor;
        this.proxyPluginFactory = proxyPluginFactory;
    }

    public AuthCommandProcessor getAuthCommandProcessor() {
        return authCommandProcessor;
    }

    public ProxyPluginFactory getProxyPluginFactory() {
        return proxyPluginFactory;
    }
}
