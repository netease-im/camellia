package com.netease.nim.camellia.redis.proxy.upstream;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 * Created by caojiajun on 2019/12/19.
 */
public interface AsyncClient {

    void sendCommand(List<Command> commands, List<CompletableFuture<Reply>> futureList);

    void preheat();
}
