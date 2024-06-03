package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ZREMRANGEBYSCORE key min max
 * <p>
 * Created by caojiajun on 2024/5/8
 */
public class ZRemRangeByScoreCommander extends ZRemRange0Commander {

    private static final byte[] script = ("local ret1 = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(ret1) == 1 then\n" +
            "  local ret = redis.call('zrangebyscore', KEYS[1], unpack(ARGV));\n" +
            "  return {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

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

        ZSetScore minScore;
        ZSetScore maxScore;
        try {
            minScore = ZSetScore.fromBytes(objects[2]);
            maxScore = ZSetScore.fromBytes(objects[3]);
        } catch (Exception e) {
            return ErrorReply.SYNTAX_ERROR;
        }
        if (minScore.getScore() > maxScore.getScore()) {
            return IntegerReply.REPLY_0;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        Map<BytesKey, Double> localCacheResult = null;

        if (cacheConfig.isZSetLocalCacheEnable()) {
            localCacheResult = cacheConfig.getZSetLRUCache().zremrangeByScore(cacheKey, minScore, maxScore);
            if (localCacheResult != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
            }
            if (localCacheResult != null && localCacheResult.isEmpty()) {
                return IntegerReply.REPLY_0;
            }
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0) {
            return zremrangeByScore(keyMeta, key, minScore, maxScore, localCacheResult);
        }

        byte[][] args = new byte[objects.length - 2][];
        System.arraycopy(objects, 2, args, 0, args.length);

        if (encodeVersion == EncodeVersion.version_1) {
            return zremrangeVersion1(keyMeta, key, cacheKey, args, script);
        }
        if (encodeVersion == EncodeVersion.version_2) {
            return zremrangeVersion2(keyMeta, key, cacheKey, args, script);
        }
        if (encodeVersion == EncodeVersion.version_3) {
            return zremrangeVersion3(keyMeta, key, cacheKey, objects, redisCommand());
        }
        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zremrangeByScore(KeyMeta keyMeta, byte[] key, ZSetScore minScore, ZSetScore maxScore, Map<BytesKey, Double> localCacheResult) {
        if (localCacheResult != null) {
            int size = BytesUtils.toInt(keyMeta.getExtra());
            byte[][] deleteStoreKeys = new byte[localCacheResult.size()*2][];
            int i = 0;
            for (Map.Entry<BytesKey, Double> entry : localCacheResult.entrySet()) {
                deleteStoreKeys[i] = keyDesign.zsetMemberSubKey1(keyMeta, key, entry.getKey().getKey());
                deleteStoreKeys[i+1] = keyDesign.zsetMemberSubKey2(keyMeta, key, entry.getKey().getKey(), BytesUtils.toBytes(entry.getValue()));
                i+=2;
            }
            kvClient.batchDelete(deleteStoreKeys);
            size = size - localCacheResult.size();
            updateKeyMeta(keyMeta, key, size);
            return IntegerReply.parse(localCacheResult.size());
        }

        byte[] startKey = keyDesign.zsetMemberSubKey2(keyMeta, key, new byte[0], BytesUtils.toBytes(minScore.getScore()));
        byte[] endKey = BytesUtils.nextBytes(keyDesign.zsetMemberSubKey2(keyMeta, key, new byte[0], BytesUtils.toBytes(maxScore.getScore())));
        int batch = kvConfig.scanBatch();
        List<byte[]> toDeleteKeys = new ArrayList<>();
        int count = 0;
        while (true) {
            List<KeyValue> list = kvClient.scanByStartEnd(startKey, endKey, batch, Sort.ASC, false);
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
                toDeleteKeys.add(keyValue.getKey());
                byte[] member = keyDesign.decodeZSetMemberBySubKey2(keyValue.getKey(), key);
                byte[] subKey1 = keyDesign.zsetMemberSubKey1(keyMeta, key, member);
                toDeleteKeys.add(subKey1);
                count ++;
            }
            if (list.size() < batch) {
                break;
            }
        }
        if (!toDeleteKeys.isEmpty()) {
            kvClient.batchDelete(toDeleteKeys.toArray(new byte[0][0]));
            int size = BytesUtils.toInt(keyMeta.getExtra());
            size = size - count;
            updateKeyMeta(keyMeta, key, size);
            return IntegerReply.parse(count);
        }
        return IntegerReply.REPLY_0;
    }
}
