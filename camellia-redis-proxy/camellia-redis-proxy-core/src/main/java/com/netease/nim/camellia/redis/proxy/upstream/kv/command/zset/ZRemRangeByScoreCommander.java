package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.NoOpResult;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.List;
import java.util.Map;

/**
 * ZREMRANGEBYSCORE key min max
 * <p>
 * Created by caojiajun on 2024/5/8
 */
public class ZRemRangeByScoreCommander extends ZRangeByScore0Commander {


    public ZRemRangeByScoreCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZREMRANGEBYSCORE;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 4;
    }

    @Override
    protected Reply execute(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(slot, key);
        if (keyMeta == null) {
            return IntegerReply.REPLY_0;
        }
        if (keyMeta.getKeyType() != KeyType.zset) {
            return ErrorReply.WRONG_TYPE;
        }

        ZSetScore minScore;
        ZSetScore maxScore;
        try {
            minScore = ZSetScore.fromBytes(objects[2]);
            maxScore = ZSetScore.fromBytes(objects[3]);
        } catch (Exception e) {
            ErrorLogCollector.collect(ZRemRangeByScoreCommander.class, "zremrangebyscore command syntax error, illegal min/max/limit");
            return ErrorReply.SYNTAX_ERROR;
        }
        if (minScore.getScore() > maxScore.getScore()) {
            return IntegerReply.REPLY_0;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        KvCacheMonitor.Type type = null;

        Map<BytesKey, Double> removedMap = null;
        Result result = null;

        WriteBufferValue<RedisZSet> bufferValue = zsetWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisZSet zSet = bufferValue.getValue();
            removedMap = zSet.zremrangeByScore(minScore, maxScore);
            type = KvCacheMonitor.Type.write_buffer;
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            if (removedMap != null && removedMap.isEmpty()) {
                return IntegerReply.REPLY_0;
            }
            result = zsetWriteBuffer.put(cacheKey, zSet);
        }

        if (cacheConfig.isZSetLocalCacheEnable()) {
            ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();

            if (removedMap == null) {
                removedMap = zSetLRUCache.zremrangeByScore(slot, cacheKey, minScore, maxScore);
                if (removedMap != null) {
                    type = KvCacheMonitor.Type.local_cache;
                    KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                }
                if (removedMap != null && removedMap.isEmpty()) {
                    return IntegerReply.REPLY_0;
                }
            } else {
                zSetLRUCache.zremrangeByScore(slot, cacheKey, minScore, maxScore);
            }

            if (removedMap == null) {
                boolean hotKey = zSetLRUCache.isHotKey(key);
                if (hotKey) {
                    RedisZSet zSet = loadLRUCache(slot, keyMeta, key);
                    if (zSet != null) {
                        //
                        type = KvCacheMonitor.Type.kv_store;
                        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                        //
                        zSetLRUCache.putZSetForWrite(slot, cacheKey, zSet);
                        //
                        removedMap = zSet.zremrangeByScore(minScore, maxScore);
                        if (removedMap != null && removedMap.isEmpty()) {
                            return IntegerReply.REPLY_0;
                        }
                    }
                }
            }

            if (result == null) {
                RedisZSet zSet = zSetLRUCache.getForWrite(slot, cacheKey);
                if (zSet != null) {
                    result = zsetWriteBuffer.put(cacheKey, zSet.duplicate());
                }
            }
        }

        if (result == null) {
            result = NoOpResult.INSTANCE;
        }

        if (type == null) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0) {
            if (removedMap == null) {
                List<ZSetTuple> list = zrangeByScoreVersion0(slot, keyMeta, key, minScore, maxScore, ZSetLimit.NO_LIMIT, true);
                removedMap = ZSetTupleUtils.toMap(list);
            }
            return zremVersion0(slot, keyMeta, key, cacheKey, removedMap, result);
        }

        if (encodeVersion == EncodeVersion.version_1) {
            return zremrangeVersion1(slot, keyMeta, key, cacheKey, objects, redisCommand(), removedMap, result);
        }

        return ErrorReply.INTERNAL_ERROR;
    }
}
