package com.netease.nim.camellia.redis.proxy.upstream.kv.command;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.UpstreamRedisClientTemplate;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/4/9
 */
public class RedisTemplate {

    private final UpstreamRedisClientTemplate template;

    public RedisTemplate(UpstreamRedisClientTemplate template) {
        this.template = template;
    }

    public CompletableFuture<Reply> sendDel(byte[] key) {
        return sendCommand(new Command(new byte[][]{RedisCommand.DEL.raw(), key}));
    }

    public CompletableFuture<Reply> sendPExpire(byte[] key, long pexpire) {
        return sendCommand(new Command(new byte[][]{RedisCommand.PEXPIRE.raw(), key, Utils.stringToBytes(String.valueOf(pexpire))}));
    }

    public Reply sendCleanExpiredKeyMetaInKv(byte[] namespace, byte[] key, long timeoutMillis) {
        return sync(sendCommand(new Command(new byte[][]{RedisCommand.KV.raw(), RedisKeyword.CLEAN.getRaw(), namespace, key})), timeoutMillis);
    }

    public CompletableFuture<Reply> sendCommand(Command command) {
        List<CompletableFuture<Reply>> futures = template.sendCommand(-1, Collections.singletonList(command));
        return futures.get(0);
    }

    public List<CompletableFuture<Reply>> sendCommand(List<Command> commands) {
        if (commands.isEmpty()) {
            return Collections.emptyList();
        }
        return template.sendCommand(-1, commands);
    }

    public List<Reply> sync(List<CompletableFuture<Reply>> futures, long timeoutMillis) {
        if (futures.isEmpty()) {
            return Collections.emptyList();
        }
        List<Reply> replies = new ArrayList<>(futures.size());
        for (CompletableFuture<Reply> future : futures) {
            Reply reply = sync(future, timeoutMillis);
            replies.add(reply);
        }
        return replies;
    }

    public Reply sync(CompletableFuture<Reply> future, long timeoutMillis) {
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return ErrorReply.TIMEOUT;
        }
    }
}
