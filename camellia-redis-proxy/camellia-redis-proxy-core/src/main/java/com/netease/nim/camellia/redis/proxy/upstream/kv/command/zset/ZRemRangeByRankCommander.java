package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.NoOpResult;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetRank;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetTuple;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetTupleUtils;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.List;
import java.util.Map;

/**
 * ZREMRANGEBYRANK key start stop
 * <p>
 * Created by caojiajun on 2024/5/8
 */
public class ZRemRangeByRankCommander extends ZRangeByRank0Commander {


    public ZRemRangeByRankCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZREMRANGEBYRANK;
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

        int start = (int) Utils.bytesToNum(objects[2]);
        int stop = (int) Utils.bytesToNum(objects[3]);

        KvCacheMonitor.Type type = null;

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);
        Map<BytesKey, Double> removedMap = null;
        Result result = null;

        WriteBufferValue<RedisZSet> bufferValue = zsetWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisZSet zSet = bufferValue.getValue();
            removedMap = zSet.zremrangeByRank(start, stop);
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
                removedMap = zSetLRUCache.zremrangeByRank(key, cacheKey, start, stop);
                if (removedMap != null) {
                    type = KvCacheMonitor.Type.local_cache;
                    KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                }
                if (removedMap != null && removedMap.isEmpty()) {
                    return IntegerReply.REPLY_0;
                }
            } else {
                zSetLRUCache.zremrangeByRank(key, cacheKey, start, stop);
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
                        zSetLRUCache.putZSetForWrite(key, cacheKey, zSet);
                        //
                        removedMap = zSet.zremrangeByRank(start, stop);
                        if (removedMap != null && removedMap.isEmpty()) {
                            return IntegerReply.REPLY_0;
                        }
                    }
                }
            }

            if (result == null) {
                RedisZSet zSet = zSetLRUCache.getForWrite(key, cacheKey);
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
            return zremrangeByRankVersion0(slot, keyMeta, key, cacheKey, start, stop, removedMap, result);
        }

        if (encodeVersion == EncodeVersion.version_1) {
            return zremrangeVersion1(slot, keyMeta, key, cacheKey, objects, redisCommand(), removedMap, result);
        }

        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zremrangeByRankVersion0(int slot, KeyMeta keyMeta, byte[] key, byte[] cacheKey, int start, int stop, Map<BytesKey, Double> removedMap, Result result) {

        int size = BytesUtils.toInt(keyMeta.getExtra());
        ZSetRank rank = new ZSetRank(start, stop, size);
        if (rank.isEmptyRank()) {
            return IntegerReply.REPLY_0;
        }
        start = rank.getStart();
        stop = rank.getStop();

        if (removedMap == null) {
            List<ZSetTuple> list = zrangeByRankVersion0(slot, keyMeta, key, start, stop, true);
            removedMap = ZSetTupleUtils.toMap(list);
        }

        return zremVersion0(slot, keyMeta, key, cacheKey, removedMap, result);
    }
}
