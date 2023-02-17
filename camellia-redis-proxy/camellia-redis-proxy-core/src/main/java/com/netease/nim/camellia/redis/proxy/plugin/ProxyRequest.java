package com.netease.nim.camellia.redis.proxy.plugin;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;


/**
 * Created by caojiajun on 2022/9/14
 */
public class ProxyRequest {
    private final int db;
    private final Command command;
    private final IUpstreamClientTemplateFactory clientTemplateFactory;

    public ProxyRequest(int db, Command command, IUpstreamClientTemplateFactory clientTemplateFactory) {
        this.db = db;
        this.command = command;
        this.clientTemplateFactory = clientTemplateFactory;
    }

    public int getDb() {
        return db;
    }

    public Command getCommand() {
        return command;
    }

    public IUpstreamClientTemplateFactory getClientTemplateFactory() {
        return clientTemplateFactory;
    }
}
