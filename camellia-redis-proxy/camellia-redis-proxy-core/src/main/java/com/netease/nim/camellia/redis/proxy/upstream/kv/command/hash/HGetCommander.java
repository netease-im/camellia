package com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.Hash;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.HashLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;

/**
 * HGET key field
 * <p>
 * Created by caojiajun on 2024/4/7
 */
public class HGetCommander extends Commander {

    public HGetCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    //get hget cache, if exists, delay ttl, return 1 and value
    //get hgetall cache, if exists, delay ttl, return 2 and value
    //return 3 and value
    private static final byte[] script = ("local ret1 = redis.call('get', KEYS[1]);\n" +
            "if ret1 then\n" +
            "\tredis.call('pexpire', KEYS[1], ARGV[2]);\n" +
            "\treturn {'1', ret1};\n" +
            "end\n" +
            "local arg1 = redis.call('exists', KEYS[2]);\n" +
            "if tonumber(arg1) == 1 then\n" +
            "\tlocal ret2 = redis.call('hget', KEYS[2], ARGV[1]);\n" +
            "\tredis.call('pexpire', KEYS[2], ARGV[3]);\n" +
            "\treturn {'2', ret2};\n" +
            "end\n" +
            "return {'3'};").getBytes(StandardCharsets.UTF_8);

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HGET;
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
            return BulkReply.NIL_REPLY;
        }
        if (keyMeta.getKeyType() != KeyType.hash) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<Hash> writeBufferValue = hashWriteBuffer.get(cacheKey);
        if (writeBufferValue != null) {
            byte[] bytes = writeBufferValue.getValue().hget(new BytesKey(field));
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            return new BulkReply(bytes);
        }

        if (cacheConfig.isHashLocalCacheEnable()) {
            HashLRUCache hashLRUCache = cacheConfig.getHashLRUCache();
            Hash hash = hashLRUCache.getForRead(cacheKey);
            if (hash != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return new BulkReply(hash.hget(new BytesKey(field)));
            }
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_1) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            byte[] subKey = keyDesign.hashFieldSubKey(keyMeta, key, field);
            KeyValue keyValue = kvClient.get(subKey);
            if (keyValue == null || keyValue.getValue() == null) {
                return BulkReply.NIL_REPLY;
            }
            return new BulkReply(keyValue.getValue());
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
            return BulkReply.NIL_REPLY;
        }

        //build hget cache
        Reply reply = sync(cacheRedisTemplate.sendPSetEx(hashFieldCacheKey, cacheConfig.hgetCacheMillis(), keyValue.getValue()));
        if (reply instanceof ErrorReply) {
            return reply;
        }
        return new BulkReply(keyValue.getValue());
    }

    private byte[] hgetallCacheMillis() {
        return Utils.stringToBytes(String.valueOf(cacheConfig.hgetallCacheMillis()));
    }

    private byte[] hgetCacheMillis() {
        return Utils.stringToBytes(String.valueOf(cacheConfig.hgetCacheMillis()));
    }
}
