package com.netease.nim.camellia.redis.proxy.upstream.kv.command.set;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.NoOpResult;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.SetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.DeleteType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.*;

/**
 * SREM key member [member ...]
 * <p>
 * Created by caojiajun on 2024/8/5
 */
public class SRemCommander extends Set0Commander {

    public SRemCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.SREM;
    }

    @Override
    protected boolean parse(Command command) {
        return command.getObjects().length >= 3;
    }

    @Override
    protected Reply execute(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        Set<BytesKey> members = new HashSet<>();
        for (int i=2; i<objects.length; i++) {
            members.add(new BytesKey(objects[i]));
        }
        int size = members.size();
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(slot, key);
        if (keyMeta == null) {
            return IntegerReply.REPLY_0;
        }
        if (keyMeta.getKeyType() != KeyType.set) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        Set<BytesKey> removedMembers = null;
        Result result = null;
        KvCacheMonitor.Type type = null;
        DeleteType deleteType = DeleteType.unknown;

        WriteBufferValue<RedisSet> bufferValue = setWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisSet set = bufferValue.getValue();
            type = KvCacheMonitor.Type.write_buffer;
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            removedMembers = set.srem(members);
            result = setWriteBuffer.put(cacheKey, set);
            if (set.isEmpty()) {
                deleteType = DeleteType.delete_all;
            } else {
                deleteType = DeleteType.delete_someone;
            }
        }

        if (cacheConfig.isSetLocalCacheEnable()) {
            SetLRUCache setLRUCache = cacheConfig.getSetLRUCache();

            if (removedMembers == null) {
                removedMembers = setLRUCache.srem(slot, cacheKey, members);
                if (removedMembers != null) {
                    type = KvCacheMonitor.Type.local_cache;
                    KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                }
            } else {
                setLRUCache.srem(slot, cacheKey, members);
            }

            if (removedMembers == null) {
                boolean hotKey = setLRUCache.isHotKey(key);
                if (hotKey) {
                    //
                    type = KvCacheMonitor.Type.kv_store;
                    KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                    //
                    RedisSet set = loadLRUCache(slot, keyMeta, key);
                    setLRUCache.putAllForWrite(slot, cacheKey, set);
                    removedMembers = set.srem(members);
                }
            }

            if (result == null) {
                RedisSet set = setLRUCache.getForWrite(slot, cacheKey);
                if (set != null) {
                    result = setWriteBuffer.put(cacheKey, new RedisSet(new HashSet<>(set.smembers())));
                    if (set.isEmpty()) {
                        deleteType = DeleteType.delete_all;
                    } else {
                        deleteType = DeleteType.delete_someone;
                    }
                }
            }
        }

        if (result == null) {
            result = NoOpResult.INSTANCE;
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        int removeSize = -1;

        if (removedMembers != null) {
            removeSize = removedMembers.size();
            members = removedMembers;
        }

        if (type == null) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        }

        if (removeSize < 0) {
            if (encodeVersion == EncodeVersion.version_0) {
                Map<BytesKey, Boolean> smismember = smismemberFromKv(slot, keyMeta, key, members);
                removeSize = 0;
                members = new HashSet<>();
                for (Map.Entry<BytesKey, Boolean> entry : smismember.entrySet()) {
                    if (entry.getValue()) {
                        removeSize++;
                        members.add(entry.getKey());
                    }
                }
            }
        }

        if (encodeVersion == EncodeVersion.version_0) {
            removeMembers(slot, keyMeta, key, members, result, false);
        } else {
            boolean checkSCard = deleteType == DeleteType.unknown;
            removeMembers(slot, keyMeta, key, members, result, checkSCard);
        }

        if (deleteType == DeleteType.delete_all) {
            keyMetaServer.deleteKeyMeta(slot, key);
        } else {
            if (encodeVersion == EncodeVersion.version_0) {
                updateKeyMeta(slot, keyMeta, key, removeSize * -1);
            }
        }

        if (encodeVersion == EncodeVersion.version_0) {
            return IntegerReply.parse(removeSize);
        } else {
            return IntegerReply.parse(size);
        }
    }

}
