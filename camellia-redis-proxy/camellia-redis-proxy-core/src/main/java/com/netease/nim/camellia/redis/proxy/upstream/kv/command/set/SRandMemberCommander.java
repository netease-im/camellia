package com.netease.nim.camellia.redis.proxy.upstream.kv.command.set;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.SetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.Set;

/**
 * SRANDMEMBER key [count]
 * <p>
 * Created by caojiajun on 2024/8/5
 */
public class SRandMemberCommander extends Set0Commander {

    public SRandMemberCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.SRANDMEMBER;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 2 || objects.length == 3;
    }

    @Override
    protected Reply execute(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        int count = 1;
        boolean batch = false;
        if (objects.length == 3) {
            count = (int) Utils.bytesToNum(objects[2]);
            batch = true;
        }
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(slot, key);
        if (keyMeta == null) {
            if (batch) {
                return MultiBulkReply.EMPTY;
            } else {
                return BulkReply.NIL_REPLY;
            }
        }
        if (keyMeta.getKeyType() != KeyType.set) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisSet> bufferValue = setWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisSet set = bufferValue.getValue();
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            Set<BytesKey> srandmember = set.srandmember(count);
            return toReply(srandmember, batch);
        }
        if (cacheConfig.isSetLocalCacheEnable()) {
            SetLRUCache setLRUCache = cacheConfig.getSetLRUCache();

            RedisSet set = setLRUCache.getForRead(key, cacheKey);

            if (set != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                Set<BytesKey> srandmember = set.srandmember(count);
                return toReply(srandmember, batch);
            }

            boolean hotKey = setLRUCache.isHotKey(key);

            if (hotKey) {
                set = loadLRUCache(slot, keyMeta, key);
                setLRUCache.putAllForRead(key, cacheKey, set);
                //
                KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                Set<BytesKey> srandmember = set.srandmember(count);
                return toReply(srandmember, batch);
            }
        }

        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());

        Set<BytesKey> set = srandmemberFromKv(slot, keyMeta, key, count);

        return toReply(set, batch);
    }

    private Reply toReply(Set<BytesKey> srandmember, boolean batch) {
        if (batch) {
            Reply[] replies = new Reply[srandmember.size()];
            int i = 0;
            for (BytesKey bytesKey : srandmember) {
                replies[i] = new BulkReply(bytesKey.getKey());
                i++;
            }
            return new MultiBulkReply(replies);
        } else {
            if (srandmember.isEmpty()) {
                return BulkReply.NIL_REPLY;
            }
            BytesKey next = srandmember.iterator().next();
            return new BulkReply(next.getKey());
        }
    }
}
