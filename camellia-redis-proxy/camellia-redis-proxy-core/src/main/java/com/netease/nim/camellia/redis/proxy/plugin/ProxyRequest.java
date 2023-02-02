package com.netease.nim.camellia.redis.proxy.plugin;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;


/**
 * Created by caojiajun on 2022/9/14
 */
public class ProxyRequest {
    private final Command command;
    private final IUpstreamClientTemplateFactory clientTemplateFactory;

    public ProxyRequest(Command command, IUpstreamClientTemplateFactory clientTemplateFactory) {
        this.command = command;
        this.clientTemplateFactory = clientTemplateFactory;
    }

    public Command getCommand() {
        return command;
    }

    public IUpstreamClientTemplateFactory getClientTemplateFactory() {
        return clientTemplateFactory;
    }
}
