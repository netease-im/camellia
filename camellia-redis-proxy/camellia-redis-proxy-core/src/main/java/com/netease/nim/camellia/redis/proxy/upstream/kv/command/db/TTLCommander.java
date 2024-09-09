package com.netease.nim.camellia.redis.proxy.upstream.kv.command.db;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ValueWrapper;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;

/**
 * TTL key
 * <p>
 * Created by caojiajun on 2024/4/29
 */
public class TTLCommander extends Commander {

    public TTLCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.TTL;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 2;
    }

    @Override
    public Reply runToCompletion(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        ValueWrapper<KeyMeta> valueWrapper = keyMetaServer.runToComplete(slot, key);
        if (valueWrapper == null) {
            return null;
        }
        return execute0(valueWrapper.get());
    }

    @Override
    protected Reply execute(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(slot, key);
        return execute0(keyMeta);
    }

    private Reply execute0(KeyMeta keyMeta) {
        if (keyMeta == null || keyMeta.isExpire()) {
            return IntegerReply.REPLY_NEGATIVE_2;
        }
        if (keyMeta.getExpireTime() < 0) {
            return IntegerReply.REPLY_NEGATIVE_1;
        }
        long expireTime = keyMeta.getExpireTime();
        long ttl = (expireTime - System.currentTimeMillis()) / 1000;
        if (ttl <= 0) {
            return IntegerReply.REPLY_NEGATIVE_2;
        }
        return new IntegerReply(ttl);
    }
}
