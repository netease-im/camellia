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
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.*;

/**
 * SMISMEMBER key member [member ...]
 * <p>
 * Created by caojiajun on 2024/8/5
 */
public class SMIsMemberCommander extends Set0Commander {

    public SMIsMemberCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.SMISMEMBER;
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
        List<BytesKey> members = new ArrayList<>();
        for (int i=2; i<objects.length; i++) {
            members.add(new BytesKey(objects[i]));
        }
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(slot, key);
        if (keyMeta == null) {
            Reply[] replies = new Reply[members.size()];
            Arrays.fill(replies, IntegerReply.REPLY_0);
            return new MultiBulkReply(replies);
        }
        if (keyMeta.getKeyType() != KeyType.set) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisSet> bufferValue = setWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisSet set = bufferValue.getValue();
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            Map<BytesKey, Boolean> smismember = set.smismember(members);
            return toReply(smismember, members);
        }
        if (cacheConfig.isSetLocalCacheEnable()) {
            SetLRUCache setLRUCache = cacheConfig.getSetLRUCache();

            RedisSet set = setLRUCache.getForRead(slot, cacheKey);

            if (set != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                Map<BytesKey, Boolean> smismember = set.smismember(members);
                return toReply(smismember, members);
            }

            boolean hotKey = setLRUCache.isHotKey(key);

            if (hotKey) {
                set = loadLRUCache(slot, keyMeta, key);
                setLRUCache.putAllForRead(slot, cacheKey, set);
                //
                KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                Map<BytesKey, Boolean> smismember = set.smismember(members);
                return toReply(smismember, members);
            }
        }

        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());

        Map<BytesKey, Boolean> smismember = smismemberFromKv(slot, keyMeta, key, members);
        return toReply(smismember, members);
    }

    private Reply toReply(Map<BytesKey, Boolean> smismember, List<BytesKey> members) {
        Reply[] replies = new Reply[members.size()];
        int i = 0;
        for (BytesKey member : members) {
            Boolean exists = smismember.get(member);
            if (exists == null || !exists) {
                replies[i] = IntegerReply.REPLY_0;
            } else {
                replies[i] = IntegerReply.REPLY_1;
            }
            i ++;
        }
        return new MultiBulkReply(replies);
    }
}
