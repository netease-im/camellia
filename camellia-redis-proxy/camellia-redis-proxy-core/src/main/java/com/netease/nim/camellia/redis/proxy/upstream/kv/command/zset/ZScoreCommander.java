package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.Index;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;

/**
 * ZSCORE key member
 * <p>
 * Created by caojiajun on 2024/5/15
 */
public class ZScoreCommander extends ZSet0Commander {

    private static final byte[] script = ("local arg = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(arg) == 1 then\n" +
            "\tlocal ret = redis.call('zscore', KEYS[1], ARG[1]);\n" +
            "\tredis.call('pexpire', KEYS[1], ARGV[2]);\n" +
            "\treturn {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

    public ZScoreCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZSCORE;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 3;
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

        byte[] member = objects[2];

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisZSet> bufferValue = zsetWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisZSet zSet = bufferValue.getValue();
            Double zscore = zSet.zscore(new BytesKey(member));
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            if (zscore == null) {
                return BulkReply.NIL_REPLY;
            } else {
                return new BulkReply(Utils.doubleToBytes(zscore));
            }
        }

        if (cacheConfig.isZSetLocalCacheEnable()) {
            ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();
            RedisZSet zSet = zSetLRUCache.getForRead(key, cacheKey);

            if (zSet != null) {
                Double zscore = zSet.zscore(new BytesKey(member));
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                if (zscore == null) {
                    return BulkReply.NIL_REPLY;
                } else {
                    return new BulkReply(Utils.doubleToBytes(zscore));
                }
            }

            boolean hotKey = zSetLRUCache.isHotKey(key);

            if (hotKey) {
                zSet = loadLRUCache(keyMeta, key);
                if (zSet != null) {
                    //
                    KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                    //
                    zSetLRUCache.putZSetForRead(key, cacheKey, zSet);
                    Double zscore = zSet.zscore(new BytesKey(member));
                    if (zscore == null) {
                        return BulkReply.NIL_REPLY;
                    } else {
                        return new BulkReply(Utils.doubleToBytes(zscore));
                    }
                }
            }
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            return zscoreFromKv(keyMeta, key, member);
        }

        if (encodeVersion == EncodeVersion.version_1) {
            Reply reply = checkCache(script, cacheKey, new byte[][]{member, zsetRangeCacheMillis()});
            if (reply != null) {
                KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return reply;
            }
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            return zscoreFromKv(keyMeta, key, member);
        }

        if (encodeVersion == EncodeVersion.version_2) {
            Index index = Index.fromRaw(member);
            Reply reply = checkCache(script, cacheKey, new byte[][]{index.getRef(), zsetRangeCacheMillis()});
            if (reply != null) {
                KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return reply;
            }
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            return zscoreFromKv(keyMeta, key, member);
        }

        if (encodeVersion == EncodeVersion.version_3) {
            Index index = Index.fromRaw(member);
            KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
            return sync(storeRedisTemplate.sendCommand(new Command(new byte[][]{RedisCommand.ZSCORE.raw(), cacheKey, index.getRef()})));
        }

        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zscoreFromKv(KeyMeta keyMeta, byte[] key, byte[] member) {
        byte[] subKey1 = keyDesign.zsetMemberSubKey1(keyMeta, key, member);
        KeyValue keyValue = kvClient.get(subKey1);
        if (keyValue == null || keyValue.getValue() == null) {
            return BulkReply.NIL_REPLY;
        }
        return new BulkReply(keyValue.getValue());
    }

    protected final byte[] zsetRangeCacheMillis() {
        return Utils.stringToBytes(String.valueOf(cacheConfig.zsetRangeCacheMillis()));
    }
}
