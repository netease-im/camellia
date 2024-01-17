package com.netease.nim.camellia.redis.proxy.http;


import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.monitor.CommandFailMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPlugin;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPluginInitResp;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPluginResponse;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/1/17
 */
public class CompletableFutureWithPlugins extends CompletableFuture<Reply> {

    private final ProxyPluginInitResp pluginInitResp;
    private final Command command;
    private final boolean fromPlugin;

    public CompletableFutureWithPlugins(ProxyPluginInitResp pluginInitResp, Command command, boolean fromPlugin) {
        this.pluginInitResp = pluginInitResp;
        this.command = command;
        this.fromPlugin = fromPlugin;
    }

    @Override
    public boolean complete(Reply value) {
        List<ProxyPlugin> replyPlugins = pluginInitResp.getReplyPlugins();
        if (!replyPlugins.isEmpty()) {
            ProxyReply reply = new ProxyReply(command, value, fromPlugin);
            for (ProxyPlugin replyPlugin : replyPlugins) {
                try {
                    ProxyPluginResponse response = replyPlugin.executeReply(reply);
                    if (!response.isPass()) {
                        Reply pluginReply = response.getReply();
                        if (ProxyMonitorCollector.isMonitorEnable()) {
                            if (pluginReply instanceof ErrorReply) {
                                CommandFailMonitor.incr(((ErrorReply) pluginReply).getError());
                            }
                        }
                        return super.complete(pluginReply);
                    }
                } catch (Exception e) {
                    ErrorLogCollector.collect(CompletableFutureWithPlugins.class, "executeReply error", e);
                }
            }
        }
        if (ProxyMonitorCollector.isMonitorEnable()) {
            if (value instanceof ErrorReply) {
                CommandFailMonitor.incr(((ErrorReply) value).getError());
            }
        }
        return super.complete(value);
    }
}
