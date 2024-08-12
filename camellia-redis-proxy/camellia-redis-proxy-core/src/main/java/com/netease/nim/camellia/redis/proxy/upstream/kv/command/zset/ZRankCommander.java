package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.Index;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;
import com.netease.nim.camellia.tools.utils.Pair;

import java.util.Arrays;
import java.util.List;

/**
 * ZRANK key member [WITHSCORE]
 * <p>
 * Created by caojiajun on 2024/6/6
 */
public class ZRankCommander extends ZSet0Commander {

    public ZRankCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZRANK;
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
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            return BulkReply.NIL_REPLY;
        }
        if (keyMeta.getKeyType() != KeyType.zset) {
            return ErrorReply.WRONG_TYPE;
        }

        BytesKey member = new BytesKey(objects[2]);
        boolean withScores = ZSetWithScoresUtils.isWithScores(objects, 3);

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisZSet> bufferValue = zsetWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisZSet zSet = bufferValue.getValue();
            Pair<Integer, ZSetTuple> zrank = zSet.zrank(member);
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            return toReply(zrank, withScores);
        }

        if (cacheConfig.isZSetLocalCacheEnable()) {
            ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();

            RedisZSet zSet = zSetLRUCache.getForRead(key, cacheKey);

            if (zSet != null) {
                Pair<Integer, ZSetTuple> zrank = zSet.zrank(member);
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return toReply(zrank, withScores);
            }

            boolean hotKey = zSetLRUCache.isHotKey(key);

            if (hotKey) {
                zSet = loadLRUCache(keyMeta, key);
                if (zSet != null) {
                    zSetLRUCache.putZSetForRead(key, cacheKey, zSet);
                    Pair<Integer, ZSetTuple> zrank = zSet.zrank(member);
                    KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                    return toReply(zrank, withScores);
                }
            }
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        if (encodeVersion == EncodeVersion.version_0) {
            Pair<Integer, ZSetTuple> zrank = zrankFromKv(keyMeta, key, member);
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            return toReply(zrank, withScores);
        }

        if (encodeVersion == EncodeVersion.version_1) {
            byte[][] cmd = new byte[objects.length][];
            System.arraycopy(objects, 0, cmd, 0, cmd.length);
            cmd[1] = cacheKey;
            Index index = Index.fromRaw(cmd[2]);
            cmd[2] = index.getRef();
            KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
            return sync(redisTemplate.sendCommand(new Command(cmd)));
        }

        return ErrorReply.INTERNAL_ERROR;
    }

    private Pair<Integer, ZSetTuple> zrankFromKv(KeyMeta keyMeta, byte[] key, BytesKey member) {
        int scanBatch = kvConfig.scanBatch();
        byte[] startKey = keyDesign.zsetMemberSubKey1(keyMeta, key, new byte[0]);
        byte[] prefix = startKey;
        int index = 0;
        while (true) {
            List<KeyValue> scan = kvClient.scanByPrefix(startKey, prefix, scanBatch, Sort.ASC, false);
            if (scan.isEmpty()) {
                return null;
            }
            for (KeyValue keyValue : scan) {
                if (keyValue == null || keyValue.getValue() == null) {
                    continue;
                }
                startKey = keyValue.getKey();
                if (Arrays.equals(keyDesign.decodeZSetMemberBySubKey1(startKey, key), member.getKey())) {
                    return new Pair<>(index, new ZSetTuple(member, Utils.bytesToDouble(keyValue.getValue())));
                }
                index++;
            }
            if (scan.size() < scanBatch) {
                return null;
            }
        }
    }

    private Reply toReply(Pair<Integer, ZSetTuple> zrank, boolean withScores) {
        if (zrank == null) {
            return BulkReply.NIL_REPLY;
        }
        if (withScores) {
            Reply[] replies = new Reply[2];
            replies[0] = IntegerReply.parse(zrank.getFirst());
            replies[1] = new BulkReply(zrank.getSecond().getMember().getKey());
            return new MultiBulkReply(replies);
        } else {
            return IntegerReply.parse(zrank.getFirst());
        }
    }
}
