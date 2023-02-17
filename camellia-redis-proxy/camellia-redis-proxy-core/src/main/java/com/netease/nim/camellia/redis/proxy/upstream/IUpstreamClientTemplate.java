package com.netease.nim.camellia.redis.proxy.upstream;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2023/1/30
 */
public interface IUpstreamClientTemplate {

    /**
     * 发送命令
     * @param db db
     * @param commands commands
     * @return reply future list
     */
    List<CompletableFuture<Reply>> sendCommand(int db, List<Command> commands);
}
