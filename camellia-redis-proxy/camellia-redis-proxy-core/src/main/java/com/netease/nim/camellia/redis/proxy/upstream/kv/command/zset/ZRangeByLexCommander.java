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
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;

import java.util.List;

/**
 * ZRANGEBYLEX key min max [LIMIT offset count]
 * <p>
 * Created by caojiajun on 2024/4/11
 */
public class ZRangeByLexCommander extends ZRangeByLex0Commander {

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

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

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

            RedisZSet zSet = zSetLRUCache.getForRead(slot, cacheKey);
            if (zSet != null) {
                List<ZSetTuple> list = zSet.zrangeByLex(minLex, maxLex, limit);
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return ZSetTupleUtils.toReply(list, false);
            }

            boolean hotKey = zSetLRUCache.isHotKey(key);

            if (hotKey) {
                zSet = loadLRUCache(slot, keyMeta, key);
                if (zSet != null) {
                    //
                    zSetLRUCache.putZSetForRead(slot, cacheKey, zSet);
                    //
                    List<ZSetTuple> list = zSet.zrangeByLex(minLex, maxLex, limit);

                    KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());

                    return ZSetTupleUtils.toReply(list, false);
                }
            }
        }

        if (encodeVersion == EncodeVersion.version_0) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            List<ZSetTuple> tuples = zrangeByLexVersion0(slot, keyMeta, key, minLex, maxLex, limit, false);
            return ZSetTupleUtils.toReply(tuples, false);
        }

        if (encodeVersion == EncodeVersion.version_1) {
            RedisZSet zSet = loadLRUCache(slot, keyMeta, key);
            if (zSet != null) {
                if (cacheConfig.isZSetLocalCacheEnable()) {
                    //
                    ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();
                    zSetLRUCache.putZSetForRead(slot, cacheKey, zSet);
                    //
                }

                List<ZSetTuple> list = zSet.zrangeByLex(minLex, maxLex, limit);

                KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());

                return ZSetTupleUtils.toReply(list, false);
            }
        }

        return ErrorReply.INTERNAL_ERROR;
    }

}
