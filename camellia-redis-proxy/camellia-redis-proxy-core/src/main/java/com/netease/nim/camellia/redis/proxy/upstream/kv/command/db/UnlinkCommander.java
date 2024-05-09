package com.netease.nim.camellia.redis.proxy.upstream.kv.command.db;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;

/**
 * UNLINK key
 * <p>
 * Created by caojiajun on 2024/4/8
 */
public class UnlinkCommander extends Commander {

    public UnlinkCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.UNLINK;
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
        int ret = 0;
        if (keyMeta != null) {
            keyMetaServer.deleteKeyMeta(key);
            ret = 1;
            gcExecutor.submitSubKeyDeleteTask(key, keyMeta);

            KeyType keyType = keyMeta.getKeyType();
            EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
            if (keyType == KeyType.zset && (encodeVersion == EncodeVersion.version_1
                    || encodeVersion == EncodeVersion.version_2 || encodeVersion == EncodeVersion.version_3)) {
                byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);
                storeRedisTemplate.sendDel(cacheKey);
            }
            if (keyType == KeyType.hash && (encodeVersion == EncodeVersion.version_2 || encodeVersion == EncodeVersion.version_3)) {
                byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);
                cacheRedisTemplate.sendDel(cacheKey);
            }
        }
        return IntegerReply.parse(ret);
    }
}
