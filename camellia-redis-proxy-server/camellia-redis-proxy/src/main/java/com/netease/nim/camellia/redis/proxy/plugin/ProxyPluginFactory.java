package com.netease.nim.camellia.redis.proxy.plugin;


/**
 * Created by caojiajun on 2022/9/16
 */
public interface ProxyPluginFactory {

    ProxyPluginInitResp initPlugins();

    ProxyPlugin initProxyPlugin(String className);

    void registerPluginUpdate(Runnable callback);
}
