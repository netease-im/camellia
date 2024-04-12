package com.netease.nim.camellia.redis.proxy.kv.core.command.db;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.kv.core.command.Commander;
import com.netease.nim.camellia.redis.proxy.kv.core.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.kv.core.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 * EXPIRE key seconds
 * <p>
 * Created by caojiajun on 2024/4/8
 */
public class ExpireCommander extends Commander {

    public ExpireCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.EXPIRE;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 3;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        long expireSeconds = Utils.bytesToNum(objects[2]);
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            return IntegerReply.REPLY_0;
        }
        keyMeta = new KeyMeta(keyMeta.getKeyMetaVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(),
                System.currentTimeMillis() + expireSeconds*1000L, keyMeta.getExtra());
        keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
        return IntegerReply.REPLY_1;
    }
}
