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
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.tools.utils.BytesKey;
import com.netease.nim.camellia.tools.utils.Pair;

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
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        Set<BytesKey> members = new HashSet<>();
        for (int i=2; i<objects.length; i++) {
            members.add(new BytesKey(objects[i]));
        }
        int size = members.size();
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
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

        WriteBufferValue<RedisSet> bufferValue = setWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisSet set = bufferValue.getValue();
            type = KvCacheMonitor.Type.write_buffer;
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            removedMembers = set.srem(members);
            result = setWriteBuffer.put(cacheKey, set);
        }

        if (cacheConfig.isSetLocalCacheEnable()) {
            SetLRUCache setLRUCache = cacheConfig.getSetLRUCache();

            if (removedMembers == null) {
                removedMembers = setLRUCache.srem(key, cacheKey, members);
                if (removedMembers != null) {
                    type = KvCacheMonitor.Type.local_cache;
                    KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                }
            } else {
                setLRUCache.srem(key, cacheKey, members);
            }

            if (removedMembers == null) {
                boolean hotKey = setLRUCache.isHotKey(key);
                if (hotKey) {
                    //
                    type = KvCacheMonitor.Type.kv_store;
                    KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                    //
                    RedisSet set = loadLRUCache(keyMeta, key);
                    setLRUCache.putAllForWrite(key, cacheKey, set);
                    removedMembers = set.srem(members);
                }
            }

            if (result == null) {
                RedisSet set = setLRUCache.getForWrite(key, cacheKey);
                if (set != null) {
                    result = setWriteBuffer.put(cacheKey, new RedisSet(new HashSet<>(set.smembers())));
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

        if (encodeVersion == EncodeVersion.version_2 || encodeVersion == EncodeVersion.version_3) {
            if (!members.isEmpty()) {
                Pair<Reply, Integer> pair = updateCache(cacheKey, members);
                if (pair != null) {
                    if (pair.getFirst() != null) {
                        return pair.getFirst();
                    }
                    if (pair.getSecond() != null && pair.getSecond() >= 0) {
                        if (type == null) {
                            type = KvCacheMonitor.Type.redis_cache;
                            KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                        }
                        removeSize = pair.getSecond();
                    }
                }
            }
        }

        if (type == null) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        }

        if (removeSize < 0) {
            if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_2) {
                Map<BytesKey, Boolean> smismember = smismemberFromKv(keyMeta, key, members);
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

        removeMembers(keyMeta, key, cacheKey, members, result);

        if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_2) {
            if (removeSize > 0) {
                updateKeyMeta(keyMeta, key, removeSize * -1);
            }
            return IntegerReply.parse(removeSize);
        } else {
            return IntegerReply.parse(size);
        }
    }

    private Pair<Reply, Integer> updateCache(byte[] cacheKey, Set<BytesKey> members) {
        byte[][] cmd = new byte[members.size() + 2][];
        cmd[0] = RedisCommand.SREM.raw();
        cmd[1] = cacheKey;
        int i = 2;
        for (BytesKey member : members) {
            cmd[i] = member.getKey();
            i++;
        }
        Reply reply = sync(cacheRedisTemplate.sendCommand(new Command(cmd)));
        if (reply instanceof ErrorReply) {
            return new Pair<>(reply, -1);
        }
        if (reply instanceof IntegerReply) {
            Long integer = ((IntegerReply) reply).getInteger();
            if (integer > 0) {
                int removeSize = integer.intValue();
                return new Pair<>(null, removeSize);
            }
        }
        return null;
    }
}
