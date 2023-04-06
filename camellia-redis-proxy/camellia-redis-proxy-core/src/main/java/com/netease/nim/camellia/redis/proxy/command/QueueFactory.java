package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.reply.Reply;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2023/4/6
 */
public interface QueueFactory {

    Queue<CommandTask> generateCommandTaskQueue();

    Queue<CompletableFuture<Reply>> generateCommandReplyQueue();
}
