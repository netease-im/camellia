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
 * ZRANGEBYLEX key min max [LIMIT offset count]
 * <p>
 * Created by caojiajun on 2024/4/11
 */
public class ZRangeByLexCommander extends ZRange0Commander {

    public ZRangeByLexCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZRANGEBYLEX;
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

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        if (encodeVersion == EncodeVersion.version_1) {
            return ErrorReply.COMMAND_NOT_SUPPORT_IN_CURRENT_KV_ENCODE_VERSION;
        }

        ZSetLex minLex;
        ZSetLex maxLex;
        ZSetLimit limit;
        try {
            minLex = ZSetLex.fromLex(objects[2]);
            maxLex = ZSetLex.fromLex(objects[3]);
            if (minLex == null || maxLex == null) {
                return new ErrorReply("ERR min or max not valid string range item");
            }
            limit = ZSetLimit.fromBytes(objects, 4);
        } catch (Exception e) {
            ErrorLogCollector.collect(ZRangeByLexCommander.class, "zlrangebylex command syntax error, illegal min/max lex");
            return ErrorReply.SYNTAX_ERROR;
        }
        if (minLex.isMax() || maxLex.isMin()) {
            return MultiBulkReply.EMPTY;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisZSet> bufferValue = zsetWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisZSet zSet = bufferValue.getValue();
            List<ZSetTuple> list = zSet.zrangeByLex(minLex, maxLex, limit);
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            return ZSetTupleUtils.toReply(list, false);
        }

        if (cacheConfig.isZSetLocalCacheEnable()) {
            ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();

            RedisZSet zSet = zSetLRUCache.getForRead(key, cacheKey);
            if (zSet != null) {
                List<ZSetTuple> list = zSet.zrangeByLex(minLex, maxLex, limit);
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return ZSetTupleUtils.toReply(list, false);
            }

            boolean hotKey = zSetLRUCache.isHotKey(key);

            if (hotKey) {
                zSet = loadLRUCache(keyMeta, key);
                if (zSet != null) {
                    //
                    zSetLRUCache.putZSetForRead(key, cacheKey, zSet);
                    //
                    List<ZSetTuple> list = zSet.zrangeByLex(minLex, maxLex, limit);

                    KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());

                    return ZSetTupleUtils.toReply(list, false);
                }
            }
        }

        if (encodeVersion == EncodeVersion.version_0) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            return zrangeByLexVersion0OrVersion2(keyMeta, key, minLex, maxLex, limit);
        }

        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zrangeByLexVersion0OrVersion2(KeyMeta keyMeta, byte[] key, ZSetLex minLex, ZSetLex maxLex, ZSetLimit limit) {
        byte[] startKey;
        if (minLex.isMin()) {
            startKey = keyDesign.zsetMemberSubKey1(keyMeta, key, new byte[0]);
        } else {
            startKey = keyDesign.zsetMemberSubKey1(keyMeta, key, minLex.getLex());
        }
        byte[] endKey;
        if (maxLex.isMax()) {
            endKey = BytesUtils.nextBytes(keyDesign.zsetMemberSubKey1(keyMeta, key, new byte[0]));
        } else {
            if (maxLex.isExcludeLex()) {
                endKey = keyDesign.zsetMemberSubKey1(keyMeta, key, maxLex.getLex());
            } else {
                endKey = BytesUtils.nextBytes(keyDesign.zsetMemberSubKey1(keyMeta, key, maxLex.getLex()));
            }
        }
        List<ZSetTuple> list = new ArrayList<>();
        int scanBatch = kvConfig.scanBatch();
        int count = 0;
        while (true) {
            List<KeyValue> scan = kvClient.scanByStartEnd(startKey, endKey, scanBatch, Sort.ASC, !minLex.isExcludeLex());
            if (scan.isEmpty()) {
                return ZSetTupleUtils.toReply(list, false);
            }
            for (KeyValue keyValue : scan) {
                if (keyValue == null || keyValue.getValue() == null) {
                    continue;
                }
                startKey = keyValue.getKey();
                byte[] member = keyDesign.decodeZSetMemberBySubKey1(keyValue.getKey(), key);
                boolean pass = ZSetLexUtil.checkLex(member, minLex, maxLex);
                if (!pass) {
                    continue;
                }
                if (count >= limit.getOffset()) {
                    list.add(new ZSetTuple(new BytesKey(member), null));
                }
                if (limit.getCount() > 0 && list.size() >= limit.getCount()) {
                    return ZSetTupleUtils.toReply(list, false);
                }
                count++;
            }
            if (scan.size() < scanBatch) {
                return ZSetTupleUtils.toReply(list, false);
            }
        }
    }

}
