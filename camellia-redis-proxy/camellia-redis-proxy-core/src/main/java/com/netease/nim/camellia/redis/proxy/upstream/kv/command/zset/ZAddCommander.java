package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.NoOpResult;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSetIndexLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.Index;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ZADD key score member [score member   ...]
 * <p>
 * Created by caojiajun on 2024/4/11
 */
public class ZAddCommander extends ZSet0Commander {

    public ZAddCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZADD;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length >= 4 && (objects.length % 2 == 0);
    }

    @Override
    protected Reply execute(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        Map<BytesKey, Double> memberMap = new HashMap<>();
        for (int i=2; i<objects.length; i+=2) {
            byte[] score = objects[i];
            byte[] member = objects[i+1];
            memberMap.put(new BytesKey(member), Utils.bytesToDouble(score));
        }
        int memberSize = memberMap.size();

        boolean first = false;
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(slot, key);
        EncodeVersion encodeVersion;
        if (keyMeta == null) {
            encodeVersion = keyDesign.zsetEncodeVersion();
            if (encodeVersion == EncodeVersion.version_0) {
                byte[] extra = BytesUtils.toBytes(memberSize);
                keyMeta = new KeyMeta(encodeVersion, KeyType.zset, System.currentTimeMillis(), -1, extra);
            } else if (encodeVersion == EncodeVersion.version_1) {
                keyMeta = new KeyMeta(encodeVersion, KeyType.zset, System.currentTimeMillis(), -1);
            } else {
                return ErrorReply.INTERNAL_ERROR;
            }
            keyMetaServer.createOrUpdateKeyMeta(slot, key, keyMeta);
            first = true;
        } else {
            if (keyMeta.getKeyType() != KeyType.zset) {
                return ErrorReply.WRONG_TYPE;
            }
            encodeVersion = keyMeta.getEncodeVersion();
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        RedisZSet zSet = null;
        Map<BytesKey, Double> existsMap = null;

        KvCacheMonitor.Type type = null;

        Result result = null;

        if (first) {
            zSet = new RedisZSet(new HashMap<>(memberMap));
            result = zsetWriteBuffer.put(cacheKey, zSet);
            //
            if (result != NoOpResult.INSTANCE) {
                type = KvCacheMonitor.Type.write_buffer;
                KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            }
        } else {
            WriteBufferValue<RedisZSet> bufferValue = zsetWriteBuffer.get(cacheKey);
            if (bufferValue != null) {
                zSet = bufferValue.getValue();
                existsMap = zSet.zadd(memberMap);
                result = zsetWriteBuffer.put(cacheKey, zSet);
                //
                type = KvCacheMonitor.Type.write_buffer;
                KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            }
        }

        if (cacheConfig.isZSetLocalCacheEnable()) {
            ZSetLRUCache zSetLRUCache = cacheConfig.getZSetLRUCache();

            if (first) {
                zSet = new RedisZSet(new HashMap<>(memberMap));
                zSetLRUCache.putZSetForWrite(slot, cacheKey, zSet);
            } else {
                Map<BytesKey, Double> map = zSetLRUCache.zadd(slot, cacheKey, memberMap);
                if (map == null) {
                    boolean hotKey = zSetLRUCache.isHotKey(key);
                    if (hotKey) {
                        zSet = loadLRUCache(slot, keyMeta, key);
                        if (zSet != null) {
                            //
                            type = KvCacheMonitor.Type.kv_store;
                            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                            //
                            zSetLRUCache.putZSetForWrite(slot, cacheKey, zSet);
                            //
                            map = zSet.zadd(memberMap);
                        }
                    }
                } else {
                    type = KvCacheMonitor.Type.local_cache;
                    KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                }
                if (existsMap == null && map != null) {
                    existsMap = map;
                }
            }
            if (result == null) {
                if (zSet == null) {
                    zSet = zSetLRUCache.getForWrite(slot, cacheKey);
                }
                if (zSet != null) {
                    result = zsetWriteBuffer.put(cacheKey, zSet.duplicate());
                }
            }
        }

        if (result == null) {
            result = NoOpResult.INSTANCE;
        }

        if (encodeVersion == EncodeVersion.version_0) {
            return zaddVersion0(slot, keyMeta, key, cacheKey, first, memberSize, memberMap, existsMap, result, type);
        }
        if (encodeVersion == EncodeVersion.version_1) {
            return zaddVersion1(slot, keyMeta, key, cacheKey, memberSize, memberMap, result, type);
        }
        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zaddVersion0(int slot, KeyMeta keyMeta, byte[] key, byte[] cacheKey, boolean first, int memberSize,
                               Map<BytesKey, Double> memberMap, Map<BytesKey, Double> existsMap, Result result, KvCacheMonitor.Type type) {
        if (type == null) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        }
        if (first) {
            List<KeyValue> list = new ArrayList<>(memberSize*2);
            for (Map.Entry<BytesKey, Double> entry : memberMap.entrySet()) {
                byte[] member = entry.getKey().getKey();
                Double score = entry.getValue();
                byte[] subKey1 = keyDesign.zsetMemberSubKey1(keyMeta, key, member);
                byte[] subKey2 = keyDesign.zsetMemberSubKey2(keyMeta, key, member, BytesUtils.toBytes(score));
                KeyValue keyValue1 = new KeyValue(subKey1, Utils.doubleToBytes(score));
                KeyValue keyValue2 = new KeyValue(subKey2, new byte[0]);
                list.add(keyValue1);
                list.add(keyValue2);
            }
            if (result.isKvWriteDelayEnable()) {
                submitAsyncWriteTask(slot, result, () -> kvClient.batchPut(slot, list));
            } else {
                kvClient.batchPut(slot, list);
            }
            return IntegerReply.parse(memberSize);
        } else {
            byte[][] existsKeys = new byte[memberSize][];
            int j = 0;
            List<KeyValue> list = new ArrayList<>(memberSize * 2);
            for (Map.Entry<BytesKey, Double> entry : memberMap.entrySet()) {
                byte[] member = entry.getKey().getKey();
                Double score = entry.getValue();
                byte[] subKey1 = keyDesign.zsetMemberSubKey1(keyMeta, key, member);
                byte[] subKey2 = keyDesign.zsetMemberSubKey2(keyMeta, key, member, BytesUtils.toBytes(score));
                KeyValue keyValue1 = new KeyValue(subKey1, Utils.doubleToBytes(score));
                KeyValue keyValue2 = new KeyValue(subKey2, new byte[0]);
                list.add(keyValue1);
                list.add(keyValue2);
                existsKeys[j] = subKey1;
                j++;
            }
            int existsCount = 0;
            List<byte[]> toDeleteSubKey2;
            if (existsMap == null) {
                List<KeyValue> keyValues = kvClient.batchGet(slot, existsKeys);
                toDeleteSubKey2 = new ArrayList<>(memberSize);
                for (KeyValue keyValue : keyValues) {
                    if (keyValue == null) {
                        continue;
                    }
                    if (keyValue.getValue() == null) {
                        continue;
                    }
                    byte[] subKey1 = keyValue.getKey();
                    byte[] member = keyDesign.decodeZSetMemberBySubKey1(subKey1, key);
                    byte[] subKey2 = keyDesign.zsetMemberSubKey2(keyMeta, key, member, keyValue.getValue());
                    toDeleteSubKey2.add(subKey2);
                    existsCount++;
                }
            } else {
                existsCount = existsMap.size();
                toDeleteSubKey2 = new ArrayList<>(existsCount);
                for (Map.Entry<BytesKey, Double> entry : existsMap.entrySet()) {
                    byte[] subKey2 = keyDesign.zsetMemberSubKey2(keyMeta, key, entry.getKey().getKey(), BytesUtils.toBytes(entry.getValue()));
                    toDeleteSubKey2.add(subKey2);
                }
            }
            if (result.isKvWriteDelayEnable()) {
                submitAsyncWriteTask(slot, result, () -> {
                    if (!toDeleteSubKey2.isEmpty()) {
                        kvClient.batchDelete(slot, toDeleteSubKey2.toArray(new byte[0][0]));
                    }
                    kvClient.batchPut(slot, list);
                });
            } else {
                if (!toDeleteSubKey2.isEmpty()) {
                    kvClient.batchDelete(slot, toDeleteSubKey2.toArray(new byte[0][0]));
                }
                kvClient.batchPut(slot, list);
            }
            int add = memberSize - existsCount;
            updateKeyMeta(slot, keyMeta, key, add);
            return IntegerReply.parse(add);
        }
    }

    private Reply zaddVersion1(int slot, KeyMeta keyMeta, byte[] key, byte[] cacheKey, int memberSize, Map<BytesKey, Double> memberMap, Result result, KvCacheMonitor.Type type) {
        if (type == null) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        }
        byte[][] rewriteCmd = new byte[memberSize*2+2][];
        rewriteCmd[0] = RedisCommand.ZADD.raw();
        rewriteCmd[1] = cacheKey;
        int i=2;
        List<KeyValue> list = new ArrayList<>(memberSize);
        List<Command> memberIndexCacheWriteCommands = new ArrayList<>();
        for (Map.Entry<BytesKey, Double> entry : memberMap.entrySet()) {
            byte[] member = entry.getKey().getKey();
            Index index = Index.fromRaw(member);
            if (index.isIndex()) {
                byte[] indexSubKey = keyDesign.zsetIndexSubKey(keyMeta, key, index);
                list.add(new KeyValue(indexSubKey, member));
                byte[] zsetMemberIndexCacheKey = keyDesign.zsetMemberIndexCacheKey(keyMeta, key, index);
                memberIndexCacheWriteCommands.add(new Command(new byte[][]{RedisCommand.PSETEX.raw(), zsetMemberIndexCacheKey, zsetMemberCacheMillis(), member}));
                //
                if (cacheConfig.isZSetLocalCacheEnable()) {
                    ZSetIndexLRUCache lruCache = cacheConfig.getZSetIndexLRUCache();
                    lruCache.putForWrite(slot, cacheKey, new BytesKey(index.getRef()), member);
                }
            }
            rewriteCmd[i] = Utils.doubleToBytes(entry.getValue());
            rewriteCmd[i+1] = index.getRef();
            i+=2;
        }
        if (result.isKvWriteDelayEnable()) {
            submitAsyncWriteTask(slot, result, () -> kvClient.batchPut(slot, list));
        } else {
            kvClient.batchPut(slot, list);
        }
        if (!memberIndexCacheWriteCommands.isEmpty()) {
            List<Reply> replyList = sync(cacheRedisTemplate.sendCommand(memberIndexCacheWriteCommands));
            for (Reply reply : replyList) {
                if (reply instanceof ErrorReply) {
                    return reply;
                }
            }
        }
        return sync(storageRedisTemplate.sendCommand(new Command(rewriteCmd)));
    }

    private void updateKeyMeta(int slot, KeyMeta keyMeta, byte[] key, int add) {
        if (add > 0) {
            int count = BytesUtils.toInt(keyMeta.getExtra()) + add;
            byte[] extra = BytesUtils.toBytes(count);
            keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
            keyMetaServer.createOrUpdateKeyMeta(slot, key, keyMeta);
        }
    }

}
