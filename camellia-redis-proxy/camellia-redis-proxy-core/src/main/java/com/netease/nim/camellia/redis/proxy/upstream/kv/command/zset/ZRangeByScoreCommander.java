package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ValueWrapper;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;

import java.util.List;

/**
 * ZRANGEBYSCORE key min max [WITHSCORES] [LIMIT offset count]
 * <p>
 * Created by caojiajun on 2024/4/11
 */
public class ZRangeByScoreCommander extends ZRangeByScore0Commander {

    public ZRangeByScoreCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZRANGEBYSCORE;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length >= 4;
    }

    @Override
    public Reply runToCompletion(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        ValueWrapper<KeyMeta> valueWrapper = keyMetaServer.runToCompletion(slot, key);
        if (valueWrapper == null) {
            return null;
        }
        KeyMeta keyMeta = valueWrapper.get();
        if (keyMeta == null) {
            return MultiBulkReply.EMPTY;
        }
        if (keyMeta.getKeyType() != KeyType.zset) {
            return ErrorReply.WRONG_TYPE;
        }
        boolean withScores = ZSetWithScoresUtils.isWithScores(objects, 4);

        ZSetScore minScore;
        ZSetScore maxScore;
        ZSetLimit limit;
        try {
            minScore = ZSetScore.fromBytes(objects[2]);
            maxScore = ZSetScore.fromBytes(objects[3]);
            limit = ZSetLimit.fromBytes(objects, 4);
        } catch (Exception e) {
            ErrorLogCollector.collect(ZRangeByLexCommander.class, "zrangebyscore command syntax error, illegal min/max/limit");
            return ErrorReply.SYNTAX_ERROR;
        }
        if (minScore.getScore() > maxScore.getScore()) {
            return MultiBulkReply.EMPTY;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisZSet> bufferValue = zsetWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisZSet zSet = bufferValue.getValue();
            List<ZSetTuple> list = zSet.zrangebyscore(minScore, maxScore, limit);
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            return ZSetTupleUtils.toReply(list, withScores);
        }

        if (cacheConfig.isZSetLocalCacheEnable()) {
            ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();

            RedisZSet zSet = zSetLRUCache.getForRead(slot, cacheKey);

            if (zSet != null) {
                List<ZSetTuple> list = zSet.zrangebyscore(minScore, maxScore, limit);
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return ZSetTupleUtils.toReply(list, withScores);
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
            return MultiBulkReply.EMPTY;
        }
        if (keyMeta.getKeyType() != KeyType.zset) {
            return ErrorReply.WRONG_TYPE;
        }
        boolean withScores = ZSetWithScoresUtils.isWithScores(objects, 4);

        ZSetScore minScore;
        ZSetScore maxScore;
        ZSetLimit limit;
        try {
            minScore = ZSetScore.fromBytes(objects[2]);
            maxScore = ZSetScore.fromBytes(objects[3]);
            limit = ZSetLimit.fromBytes(objects, 4);
        } catch (Exception e) {
            ErrorLogCollector.collect(ZRangeByLexCommander.class, "zrangebyscore command syntax error, illegal min/max/limit");
            return ErrorReply.SYNTAX_ERROR;
        }
        if (minScore.getScore() > maxScore.getScore()) {
            return MultiBulkReply.EMPTY;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisZSet> bufferValue = zsetWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisZSet zSet = bufferValue.getValue();
            List<ZSetTuple> list = zSet.zrangebyscore(minScore, maxScore, limit);
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            return ZSetTupleUtils.toReply(list, withScores);
        }

        if (cacheConfig.isZSetLocalCacheEnable()) {
            ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();

            RedisZSet zSet = zSetLRUCache.getForRead(slot, cacheKey);

            if (zSet != null) {
                List<ZSetTuple> list = zSet.zrangebyscore(minScore, maxScore, limit);
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return ZSetTupleUtils.toReply(list, withScores);
            }

            boolean hotKey = zSetLRUCache.isHotKey(key, redisCommand());

            if (hotKey) {
                zSet = loadLRUCache(slot, keyMeta, key);
                if (zSet != null) {
                    //
                    zSetLRUCache.putZSetForRead(slot, cacheKey, zSet);
                    //
                    List<ZSetTuple> list = zSet.zrangebyscore(minScore, maxScore, limit);

                    KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());

                    return ZSetTupleUtils.toReply(list, withScores);
                }
            }
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        if (encodeVersion == EncodeVersion.version_0) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            List<ZSetTuple> list = zrangeByScoreVersion0(slot, keyMeta, key, minScore, maxScore, limit, withScores);
            return ZSetTupleUtils.toReply(list, withScores);
        }

        if (encodeVersion == EncodeVersion.version_1) {
            KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
            return zrangeVersion1(slot, keyMeta, key, cacheKey, objects, withScores);
        }

        return ErrorReply.INTERNAL_ERROR;
    }

}
