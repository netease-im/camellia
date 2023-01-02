package com.netease.nim.camellia.redis.proxy.plugin;

import java.util.List;

/**
 * Created by caojiajun on 2022/9/19
 */
public class ProxyPluginInitResp {

    private final List<ProxyPlugin> requestPlugins;
    private final List<ProxyPlugin> replyPlugins;

    public ProxyPluginInitResp(List<ProxyPlugin> requestPlugins, List<ProxyPlugin> replyPlugins) {
        this.requestPlugins = requestPlugins;
        this.replyPlugins = replyPlugins;
    }

    public List<ProxyPlugin> getRequestPlugins() {
        return requestPlugins;
    }

    public List<ProxyPlugin> getReplyPlugins() {
        return replyPlugins;
    }
}
