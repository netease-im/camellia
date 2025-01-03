package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2025/1/3
 */
public class EmbeddedClient implements IUpstreamClient {

    @Override
    public void sendCommand(int db, List<Command> commands, List<CompletableFuture<Reply>> futureList) {
        if (db > 0) {
            for (CompletableFuture<Reply> future : futureList) {
                future.complete(ErrorReply.DB_INDEX_OUT_OF_RANGE);
            }
            return;
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void preheat() {
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void shutdown() {
    }

    @Override
    public Resource getResource() {
        return null;
    }

    @Override
    public void renew() {

    }
}
