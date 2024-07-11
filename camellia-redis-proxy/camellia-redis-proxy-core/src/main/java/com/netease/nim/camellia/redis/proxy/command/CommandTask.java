package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.plugin.ProxyPlugin;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPluginResponse;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyReply;
import com.netease.nim.camellia.redis.proxy.plugin.rewrite.RouteRewriteResult;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;

import java.util.List;

/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class CommandTask {

    private final CommandTaskQueue taskQueue;
    private final Command command;
    private final List<ProxyPlugin> plugins;
    private Reply reply;

    public CommandTask(CommandTaskQueue taskQueue, Command command, List<ProxyPlugin> plugins) {
        this.command = command;
        this.taskQueue = taskQueue;
        this.plugins = plugins;
    }

    public List<ProxyPlugin> getPlugins() {
        return plugins;
    }

    public void replyCompleted(Reply reply, boolean fromPlugin) {
        replyCompleted(reply, fromPlugin, false);
    }

    public RouteRewriteResult replyCompleted(Reply reply, boolean fromPlugin, boolean supportRedirect) {
        try {
            if (plugins != null && !plugins.isEmpty()) {
                ProxyReply proxyReply = new ProxyReply(command, reply, fromPlugin, supportRedirect);
                for (ProxyPlugin plugin : plugins) {
                    try {
                        ProxyPluginResponse response = plugin.executeReply(proxyReply);
                        if (supportRedirect && response.getRouteRewriterResult() != null) {
                            return response.getRouteRewriterResult();
                        }
                        if (!response.isPass()) {
                            this.reply = response.getReply();
                            this.taskQueue.callback();
                            return null;
                        }
                    } catch (Exception e) {
                        ErrorLogCollector.collect(CommandTask.class, "executeReply error", e);
                    }
                }
            }
            this.reply = reply;
            this.taskQueue.callback();
            return null;
        } catch (Exception e) {
            ErrorLogCollector.collect(CommandTask.class, e.getMessage(), e);
            this.reply = reply;
            this.taskQueue.callback();
            return null;
        }
    }

    public void replyCompleted(Reply reply) {
        replyCompleted(reply, false, false);
    }

    public Command getCommand() {
        return command;
    }

    public Reply getReply() {
        return reply;
    }
}
