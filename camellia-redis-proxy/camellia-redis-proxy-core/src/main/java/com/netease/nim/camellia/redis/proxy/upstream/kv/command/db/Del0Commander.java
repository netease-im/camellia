package com.netease.nim.camellia.redis.proxy.upstream.kv.command.db;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;


/**
 * Created by caojiajun on 2024/4/8
 */
public abstract class Del0Commander extends Commander {

    public Del0Commander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 2;
    }

    @Override
    protected Reply execute(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(slot, key);
        int ret = 0;
        if (keyMeta != null) {
            keyMetaServer.deleteKeyMeta(slot, key);
            ret = 1;
            gcExecutor.submitSubKeyDeleteTask(slot, key, keyMeta);
            //
            byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);
            KeyType keyType = keyMeta.getKeyType();
            EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
            //redis
            if (keyType == KeyType.zset && encodeVersion == EncodeVersion.version_1) {
                storageRedisTemplate.sendDel(cacheKey);
            }
            //local
            if (keyType == KeyType.zset && cacheConfig.isZSetLocalCacheEnable()) {
                cacheConfig.getZSetLRUCache().del(slot, cacheKey);
            }
            if (keyType == KeyType.hash && cacheConfig.isHashLocalCacheEnable()) {
                cacheConfig.getHashLRUCache().del(slot, cacheKey);
            }
            if (keyType == KeyType.set && cacheConfig.isSetLocalCacheEnable()) {
                cacheConfig.getSetLRUCache().del(slot, cacheKey);
            }
        }
        return IntegerReply.parse(ret);
    }
}
