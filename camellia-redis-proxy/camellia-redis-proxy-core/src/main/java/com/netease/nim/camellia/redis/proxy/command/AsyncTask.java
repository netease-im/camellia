package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.plugin.ProxyPlugin;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPluginResponse;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;

import java.util.List;

/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class AsyncTask {

    private final AsyncTaskQueue taskQueue;
    private final Command command;
    private final List<ProxyPlugin> plugins;
    private Reply reply;

    public AsyncTask(AsyncTaskQueue taskQueue, Command command, List<ProxyPlugin> plugins) {
        this.command = command;
        this.taskQueue = taskQueue;
        this.plugins = plugins;
    }

    public List<ProxyPlugin> getPlugins() {
        return plugins;
    }

    public void replyCompleted(Reply reply, boolean fromPlugin) {
        try {
            if (plugins != null && !plugins.isEmpty()) {
                ProxyReply proxyReply = new ProxyReply(command, reply, fromPlugin);
                for (ProxyPlugin plugin : plugins) {
                    try {
                        ProxyPluginResponse response = plugin.executeReply(proxyReply);
                        if (!response.isPass()) {
                            this.reply = response.getReply();
                            this.taskQueue.callback();
                            return;
                        }
                    } catch (Exception e) {
                        ErrorLogCollector.collect(AsyncTask.class, "executeReply error", e);
                    }
                }
            }
            this.reply = reply;
            this.taskQueue.callback();
        } catch (Exception e) {
            ErrorLogCollector.collect(AsyncTask.class, e.getMessage(), e);
            this.reply = reply;
            this.taskQueue.callback();
        }
    }

    public void replyCompleted(Reply reply) {
        replyCompleted(reply, false);
    }

    public Command getCommand() {
        return command;
    }

    public Reply getReply() {
        return reply;
    }
}
