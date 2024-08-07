package com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.HashLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisHash;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HVALS key
 * <p>
 * Created by caojiajun on 2024/5/15
 */
public class HValsCommander extends Hash0Commander {

    private static final byte[] script = ("local arg = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(arg) == 1 then\n" +
            "\tlocal ret = redis.call('hvals', KEYS[1]);\n" +
            "\tredis.call('pexpire', KEYS[1], ARGV[1]);\n" +
            "\treturn {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

    public HValsCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HVALS;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 2;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            return MultiBulkReply.EMPTY;
        }
        if (keyMeta.getKeyType() != KeyType.hash) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisHash> writeBufferValue = hashWriteBuffer.get(cacheKey);
        if (writeBufferValue != null) {
            RedisHash hash = writeBufferValue.getValue();
            if (hash != null) {
                KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
                return toReply(writeBufferValue.getValue().hgetAll());
            }
        }

        if (cacheConfig.isHashLocalCacheEnable()) {
            HashLRUCache hashLRUCache = cacheConfig.getHashLRUCache();
            RedisHash hash = hashLRUCache.getForRead(key, cacheKey);
            if (hash != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return toReply(hash.hgetAll());
            }
            boolean hotKey = hashLRUCache.isHotKey(key);
            if (hotKey) {
                hash = loadLRUCache(keyMeta, key);
                hashLRUCache.putAllForRead(key, cacheKey, hash);
                return toReply(hash.hgetAll());
            }
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_1) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            Map<BytesKey, byte[]> map = hgetallFromKv(keyMeta, key);
            if (cacheConfig.isHashLocalCacheEnable()) {
                cacheConfig.getHashLRUCache().putAllForRead(key, cacheKey, new RedisHash(map));
            }
            return toReply(map);
        }

        Reply reply = checkCache(script, cacheKey, new byte[][]{hgetallCacheMillis()});
        if (reply != null) {
            KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
            return reply;
        }

        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());

        Map<BytesKey, byte[]> map = hgetallFromKv(keyMeta, key);
        if (cacheConfig.isHashLocalCacheEnable()) {
            cacheConfig.getHashLRUCache().putAllForRead(key, cacheKey, new RedisHash(map));
        }

        ErrorReply errorReply = buildCache(cacheKey, map);
        if (errorReply != null) {
            return errorReply;
        }

        return toReply(map);
    }

    private MultiBulkReply toReply(Map<BytesKey, byte[]> map) {
        if (map.isEmpty()) {
            return MultiBulkReply.EMPTY;
        }
        Reply[] replies = new Reply[map.size()];
        int i = 0;
        for (Map.Entry<BytesKey, byte[]> entry : map.entrySet()) {
            replies[i] = new BulkReply(entry.getValue());
            i++;
        }
        return new MultiBulkReply(replies);
    }
}
