package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.ArrayList;
import java.util.List;

/**
 * ZREVRANGEBYSCORE key max min [WITHSCORES] [LIMIT offset count]
 * <p>
 * Created by caojiajun on 2024/4/11
 */
public class ZRevRangeByScoreCommander extends ZSet0Commander {

    public ZRevRangeByScoreCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZREVRANGEBYSCORE;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length >= 4;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            return MultiBulkReply.EMPTY;
        }
        if (keyMeta.getKeyType() != KeyType.zset) {
            return ErrorReply.WRONG_TYPE;
        }
        boolean withScores = ZSetWithScoresUtils.isWithScores(objects, 4);

        ZSetScore maxScore;
        ZSetScore minScore;
        ZSetLimit limit;
        try {
            maxScore = ZSetScore.fromBytes(objects[2]);
            minScore = ZSetScore.fromBytes(objects[3]);
            limit = ZSetLimit.fromBytes(objects, 4);
        } catch (Exception e) {
            ErrorLogCollector.collect(ZRevRangeByScoreCommander.class, "zrevrangebyscore command syntax error, illegal min/max/limit");
            return ErrorReply.SYNTAX_ERROR;
        }
        if (minScore.getScore() > maxScore.getScore()) {
            return MultiBulkReply.EMPTY;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisZSet> bufferValue = zsetWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisZSet zSet = bufferValue.getValue();
            List<ZSetTuple> list = zSet.zrevrangeByScore(minScore, maxScore, limit);
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            return ZSetTupleUtils.toReply(list, withScores);
        }

        if (cacheConfig.isZSetLocalCacheEnable()) {
            ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();

            RedisZSet zSet = zSetLRUCache.getForRead(key, cacheKey);

            if (zSet != null) {
                List<ZSetTuple> list = zSet.zrevrangeByScore(minScore, maxScore, limit);
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return ZSetTupleUtils.toReply(list, withScores);
            }

            boolean hotKey = zSetLRUCache.isHotKey(key);

            if (hotKey) {
                zSet = loadLRUCache(keyMeta, key);
                if (zSet != null) {
                    //
                    zSetLRUCache.putZSetForRead(key, cacheKey, zSet);
                    //
                    List<ZSetTuple> list = zSet.zrevrangeByScore(minScore, maxScore, limit);

                    KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());

                    return ZSetTupleUtils.toReply(list, withScores);
                }
            }
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            if (!kvClient.supportReverseScan()) {
                return zrevrangeByScoreVersion0NotSupportReverseScan(keyMeta, key, cacheKey, minScore, maxScore, limit, withScores);
            }
            return zrevrangeByScoreVersion0(keyMeta, key, minScore, maxScore, limit, withScores);
        }

        if (encodeVersion == EncodeVersion.version_1) {
            KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
            return zrangeVersion1(keyMeta, key, cacheKey, objects, withScores);
        }

        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zrevrangeByScoreVersion0NotSupportReverseScan(KeyMeta keyMeta, byte[] key, byte[] cacheKey, ZSetScore minScore, ZSetScore maxScore, ZSetLimit limit, boolean withScores) {
        RedisZSet zSet = loadLRUCache(keyMeta, key);
        if (zSet == null) {
            return ErrorReply.NOT_SUPPORT;
        }
        //
        if (cacheConfig.isZSetLocalCacheEnable()) {
            ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();
            zSetLRUCache.putZSetForRead(key, cacheKey, zSet);
        }
        //
        List<ZSetTuple> list = zSet.zrevrangeByScore(minScore, maxScore, limit);

        return ZSetTupleUtils.toReply(list, withScores);
    }

    private Reply zrevrangeByScoreVersion0(KeyMeta keyMeta, byte[] key, ZSetScore minScore, ZSetScore maxScore, ZSetLimit limit, boolean withScores) {
        byte[] startKey = BytesUtils.nextBytes(keyDesign.zsetMemberSubKey2(keyMeta, key, new byte[0], BytesUtils.toBytes(maxScore.getScore())));
        byte[] endKey = keyDesign.zsetMemberSubKey2(keyMeta, key, new byte[0], BytesUtils.toBytes(minScore.getScore()));
        int batch = kvConfig.scanBatch();
        int count = 0;
        List<ZSetTuple> result = new ArrayList<>(limit.getCount() < 0 ? 16 : Math.min(limit.getCount(), 100));
        while (true) {
            if (limit.getCount() > 0) {
                batch = Math.min(kvConfig.scanBatch(), limit.getCount() - result.size());
            }
            List<KeyValue> list = kvClient.scanByStartEnd(startKey, endKey, batch, Sort.DESC, !maxScore.isExcludeScore());
            if (list.isEmpty()) {
                break;
            }
            for (KeyValue keyValue : list) {
                if (keyValue == null) {
                    continue;
                }
                startKey = keyValue.getKey();
                if (keyValue.getValue() == null) {
                    continue;
                }
                double score = keyDesign.decodeZSetScoreBySubKey2(keyValue.getKey(), key);
                boolean pass = ZSetScoreUtils.checkScore(score, minScore, maxScore);
                if (!pass) {
                    continue;
                }
                if (count >= limit.getOffset()) {
                    byte[] member = keyDesign.decodeZSetMemberBySubKey2(keyValue.getKey(), key);
                    ZSetTuple tuple;
                    if (withScores) {
                        tuple = new ZSetTuple(new BytesKey(member), score);
                    } else {
                        tuple = new ZSetTuple(new BytesKey(member), null);
                    }
                    result.add(tuple);
                    if (limit.getCount() > 0 && result.size() >= limit.getCount()) {
                        break;
                    }
                }
                count ++;
            }
            if (list.size() < batch) {
                break;
            }
        }
        return ZSetTupleUtils.toReply(result, withScores);
    }

}
