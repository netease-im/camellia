package com.netease.nim.camellia.redis.proxy.upstream.kv.command.db;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;

/**
 * EXPIRETIME key
 * <p>
 * Created by caojiajun on 2024/4/30
 */
public class ExpireTimeCommander extends Commander {

    public ExpireTimeCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.EXPIRETIME;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 2;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null || keyMeta.isExpire()) {
            return IntegerReply.REPLY_NEGATIVE_2;
        }
        if (keyMeta.getExpireTime() < 0) {
            return IntegerReply.REPLY_NEGATIVE_1;
        }
        long expireTime = keyMeta.getExpireTime();
        return new IntegerReply(expireTime / 1000);
    }
}
