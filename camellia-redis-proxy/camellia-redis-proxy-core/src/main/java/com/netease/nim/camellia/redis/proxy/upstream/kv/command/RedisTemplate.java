package com.netease.nim.camellia.redis.proxy.upstream.kv.command;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.UpstreamRedisClientTemplate;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/4/9
 */
public class RedisTemplate {

    private static final byte[] zero = "0".getBytes(StandardCharsets.UTF_8);
    private static final byte[] one = "1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] two = "2".getBytes(StandardCharsets.UTF_8);
    private static final byte[] three = "3".getBytes(StandardCharsets.UTF_8);

    private final UpstreamRedisClientTemplate template;

    public RedisTemplate(UpstreamRedisClientTemplate template) {
        this.template = template;
    }


    public Command luaCommand(byte[] script, byte[][] keys, byte[][] args) {
        int length = keys.length;
        byte[][] cmd = new byte[3 + keys.length + args.length][];
        cmd[0] = RedisCommand.EVAL.raw();
        cmd[1] = script;
        if (length == 0) {
            cmd[2] = zero;
        } else if (length == 1) {
            cmd[2] = one;
        } else if (length == 2) {
            cmd[2] = two;
        } else if (length == 3) {
            cmd[2] = three;
        } else {
            cmd[2] = String.valueOf(length).getBytes(StandardCharsets.UTF_8);
        }
        int index = 3;
        for (byte[] key : keys) {
            cmd[index] = key;
            index ++;
        }
        for (byte[] arg : args) {
            cmd[index] = arg;
            index ++;
        }
        return new Command(cmd);
    }

    public CompletableFuture<Reply> sendLua(byte[] script, byte[][] keys, byte[][] args) {
        return sendCommand(luaCommand(script, keys, args));
    }

    public CompletableFuture<Reply> sendDel(byte[] key) {
        return sendCommand(new Command(new byte[][]{RedisCommand.DEL.raw(), key}));
    }

    public CompletableFuture<Reply> sendPExpire(byte[] key, long pexpire) {
        return sendCommand(new Command(new byte[][]{RedisCommand.PEXPIRE.raw(), key, Utils.stringToBytes(String.valueOf(pexpire))}));
    }

    public CompletableFuture<Reply> sendExists(byte[] key) {
        return sendCommand(new Command(new byte[][]{RedisCommand.EXISTS.raw(), key}));
    }

    public CompletableFuture<Reply> sendGet(byte[] key) {
        return sendCommand(new Command(new byte[][]{RedisCommand.GET.raw(), key}));
    }

    public CompletableFuture<Reply> sendSet(byte[] key, byte[] value) {
        return sendCommand(new Command(new byte[][]{RedisCommand.SET.raw(), key, value}));
    }

    public CompletableFuture<Reply> sendPSetEx(byte[] key, long expireMillis, byte[] value) {
        return sendCommand(new Command(new byte[][]{RedisCommand.PSETEX.raw(), key, Utils.stringToBytes(String.valueOf(expireMillis)), value}));
    }

    public Reply sendCleanExpiredKeyMetaInKv(byte[] namespace, byte[] key, long timeoutMillis) {
        return sync(sendCommand(new Command(new byte[][]{RedisCommand.KV.raw(), RedisKeyword.CLEAN.getRaw(), namespace, key})), timeoutMillis);
    }

    public CompletableFuture<Reply> sendCommand(Command command) {
        List<CompletableFuture<Reply>> futures = template.sendCommand(-1, Collections.singletonList(command));
        return futures.get(0);
    }

    public List<CompletableFuture<Reply>> sendCommand(List<Command> commands) {
        return template.sendCommand(-1, commands);
    }

    public List<Reply> sync(List<CompletableFuture<Reply>> futures, long timeoutMillis) {
        List<Reply> replies = new ArrayList<>();
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
