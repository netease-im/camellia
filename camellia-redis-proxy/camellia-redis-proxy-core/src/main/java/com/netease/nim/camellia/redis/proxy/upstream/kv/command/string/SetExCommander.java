package com.netease.nim.camellia.redis.proxy.upstream.kv.command.string;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 * SETEX key seconds value
 * <p>
 * Created by caojiajun on 2024/4/11
 */
public class SetExCommander extends Commander {

    public SetExCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.SETEX;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 4;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        long seconds = Utils.bytesToNum(objects[2]);
        byte[] value = objects[3];

        long expireTime = System.currentTimeMillis() + seconds * 1000L;
        KeyMeta keyMeta = new KeyMeta(EncodeVersion.version_0, KeyType.string, System.currentTimeMillis(), expireTime, value);
        keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
        return null;
    }
}
