package com.netease.nim.camellia.redis.proxy.kv.core.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.kv.core.command.Commander;
import com.netease.nim.camellia.redis.proxy.kv.core.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.kv.core.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.kv.core.meta.KeyMetaVersion;
import com.netease.nim.camellia.redis.proxy.kv.core.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.kv.core.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;

/**
 * HLEN key
 * <p>
 * Created by caojiajun on 2024/4/11
 */
public class HLenCommander extends Commander {

    public HLenCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HLEN;
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
        if (keyMeta == null) {
            return IntegerReply.REPLY_0;
        }
        if (keyMeta.getKeyType() != KeyType.hash) {
            return ErrorReply.WRONG_TYPE;
        }
        if (keyMeta.getKeyMetaVersion() == KeyMetaVersion.version_0) {
            int size = BytesUtils.toInt(keyMeta.getExtra());
            return IntegerReply.parse(size);
        } else if (keyMeta.getKeyMetaVersion() == KeyMetaVersion.version_1) {
            byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);
            Reply reply = sync(cacheRedisTemplate.sendCommand(new Command(new byte[][]{RedisCommand.HLEN.raw(), cacheKey})));
            if (reply instanceof IntegerReply) {
                Long size = ((IntegerReply) reply).getInteger();
                if (size != null && size > 0) {
                    return reply;
                }
            }
            byte[] startKey = keyStruct.hashFieldStoreKey(keyMeta, key, new byte[0]);
            long count = kvClient.count(startKey, startKey, false);
            return IntegerReply.parse(count);
        } else {
            return ErrorReply.INTERNAL_ERROR;
        }
    }
}
