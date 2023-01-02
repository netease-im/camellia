package com.netease.nim.camellia.redis.proxy.plugin;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.upstream.AsyncCamelliaRedisTemplateChooser;


/**
 * Created by caojiajun on 2022/9/14
 */
public class ProxyRequest {
    private final Command command;
    private final AsyncCamelliaRedisTemplateChooser chooser;

    public ProxyRequest(Command command, AsyncCamelliaRedisTemplateChooser chooser) {
        this.command = command;
        this.chooser = chooser;
    }

    public Command getCommand() {
        return command;
    }

    public AsyncCamelliaRedisTemplateChooser getChooser() {
        return chooser;
    }
}
