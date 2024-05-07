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
import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 * PEXPIRE key milliseconds [NX | XX | GT | LT]
 * <p>
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
        return objects.length == 3 || objects.length == 4;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        long expireMillis = Utils.bytesToNum(objects[2]);
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            return IntegerReply.REPLY_0;
        }
        long expireTime = System.currentTimeMillis() + expireMillis;
        String arg = null;
        if (objects.length == 4) {
            arg = Utils.bytesToString(objects[3]);
        }
        if (arg != null) {
            if (arg.equalsIgnoreCase("NX")) {
                if (keyMeta.getExpireTime() > 0) {
                    return IntegerReply.REPLY_0;
                }
            } else if (arg.equalsIgnoreCase("XX")) {
                if (keyMeta.getExpireTime() < 0) {
                    return IntegerReply.REPLY_0;
                }
            } else if (arg.equalsIgnoreCase("GT")) {
                if (expireTime <= keyMeta.getExpireTime()) {
                    return IntegerReply.REPLY_0;
                }
            } else if (arg.equalsIgnoreCase("LT")) {
                if (expireTime >= keyMeta.getExpireTime()) {
                    return IntegerReply.REPLY_0;
                }
            }
        }
        keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(),
                keyMeta.getKeyVersion(), expireTime, keyMeta.getExtra());
        keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
        //
        KeyType keyType = keyMeta.getKeyType();
        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (keyType == KeyType.zset && encodeVersion == EncodeVersion.version_3) {
            byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);
            storeRedisTemplate.sendPExpire(cacheKey, expireTime - System.currentTimeMillis() + 1000L);
        }
        if (keyType == KeyType.zset && (encodeVersion == EncodeVersion.version_1 || encodeVersion == EncodeVersion.version_2)) {
            byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);
            long pexpire = Math.min(expireTime - System.currentTimeMillis() + 1000L, cacheConfig.zsetRangeCacheMillis());
            cacheRedisTemplate.sendPExpire(cacheKey, pexpire);
        }
        if (keyType == KeyType.hash && (encodeVersion == EncodeVersion.version_2 || encodeVersion == EncodeVersion.version_3)) {
            byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);
            long pexpire = Math.min(expireTime - System.currentTimeMillis() + 1000L, cacheConfig.hgetallCacheMillis());
            cacheRedisTemplate.sendPExpire(cacheKey, pexpire);
        }
        return IntegerReply.REPLY_1;
    }
}
