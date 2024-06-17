package com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.Hash;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.HashLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;

/**
 * HSTRLEN key field
 * <p>
 * Created by caojiajun on 2024/6/6
 */
public class HStrLenCommander extends Hash0Commander {

    private static final byte[] script = ("local ret1 = redis.call('strlen', KEYS[1]);\n" +
            "if tonumber(ret1) > 0 then\n" +
            "\tredis.call('pexpire', KEYS[1], ARGV[2]);\n" +
            "\treturn {'1', ret1};\n" +
            "end\n" +
            "local arg1 = redis.call('exists', KEYS[2]);\n" +
            "if tonumber(arg1) == 1 then\n" +
            "\tlocal ret2 = redis.call('hstrlen', KEYS[2], ARGV[1]);\n" +
            "\tredis.call('pexpire', KEYS[2], ARGV[3]);\n" +
            "\treturn {'2', ret2};\n" +
            "end\n" +
            "return {'3'};").getBytes(StandardCharsets.UTF_8);



    public HStrLenCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HSTRLEN;
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
        byte[] field = objects[2];

        //meta
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            return IntegerReply.REPLY_0;
        }
        if (keyMeta.getKeyType() != KeyType.hash) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<Hash> writeBufferValue = hashWriteBuffer.get(cacheKey);
        if (writeBufferValue != null) {
            int hstrlen = writeBufferValue.getValue().hstrlen(new BytesKey(field));
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            return IntegerReply.parse(hstrlen);
        }

        if (cacheConfig.isHashLocalCacheEnable()) {
            HashLRUCache hashLRUCache = cacheConfig.getHashLRUCache();
            Hash hash = hashLRUCache.getForRead(key, cacheKey);
            if (hash != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return IntegerReply.parse(hash.hstrlen(new BytesKey(field)));
            }
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_1) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            byte[] subKey = keyDesign.hashFieldSubKey(keyMeta, key, field);
            KeyValue keyValue = kvClient.get(subKey);
            if (keyValue == null || keyValue.getValue() == null) {
                return IntegerReply.REPLY_0;
            }
            return IntegerReply.parse(Utils.bytesToString(keyValue.getValue()).length());
        }

        byte[] hashFieldCacheKey = keyDesign.hashFieldCacheKey(keyMeta, key, field);

        //cache
        {
            Reply reply = sync(cacheRedisTemplate.sendLua(script, new byte[][]{hashFieldCacheKey, cacheKey},
                    new byte[][]{field, hgetCacheMillis(), hgetallCacheMillis()}));
            if (reply instanceof ErrorReply) {
                return reply;
            }
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                String type = Utils.bytesToString(((BulkReply) replies[0]).getRaw());
                if (type.equalsIgnoreCase("1") || type.equalsIgnoreCase("2")) {
                    KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                    return replies[1];
                }
            }
        }

        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());

        //get from kv
        byte[] subKey = keyDesign.hashFieldSubKey(keyMeta, key, field);
        KeyValue keyValue = kvClient.get(subKey);

        if (keyValue == null || keyValue.getValue() == null) {
            return IntegerReply.REPLY_0;
        }

        //build hget cache
        Reply reply = sync(cacheRedisTemplate.sendPSetEx(hashFieldCacheKey, cacheConfig.hgetCacheMillis(), keyValue.getValue()));
        if (reply instanceof ErrorReply) {
            return reply;
        }

        return IntegerReply.parse(Utils.bytesToString(keyValue.getValue()).length());
    }

}
