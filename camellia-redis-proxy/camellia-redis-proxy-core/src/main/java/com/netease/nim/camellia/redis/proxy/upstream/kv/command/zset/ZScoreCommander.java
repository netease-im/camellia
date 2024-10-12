package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ValueWrapper;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.Index;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;


/**
 * ZSCORE key member
 * <p>
 * Created by caojiajun on 2024/5/15
 */
public class ZScoreCommander extends ZSet0Commander {

    public ZScoreCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZSCORE;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 3;
    }

    @Override
    public Reply runToCompletion(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];

        ValueWrapper<KeyMeta> valueWrapper = keyMetaServer.runToComplete(slot, key);
        if (valueWrapper == null) {
            return null;
        }
        KeyMeta keyMeta = valueWrapper.get();
        if (keyMeta == null) {
            return BulkReply.NIL_REPLY;
        }
        if (keyMeta.getKeyType() != KeyType.zset) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] member = objects[2];

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisZSet> bufferValue = zsetWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisZSet zSet = bufferValue.getValue();
            Double zscore = zSet.zscore(new BytesKey(member));
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            if (zscore == null) {
                return BulkReply.NIL_REPLY;
            } else {
                return new BulkReply(Utils.doubleToBytes(zscore));
            }
        }

        if (cacheConfig.isZSetLocalCacheEnable()) {
            ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();
            RedisZSet zSet = zSetLRUCache.getForRead(slot, cacheKey);

            if (zSet != null) {
                Double zscore = zSet.zscore(new BytesKey(member));
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                if (zscore == null) {
                    return BulkReply.NIL_REPLY;
                } else {
                    return new BulkReply(Utils.doubleToBytes(zscore));
                }
            }
        }
        return null;
    }

    @Override
    protected Reply execute(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(slot, key);
        if (keyMeta == null) {
            return BulkReply.NIL_REPLY;
        }
        if (keyMeta.getKeyType() != KeyType.zset) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] member = objects[2];

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisZSet> bufferValue = zsetWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisZSet zSet = bufferValue.getValue();
            Double zscore = zSet.zscore(new BytesKey(member));
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            if (zscore == null) {
                return BulkReply.NIL_REPLY;
            } else {
                return new BulkReply(Utils.doubleToBytes(zscore));
            }
        }

        if (cacheConfig.isZSetLocalCacheEnable()) {
            ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();
            RedisZSet zSet = zSetLRUCache.getForRead(slot, cacheKey);

            if (zSet != null) {
                Double zscore = zSet.zscore(new BytesKey(member));
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                if (zscore == null) {
                    return BulkReply.NIL_REPLY;
                } else {
                    return new BulkReply(Utils.doubleToBytes(zscore));
                }
            }

            boolean hotKey = zSetLRUCache.isHotKey(key, redisCommand());

            if (hotKey) {
                zSet = loadLRUCache(slot, keyMeta, key);
                if (zSet != null) {
                    //
                    KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                    //
                    zSetLRUCache.putZSetForRead(slot, cacheKey, zSet);
                    Double zscore = zSet.zscore(new BytesKey(member));
                    if (zscore == null) {
                        return BulkReply.NIL_REPLY;
                    } else {
                        return new BulkReply(Utils.doubleToBytes(zscore));
                    }
                }
            }
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            return zscoreFromKv(slot, keyMeta, key, member);
        }

        if (encodeVersion == EncodeVersion.version_1) {
            Index index = Index.fromRaw(member);
            KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
            return sync(storageRedisTemplate.sendCommand(new Command(new byte[][]{RedisCommand.ZSCORE.raw(), cacheKey, index.getRef()})));
        }

        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zscoreFromKv(int slot, KeyMeta keyMeta, byte[] key, byte[] member) {
        byte[] subKey1 = keyDesign.zsetMemberSubKey1(keyMeta, key, member);
        KeyValue keyValue = kvClient.get(slot, subKey1);
        if (keyValue == null || keyValue.getValue() == null) {
            return BulkReply.NIL_REPLY;
        }
        return new BulkReply(keyValue.getValue());
    }

}
