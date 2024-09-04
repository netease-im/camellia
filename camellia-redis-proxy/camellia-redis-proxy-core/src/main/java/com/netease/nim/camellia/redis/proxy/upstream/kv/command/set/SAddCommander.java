package com.netease.nim.camellia.redis.proxy.upstream.kv.command.set;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.NoOpResult;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.SetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.*;

/**
 * SADD key member [member ...]
 * <p>
 * Created by caojiajun on 2024/8/5
 */
public class SAddCommander extends Set0Commander {

    public SAddCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.SADD;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length >= 3;
    }

    @Override
    protected Reply execute(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];

        Set<BytesKey> memberSet = new HashSet<>();
        for (int i=2; i<objects.length; i++) {
            byte[] member = objects[i];
            memberSet.add(new BytesKey(member));
        }

        int memberSize = memberSet.size();

        boolean first = false;

        //check meta
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(slot, key);
        if (keyMeta == null) {
            EncodeVersion encodeVersion = keyDesign.setEncodeVersion();
            if (encodeVersion == EncodeVersion.version_0) {
                int count = memberSet.size();
                byte[] extra = BytesUtils.toBytes(count);
                keyMeta = new KeyMeta(encodeVersion, KeyType.set, System.currentTimeMillis(), -1, extra);
            } else if (encodeVersion == EncodeVersion.version_1) {
                keyMeta = new KeyMeta(encodeVersion, KeyType.set, System.currentTimeMillis(), -1);
            } else {
                return ErrorReply.INTERNAL_ERROR;
            }
            keyMetaServer.createOrUpdateKeyMeta(slot, key, keyMeta);
            first = true;
        } else {
            if (keyMeta.getKeyType() != KeyType.set) {
                return ErrorReply.WRONG_TYPE;
            }
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        Result result = null;
        RedisSet set = null;
        Set<BytesKey> existsMemberSet = null;

        KvCacheMonitor.Type type = null;

        if (first) {
            set = new RedisSet(new HashSet<>(memberSet));
            result = setWriteBuffer.put(cacheKey, set);
            //
            if (result != NoOpResult.INSTANCE) {
                type = KvCacheMonitor.Type.write_buffer;
                KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            }
        } else {
            WriteBufferValue<RedisSet> bufferValue = setWriteBuffer.get(cacheKey);
            if (bufferValue != null) {
                set = bufferValue.getValue();
                existsMemberSet = set.sadd(memberSet);
                result = setWriteBuffer.put(cacheKey, set);
                //
                type = KvCacheMonitor.Type.write_buffer;
                KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            }
        }

        if (cacheConfig.isSetLocalCacheEnable()) {
            SetLRUCache setLRUCache = cacheConfig.getSetLRUCache();

            if (first) {
                set = new RedisSet(new HashSet<>(memberSet));
                setLRUCache.putAllForWrite(slot, cacheKey, set);
            } else {
                Set<BytesKey> existsSet = setLRUCache.sadd(slot, cacheKey, memberSet);
                if (existsSet == null) {
                    boolean hotKey = setLRUCache.isHotKey(key);
                    if (hotKey) {
                        //
                        type = KvCacheMonitor.Type.kv_store;
                        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                        //
                        set = loadLRUCache(slot, keyMeta, key);
                        //
                        setLRUCache.putAllForWrite(slot, cacheKey, set);
                        //
                        existsSet = set.sadd(memberSet);
                    }
                } else {
                    type = KvCacheMonitor.Type.local_cache;
                    KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                }
                if (existsMemberSet == null && existsSet != null) {
                    existsMemberSet = existsSet;
                }
            }

            if (result == null) {
                if (set == null) {
                    set = setLRUCache.getForWrite(slot, cacheKey);
                }
                if (set != null) {
                    result = setWriteBuffer.put(cacheKey, set.duplicate());
                }
            }
        }

        if (result == null) {
            result = NoOpResult.INSTANCE;
        }

        int existsMemberSize = -1;
        if (existsMemberSet != null) {
            existsMemberSize = existsMemberSet.size();
        }

        if (existsMemberSet != null && !existsMemberSet.isEmpty()) {
            memberSet.removeAll(existsMemberSet);
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        if (encodeVersion == EncodeVersion.version_0) {
            return saddVersion0(slot, keyMeta, key, cacheKey, first, memberSize, existsMemberSize, memberSet, result, type);
        }

        if (encodeVersion == EncodeVersion.version_1) {
            return saddVersion1(slot, keyMeta, key, cacheKey, first, memberSize, existsMemberSize, memberSet, result, type);
        }

        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply saddVersion0(int slot, KeyMeta keyMeta, byte[] key, byte[] cacheKey, boolean first, int memberSize,
                               int existsMemberSize, Set<BytesKey> memberSet, Result result, KvCacheMonitor.Type type) {
        if (type == null) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        }
        if (first) {
            writeMembers(slot, keyMeta, key, cacheKey, memberSet, result);
            return IntegerReply.parse(memberSize);
        }

        if (existsMemberSize < 0) {
            existsMemberSize = 0;
            byte[][] subKeyArray = new byte[memberSize][];
            int i=0;
            for (BytesKey bytesKey : memberSet) {
                byte[] member = bytesKey.getKey();
                byte[] subKey = keyDesign.setMemberSubKey(keyMeta, key, member);
                subKeyArray[i] = subKey;
                i++;
            }
            List<KeyValue> keyValues = kvClient.batchGet(slot, subKeyArray);
            for (KeyValue keyValue : keyValues) {
                if (keyValue == null || keyValue.getKey() == null) {
                    continue;
                }
                existsMemberSize ++;
                memberSet.remove(new BytesKey(keyValue.getKey()));
            }
        }

        int add = memberSize - existsMemberSize;

        updateKeyMeta(slot, keyMeta, key, add);

        if (add <= 0) {
            writeMembers(slot, keyMeta, key, cacheKey, Collections.emptySet(), result);
        } else {
            writeMembers(slot, keyMeta, key, cacheKey, memberSet, result);
        }

        return IntegerReply.parse(memberSize - existsMemberSize);
    }

    private Reply saddVersion1(int slot, KeyMeta keyMeta, byte[] key, byte[] cacheKey, boolean first, int memberSize,
                               int existsMemberSize, Set<BytesKey> memberSet, Result result, KvCacheMonitor.Type type) {
        if (type == null) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        }
        if (first) {
            writeMembers(slot, keyMeta, key, cacheKey, memberSet, result);
        } else {
            if (memberSize == existsMemberSize) {
                writeMembers(slot, keyMeta, key, cacheKey, Collections.emptySet(), result);
            } else {
                writeMembers(slot, keyMeta, key, cacheKey, memberSet, result);
            }
        }
        return IntegerReply.parse(memberSize);
    }

}
