package com.netease.nim.camellia.redis.proxy.upstream.kv.command.set;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.NoOpResult;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.HashSet;
import java.util.Set;

/**
 * SPOP key [count]
 * <p>
 * Created by caojiajun on 2024/8/5
 */
public class SPopCommander extends Set0Commander {

    public SPopCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.SPOP;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 2 || objects.length == 3;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        int count = 1;
        boolean batch = false;
        if (objects.length == 3) {
            count = (int) Utils.bytesToNum(objects[2]);
            batch = true;
        }
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            return BulkReply.NIL_REPLY;
        }
        if (keyMeta.getKeyType() != KeyType.set) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        Set<BytesKey> spop = null;
        Result result = null;

        WriteBufferValue<RedisSet> bufferValue = setWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisSet set = bufferValue.getValue();
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            spop = set.spop(count);
            result = setWriteBuffer.put(cacheKey, set);
        }
        if (cacheConfig.isSetLocalCacheEnable()) {
            if (spop == null) {
                spop = cacheConfig.getSetLRUCache().spop(key, cacheKey, count);
            } else {
                cacheConfig.getSetLRUCache().srem(key, cacheKey, spop);
            }

            if (result == null) {
                RedisSet set = cacheConfig.getSetLRUCache().getForWrite(key, cacheKey);
                result = setWriteBuffer.put(cacheKey, new RedisSet(new HashSet<>(set.smembers())));
            }
        }

        if (result == null) {
            result = NoOpResult.INSTANCE;
        }

        if (spop == null) {
            spop = srandmemberFromKv(keyMeta, key, count);
        }

        removeMembers(keyMeta, key, cacheKey, spop, result);

        updateKeyMeta(keyMeta, key, spop.size() * -1);

        return toReply(spop, batch);
    }

    private Reply toReply(Set<BytesKey> spop, boolean batch) {
        if (!batch) {
            if (spop.isEmpty()) {
                return BulkReply.NIL_REPLY;
            }
            BytesKey next = spop.iterator().next();
            return new BulkReply(next.getKey());
        }
        Reply[] replies = new Reply[spop.size()];
        int i = 0;
        for (BytesKey member : spop) {
            replies[i] = new BulkReply(member.getKey());
            i ++;
        }
        return new MultiBulkReply(replies);
    }
}
