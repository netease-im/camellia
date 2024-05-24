package com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HKEYS key
 * <p>
 * Created by caojiajun on 2024/5/15
 */
public class HKeysCommander extends Hash0Commander {

    private static final byte[] script = ("local arg = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(arg) == 1 then\n" +
            "\tlocal ret = redis.call('hkeys', KEYS[1]);\n" +
            "\tredis.call('pexpire', KEYS[1], ARGV[1]);\n" +
            "\treturn {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

    public HKeysCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HKEYS;
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

        WriteBufferValue<Map<BytesKey, byte[]>> writeBufferValue = hashWriteBuffer.get(cacheKey);
        if (writeBufferValue != null) {
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            return toReply(writeBufferValue.getValue());
        }

        if (cacheConfig.isHashLocalCacheEnable()) {
            Map<BytesKey, byte[]> map = cacheConfig.getHashLRUCache().hgetAll(cacheKey);
            if (map != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return toReply(map);
            }
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_1) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            Map<BytesKey, byte[]> map = hgetallFromKv(keyMeta, key);
            if (cacheConfig.isHashLocalCacheEnable()) {
                cacheConfig.getHashLRUCache().putAllForRead(cacheKey, map);
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
            cacheConfig.getHashLRUCache().putAllForRead(cacheKey, map);
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
            replies[i] = new BulkReply(entry.getKey().getKey());
            i++;
        }
        return new MultiBulkReply(replies);
    }
}
