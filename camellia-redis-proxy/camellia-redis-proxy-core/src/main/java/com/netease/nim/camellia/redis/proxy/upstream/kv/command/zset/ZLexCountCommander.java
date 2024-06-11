package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
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
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.ScriptReplyUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * ZLEXCOUNT key min max
 * <p>
 * Created by caojiajun on 2024/6/6
 */
public class ZLexCountCommander extends ZSet0Commander {

    private static final byte[] script = ("local ret1 = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(ret1) == 1 then\n" +
            "  local ret = redis.call('zlexcount', KEYS[1], ARGV[1], ARGV[2]);\n" +
            "  return {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

    public ZLexCountCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZLEXCOUNT;
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

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_3) {
            return ErrorReply.COMMAND_NOT_SUPPORT_IN_CURRENT_KV_ENCODE_VERSION;
        }

        ZSetLex minLex;
        ZSetLex maxLex;
        try {
            minLex = ZSetLex.fromLex(objects[2]);
            maxLex = ZSetLex.fromLex(objects[3]);
            if (minLex == null || maxLex == null) {
                return new ErrorReply("ERR min or max not valid string range item");
            }
        } catch (Exception e) {
            return ErrorReply.SYNTAX_ERROR;
        }
        if (minLex.isMax() || maxLex.isMin()) {
            return IntegerReply.REPLY_0;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<ZSet> bufferValue = zsetWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            ZSet zSet = bufferValue.getValue();
            int zcount = zSet.zlexcount(minLex, maxLex);
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            return IntegerReply.parse(zcount);
        }

        if (cacheConfig.isZSetLocalCacheEnable()) {
            ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();

            boolean hotKey = zSetLRUCache.isHotKey(key);

            ZSet zSet = zSetLRUCache.getForRead(cacheKey);

            if (zSet != null) {
                int zcount = zSet.zlexcount(minLex, maxLex);
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return IntegerReply.parse(zcount);
            }

            if (hotKey) {
                zSet = loadLRUCache(keyMeta, key);
                if (zSet != null) {
                    //
                    zSetLRUCache.putZSetForRead(cacheKey, zSet);
                    //
                    int zcount = zSet.zlexcount(minLex, maxLex);

                    KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());

                    return IntegerReply.parse(zcount);
                }
            }
        }

        if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_2) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            return IntegerReply.parse(zrangeByLexFromKv(keyMeta, key, minLex, maxLex));
        }

        if (encodeVersion == EncodeVersion.version_1) {
            Reply reply = sync(cacheRedisTemplate.sendLua(script, new byte[][]{cacheKey}, new byte[][]{objects[2], objects[3]}));
            reply = ScriptReplyUtils.check(reply);
            if (reply != null) {
                KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return reply;
            }
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            return IntegerReply.parse(zrangeByLexFromKv(keyMeta, key, minLex, maxLex));
        }

        return ErrorReply.INTERNAL_ERROR;
    }

    private int zrangeByLexFromKv(KeyMeta keyMeta, byte[] key, ZSetLex minLex, ZSetLex maxLex) {
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
        int scanBatch = kvConfig.scanBatch();
        int count = 0;
        while (true) {
            List<KeyValue> scan = kvClient.scanByStartEnd(startKey, endKey, scanBatch, Sort.ASC, !minLex.isExcludeLex());
            if (scan.isEmpty()) {
                break;
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
                count++;
            }
            if (scan.size() < scanBatch) {
                break;
            }
        }
        return count;
    }
}
