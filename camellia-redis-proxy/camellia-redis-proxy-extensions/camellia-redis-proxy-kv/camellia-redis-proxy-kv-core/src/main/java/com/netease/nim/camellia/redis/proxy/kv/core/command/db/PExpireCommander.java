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
 * Created by caojiajun on 2024/4/8
 */
public class PExpireCommander extends Commander {

    public PExpireCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.PEXPIRE;
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
        long expireMillis = Utils.bytesToNum(objects[2]);
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key, null, false);
        if (keyMeta == null) {
            return IntegerReply.REPLY_0;
        }
        keyMeta = new KeyMeta(keyMeta.getKeyType(), keyMeta.getVersion(), System.currentTimeMillis() + expireMillis);
        keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
        return IntegerReply.REPLY_1;
    }
}
