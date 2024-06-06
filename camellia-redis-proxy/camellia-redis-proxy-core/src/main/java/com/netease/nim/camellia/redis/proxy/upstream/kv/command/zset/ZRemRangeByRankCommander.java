package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.NoOpResult;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ZREMRANGEBYRANK key start stop
 * <p>
 * Created by caojiajun on 2024/5/8
 */
public class ZRemRangeByRankCommander extends ZRemRange0Commander {

    private static final byte[] script = ("local ret1 = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(ret1) == 1 then\n" +
            "  local ret = redis.call('zrange', KEYS[1], unpack(ARGV));\n" +
            "  return {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

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
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            return IntegerReply.REPLY_0;
        }
        if (keyMeta.getKeyType() != KeyType.zset) {
            return ErrorReply.WRONG_TYPE;
        }

        int start = (int) Utils.bytesToNum(objects[2]);
        int stop = (int) Utils.bytesToNum(objects[3]);

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);
        Map<BytesKey, Double> localCacheResult = null;
        Result result = null;

        WriteBufferValue<ZSet> bufferValue = zsetWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            ZSet zSet = bufferValue.getValue();
            localCacheResult = zSet.zremrangeByRank(start, stop);
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            if (localCacheResult != null && localCacheResult.isEmpty()) {
                return IntegerReply.REPLY_0;
            }
            result = zsetWriteBuffer.put(cacheKey, zSet);
        }

        if (cacheConfig.isZSetLocalCacheEnable()) {
            ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();

            boolean hotKey = zSetLRUCache.isHotKey(key);

            if (localCacheResult == null) {
                localCacheResult = zSetLRUCache.zremrangeByRank(cacheKey, start, stop);
                if (localCacheResult != null) {
                    KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                }
                if (localCacheResult != null && localCacheResult.isEmpty()) {
                    return IntegerReply.REPLY_0;
                }
            } else {
                zSetLRUCache.zremrangeByRank(cacheKey, start, stop);
            }

            if (hotKey && localCacheResult == null) {
                ZSet zSet = loadLRUCache(keyMeta, key);
                if (zSet != null) {
                    //
                    zSetLRUCache.putZSetForWrite(cacheKey, zSet);
                    //
                    localCacheResult = zSet.zremrangeByRank(start, stop);
                }
            }

            if (result == null) {
                ZSet zSet = zSetLRUCache.getForWrite(cacheKey);
                if (zSet != null) {
                    result = zsetWriteBuffer.put(cacheKey, zSet.duplicate());
                }
            }
        }

        if (result == null) {
            result = NoOpResult.INSTANCE;
        }

        if (localCacheResult != null) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0) {
            return zremrangeByRankVersion0(keyMeta, key, cacheKey, start, stop, localCacheResult, result);
        }

        if (encodeVersion == EncodeVersion.version_3) {
            return zremrangeVersion3(keyMeta, key, cacheKey, objects, redisCommand(), localCacheResult, result);
        }

        byte[][] args = new byte[objects.length - 2][];
        System.arraycopy(objects, 2, args, 0, args.length);

        if (encodeVersion == EncodeVersion.version_1) {
            return zremrangeVersion1(keyMeta, key, cacheKey, args, script, localCacheResult, result);
        }
        if (encodeVersion == EncodeVersion.version_2) {
            return zremrangeVersion2(keyMeta, key, cacheKey, args, script, localCacheResult, result);
        }

        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zremrangeByRankVersion0(KeyMeta keyMeta, byte[] key, byte[] cacheKey, int start, int stop, Map<BytesKey, Double> localCacheResult, Result result) {
        int size = BytesUtils.toInt(keyMeta.getExtra());
        ZSetRank rank = new ZSetRank(start, stop, size);
        if (rank.isEmptyRank()) {
            return MultiBulkReply.EMPTY;
        }
        start = rank.getStart();
        stop = rank.getStop();

        if (localCacheResult != null) {
            byte[][] deleteStoreKeys = new byte[localCacheResult.size()*2][];
            int i = 0;
            for (Map.Entry<BytesKey, Double> entry : localCacheResult.entrySet()) {
                deleteStoreKeys[i] = keyDesign.zsetMemberSubKey1(keyMeta, key, entry.getKey().getKey());
                deleteStoreKeys[i+1] = keyDesign.zsetMemberSubKey2(keyMeta, key, entry.getKey().getKey(), BytesUtils.toBytes(entry.getValue()));
                i+=2;
            }

            if (result.isKvWriteDelayEnable()) {
                submitAsyncWriteTask(cacheKey, result, () -> kvClient.batchDelete(deleteStoreKeys));
            } else {
                kvClient.batchDelete(deleteStoreKeys);
            }

            size = size - localCacheResult.size();
            updateKeyMeta(keyMeta, key, size);

            return IntegerReply.parse(localCacheResult.size());
        }

        byte[] startKey = keyDesign.zsetMemberSubKey1(keyMeta, key, new byte[0]);
        int ret = zremrangeByRank0(keyMeta, key, startKey, startKey, start, stop);
        if (ret > 0) {
            size = size - ret;
            updateKeyMeta(keyMeta, key, size);
        }
        return IntegerReply.parse(ret);
    }

    private int zremrangeByRank0(KeyMeta keyMeta, byte[] key, byte[] startKey, byte[] prefix, int start, int stop) {
        int targetSize = stop - start;
        List<byte[]> list = new ArrayList<>(Math.min(targetSize, 512));
        int scanBatch = kvConfig.scanBatch();
        int count = 0;
        while (true) {
            int limit = Math.min(targetSize - list.size(), scanBatch);
            List<KeyValue> scan = kvClient.scanByPrefix(startKey, prefix, limit, Sort.ASC, false);
            if (scan.isEmpty()) {
                return deleteKv(list);
            }
            for (KeyValue keyValue : scan) {
                if (keyValue == null || keyValue.getValue() == null) {
                    continue;
                }
                startKey = keyValue.getKey();
                if (count >= start) {
                    byte[] member = keyDesign.decodeZSetMemberBySubKey1(keyValue.getKey(), key);
                    list.add(keyValue.getKey());
                    double score = Utils.bytesToDouble(keyValue.getValue());
                    list.add(keyDesign.zsetMemberSubKey2(keyMeta, key, member, BytesUtils.toBytes(score)));
                }
                if (count >= stop) {
                    return deleteKv(list);
                }
                count++;
            }
            if (scan.size() < limit) {
                return deleteKv(list);
            }
        }
    }

    private int deleteKv(List<byte[]> list) {
        kvClient.batchDelete(list.toArray(new byte[0][0]));
        return list.size() / 2;
    }
}
