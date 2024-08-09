package com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
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
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 *
 * HSET key field value [field value ...]
 * <p>
 * Created by caojiajun on 2024/4/7
 */
public class HSetCommander extends Hash0Commander {

    private static final byte[] script = ("local ret1 = '1';\n" +
            "local ret2 = '1';\n" +
            "local ret3 = '1';\n" +
            "local arg1 = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(arg1) == 1 then\n" +
            "\tret1 = '2';\n" +
            "\tret2 = redis.call('hset', KEYS[1], ARGV[1], ARGV[2]);\n" +
            "end\n" +
            "local arg2 = redis.call('pttl', KEYS[2]);\n" +
            "if tonumber(arg2) > 0 then\n" +
            "\tret3 = '2';\n" +
            "\tredis.call('psetex', KEYS[2], arg2, ARGV[2]);\n" +
            "end\n" +
            "return {ret1, ret2, ret3};").getBytes(StandardCharsets.UTF_8);
    private static final byte[] two = "2".getBytes(StandardCharsets.UTF_8);

    public HSetCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HSET;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        if (objects.length < 4) {
            return false;
        }
        return objects.length % 2 == 0;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];

        Map<BytesKey, byte[]> fieldMap = new HashMap<>();
        for (int i=2; i<objects.length; i+=2) {
            byte[] field = objects[i];
            byte[] value = objects[i + 1];
            fieldMap.put(new BytesKey(field), value);
        }
        int fieldSize = fieldMap.size();

        boolean first = false;

        //check meta
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            EncodeVersion encodeVersion = keyDesign.hashKeyMetaVersion();
            if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_2) {
                int count = fieldMap.size();
                byte[] extra = BytesUtils.toBytes(count);
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

        KvCacheMonitor.Type type = null;

        int existsCount = -1;
        Map<BytesKey, byte[]> existsMap = null;

        Result result = null;
        if (first) {
            existsCount = 0;
            result = hashWriteBuffer.put(cacheKey, new RedisHash(new HashMap<>(fieldMap)));
            //
            if (result != NoOpResult.INSTANCE) {
                KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
                type = KvCacheMonitor.Type.write_buffer;
            }
        } else {
            WriteBufferValue<RedisHash> value = hashWriteBuffer.get(cacheKey);
            if (value != null) {
                RedisHash hash = value.getValue();
                existsMap = hash.hset(fieldMap);
                existsCount = existsMap.size();
                result = hashWriteBuffer.put(cacheKey, hash);
                KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
                type = KvCacheMonitor.Type.write_buffer;
            }
        }

        if (cacheConfig.isHashLocalCacheEnable()) {
            HashLRUCache hashLRUCache = cacheConfig.getHashLRUCache();

            Map<BytesKey, byte[]> existMapByCache = null;
            if (first) {
                hashLRUCache.putAllForWrite(key, cacheKey, new RedisHash(new HashMap<>(fieldMap)));
            } else {
                existMapByCache = hashLRUCache.hset(key, cacheKey, fieldMap);
                if (existMapByCache == null) {
                    boolean hotKey = hashLRUCache.isHotKey(key);
                    if (hotKey) {
                        Map<BytesKey, byte[]> hgetall = hgetallFromKv(keyMeta, key);
                        hashLRUCache.putAllForWrite(key, cacheKey, new RedisHash(hgetall));
                        existMapByCache = hashLRUCache.hset(key, cacheKey, fieldMap);
                    }
                } else {
                    KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                    type = KvCacheMonitor.Type.local_cache;
                }
            }

            if (existsMap == null && existsCount < 0 && existMapByCache != null) {
                existsCount = existMapByCache.size();
                existsMap = existMapByCache;
            }

            if (result == null) {
                RedisHash hash = hashLRUCache.getForWrite(key, cacheKey);
                if (hash != null) {
                    result = hashWriteBuffer.put(cacheKey, hash.duplicate());
                }
            }
        }

        if (result == null) {
            result = NoOpResult.INSTANCE;
        }

        if (!first && existsMap != null && !existsMap.isEmpty()) {
            for (Map.Entry<BytesKey, byte[]> entry : existsMap.entrySet()) {
                byte[] value = fieldMap.get(entry.getKey());
                if (Arrays.equals(value, entry.getValue())) {
                    fieldMap.remove(entry.getKey());
                }
            }
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        if (first || encodeVersion == EncodeVersion.version_1) {
            if (type == null) {
                KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            }
            List<KeyValue> list = new ArrayList<>();
            for (Map.Entry<BytesKey, byte[]> entry : fieldMap.entrySet()) {
                byte[] field = entry.getKey().getKey();
                byte[] value = entry.getValue();
                byte[] subKey = keyDesign.hashFieldSubKey(keyMeta, key, field);
                KeyValue keyValue = new KeyValue(subKey, value);
                list.add(keyValue);
            }
            batchPut(cacheKey, result, list);
            return IntegerReply.parse(fieldSize);
        }

        if (encodeVersion == EncodeVersion.version_0) {
            byte[][] subKeys = new byte[fieldMap.size()][];
            List<KeyValue> list = new ArrayList<>();
            int i=0;
            for (Map.Entry<BytesKey, byte[]> entry : fieldMap.entrySet()) {
                byte[] field = entry.getKey().getKey();
                byte[] value = entry.getValue();
                byte[] subKey = keyDesign.hashFieldSubKey(keyMeta, key, field);
                KeyValue keyValue = new KeyValue(subKey, value);
                list.add(keyValue);
                subKeys[i] = subKey;
                i++;
            }
            if (existsCount < 0) {
                KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                boolean[] exists = kvClient.exists(subKeys);
                existsCount = Utils.count(exists);
            }
            int add = fieldSize - existsCount;
            if (add > 0) {
                int size = BytesUtils.toInt(keyMeta.getExtra());
                size = size + add;
                keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(),
                        keyMeta.getKeyVersion(), keyMeta.getExpireTime(), BytesUtils.toBytes(size));
                keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
            }
            batchPut(cacheKey, result, list);
            return IntegerReply.parse(add);
        }

        //param parse
        List<Command> commands = new ArrayList<>(fieldMap.size());
        List<KeyValue> list = new ArrayList<>(fieldMap.size());
        List<byte[]> fieldList = new ArrayList<>(fieldMap.size());
        for (Map.Entry<BytesKey, byte[]> entry : fieldMap.entrySet()) {
            byte[] field = entry.getKey().getKey();
            fieldList.add(field);
            byte[] value = entry.getValue();
            byte[] subKey = keyDesign.hashFieldSubKey(keyMeta, key, field);//store-key
            KeyValue keyValue = new KeyValue(subKey, value);
            list.add(keyValue);
            byte[] hashFieldCacheKey = keyDesign.hashFieldCacheKey(keyMeta, key, field);//cache-key
            Command cmd = new Command(new byte[][]{RedisCommand.EVAL.raw(), script, two, cacheKey, hashFieldCacheKey, field, value});
            commands.add(cmd);
        }

        int existsFields = 0;
        List<byte[]> unknownFields = new ArrayList<>();
        //cache
        List<Reply> replyList = sync(cacheRedisTemplate.sendCommand(commands));
        int index = 0;
        for (Reply reply : replyList) {
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                if (replies[0] instanceof BulkReply) {
                    byte[] raw = ((BulkReply) replies[0]).getRaw();
                    if (Utils.bytesToString(raw).equalsIgnoreCase("2")) {
                        if (replies[1] instanceof IntegerReply) {
                            Long integer = ((IntegerReply) replies[1]).getInteger();
                            if (integer == 0) {
                                existsFields++;
                                index++;
                                continue;
                            } else if (integer == 1) {
                                index++;
                                continue;
                            }
                        }
                    }
                }
                if (replies[2] instanceof BulkReply) {
                    byte[] raw = ((BulkReply) replies[2]).getRaw();
                    if (Utils.bytesToString(raw).equalsIgnoreCase("2")) {
                        existsFields++;
                        index++;
                        continue;
                    }
                }
            }
            unknownFields.add(fieldList.get(index));
            index++;
        }

        if (type == null && unknownFields.isEmpty()) {
            type = KvCacheMonitor.Type.redis_cache;
            KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
        }

        if (keyMeta.getEncodeVersion() == EncodeVersion.version_2) {
            if (existsCount < 0) {
                existsCount = existsFields;
                if (!unknownFields.isEmpty()) {
                    byte[][] subKeys = new byte[unknownFields.size()][];
                    for (int i = 0; i < unknownFields.size(); i++) {
                        byte[] field = unknownFields.get(i);
                        subKeys[i] = keyDesign.hashFieldSubKey(keyMeta, key, field);
                    }
                    boolean[] exists = kvClient.exists(subKeys);
                    existsCount += Utils.count(exists);

                    type = KvCacheMonitor.Type.kv_store;
                    KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                }
            }
            int add = fieldSize - existsCount;
            if (add > 0) {
                int size = BytesUtils.toInt(keyMeta.getExtra());
                size = size + add;
                keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(),
                        keyMeta.getKeyVersion(), keyMeta.getExpireTime(), BytesUtils.toBytes(size));
                keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
            }

            batchPut(cacheKey, result, list);

            for (Reply reply : replyList) {
                if (reply instanceof ErrorReply) {
                    return reply;
                }
            }

            if (type == null) {
                KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            }

            return IntegerReply.parse(add);
        } else if (keyMeta.getEncodeVersion() == EncodeVersion.version_3) {

            batchPut(cacheKey, result, list);

            for (Reply reply : replyList) {
                if (reply instanceof ErrorReply) {
                    return reply;
                }
            }

            if (type == null) {
                KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            }

            return IntegerReply.parse(fieldSize);//可能不准
        } else {
            return ErrorReply.INTERNAL_ERROR;
        }
    }

    private void batchPut(byte[] cacheKey, Result result, List<KeyValue> list) {
        if (!result.isKvWriteDelayEnable()) {
            kvClient.batchPut(list);
        } else {
            submitAsyncWriteTask(cacheKey, result, () -> kvClient.batchPut(list));
        }
    }

}
