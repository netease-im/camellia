package com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.NoOpResult;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisHash;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.HashLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HSETNX key field value
 * <p>
 * Created by caojiajun on 2024/6/6
 */
public class HSetNxCommander extends Hash0Commander {

    private static final byte[] script = ("local arg1 = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(arg1) == 1 then\n" +
            "\treturn 1;\n" +
            "end\n" +
            "local arg2 = redis.call('exists', KEYS[2]);\n" +
            "if tonumber(arg2) == 1 then\n" +
            "\tlocal arg3 = redis.call('hsetnx', KEYS[2], ARGV[1], ARGV[2]);\n" +
            "\tif tonumber(arg3) == 1 then\n" +
            "\t\treturn 2;\n" +
            "\tend\n" +
            "\tif tonumber(arg3) == 0 then\n" +
            "\t\treturn 3;\n" +
            "\tend\n" +
            "end\n" +
            "return 4;").getBytes(StandardCharsets.UTF_8);

    private static final int cache_miss = 0;
    private static final int cache_hit_exist = 1;
    private static final int cache_hit_not_exists = 2;

    public HSetNxCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HSETNX;
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
        byte[] field = objects[2];
        byte[] value = objects[3];

        boolean first = false;

        //check meta
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            EncodeVersion encodeVersion = keyDesign.hashKeyMetaVersion();
            if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_2) {
                byte[] extra = BytesUtils.toBytes(1);
                keyMeta = new KeyMeta(encodeVersion, KeyType.hash, System.currentTimeMillis(), -1, extra);
            } else if (encodeVersion == EncodeVersion.version_1 || encodeVersion == EncodeVersion.version_3) {
                keyMeta = new KeyMeta(encodeVersion, KeyType.hash, System.currentTimeMillis(), -1);
            } else {
                return ErrorReply.INTERNAL_ERROR;
            }
            keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
            first = true;
        } else {
            if (keyMeta.getKeyType() != KeyType.hash) {
                return ErrorReply.WRONG_TYPE;
            }
        }


        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);
        BytesKey filedKey = new BytesKey(field);

        if (first) {
            byte[] subKey = keyDesign.hashFieldSubKey(keyMeta, key, field);
            kvClient.put(subKey, value);
            return IntegerReply.REPLY_1;
        }

        int cacheCheck = cache_miss;

        Result result = null;
        WriteBufferValue<RedisHash> writeBufferValue = hashWriteBuffer.get(cacheKey);
        if (writeBufferValue != null) {
            RedisHash hash = writeBufferValue.getValue();
            byte[] hget = hash.hget(filedKey);
            if (hget != null) {
                KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
                return IntegerReply.REPLY_0;
            } else {
                cacheCheck = cache_hit_not_exists;
            }
            hash.hset(filedKey, value);
            result = hashWriteBuffer.put(cacheKey, hash);
        }

        if (cacheConfig.isHashLocalCacheEnable()) {
            HashLRUCache hashLRUCache = cacheConfig.getHashLRUCache();

            boolean hotKey = hashLRUCache.isHotKey(key);

            boolean loadFromKv = false;
            RedisHash hash = hashLRUCache.getForWrite(key, cacheKey);
            if (hash == null) {
                if (hotKey) {
                    Map<BytesKey, byte[]> map = hgetallFromKv(keyMeta, key);
                    loadFromKv = true;
                    hash = new RedisHash(map);
                    hashLRUCache.putAllForWrite(key, cacheKey, hash);
                }
            }
            if (hash != null) {
                byte[] hget = hash.hget(filedKey);
                if (hget != null) {
                    if (!loadFromKv) {
                        KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                    }
                    return IntegerReply.REPLY_0;
                } else {
                    hash.hset(filedKey, value);
                    cacheCheck = cache_hit_not_exists;
                }
            }
            if (result == null) {
                hash = hashLRUCache.getForWrite(key, cacheKey);
                if (hash != null) {
                    result = hashWriteBuffer.put(cacheKey, hash.duplicate());
                }
            }
        }

        if (result == null) {
            result = NoOpResult.INSTANCE;
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_1) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            byte[] subKey = keyDesign.hashFieldSubKey(keyMeta, key, field);

            if (cacheCheck == cache_miss) {
                KeyValue keyValue = kvClient.get(subKey);
                if (keyValue != null && keyValue.getValue() != null) {
                    return IntegerReply.REPLY_0;
                }
            }

            if (encodeVersion == EncodeVersion.version_0) {
                int size = BytesUtils.toInt(keyMeta.getExtra());
                keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(),
                        keyMeta.getKeyVersion(), keyMeta.getExpireTime(), BytesUtils.toBytes(size + 1));
                keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
            }
            put(cacheKey, result, new KeyValue(subKey, value));
            return IntegerReply.REPLY_1;
        }

        if (cacheCheck == cache_miss) {
            byte[] hashFieldCacheKey = keyDesign.hashFieldCacheKey(keyMeta, key, field);
            Reply reply = sync(cacheRedisTemplate.sendLua(script, new byte[][]{hashFieldCacheKey, cacheKey}, new byte[][]{field, value}));
            if (reply instanceof ErrorReply) {
                return reply;
            }
            if (reply instanceof IntegerReply) {
                Long integer = ((IntegerReply) reply).getInteger();
                if (integer == 1) {
                    cacheCheck = cache_hit_exist;
                } else if (integer == 2) {
                    cacheCheck = cache_hit_not_exists;
                } else if (integer == 3) {
                    cacheCheck = cache_hit_exist;
                }
            }
        }

        if (cacheCheck == cache_hit_exist) {
            KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
            return IntegerReply.REPLY_0;
        }

        byte[] subKey = keyDesign.hashFieldSubKey(keyMeta, key, field);

        if (cacheCheck == cache_miss) {
            KeyValue keyValue = kvClient.get(subKey);
            if (keyValue != null && keyValue.getValue() != null) {
                return IntegerReply.REPLY_0;
            }
        }

        if (encodeVersion == EncodeVersion.version_2) {
            int size = BytesUtils.toInt(keyMeta.getExtra());
            keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(),
                    keyMeta.getKeyVersion(), keyMeta.getExpireTime(), BytesUtils.toBytes(size + 1));
            keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
        }

        put(cacheKey, result, new KeyValue(subKey, value));

        return IntegerReply.REPLY_1;
    }

    private void put(byte[] cacheKey, Result result, KeyValue keyValue) {
        if (!result.isKvWriteDelayEnable()) {
            kvClient.put(keyValue.getKey(), keyValue.getValue());
        } else {
            submitAsyncWriteTask(cacheKey, result, () -> kvClient.put(keyValue.getKey(), keyValue.getValue()));
        }
    }
}
