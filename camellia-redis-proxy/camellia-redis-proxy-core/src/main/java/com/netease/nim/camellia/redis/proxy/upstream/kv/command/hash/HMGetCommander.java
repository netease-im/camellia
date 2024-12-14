package com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.HashLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisHash;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ValueWrapper;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.*;

/**
 * HMGET key field [field ...]
 * <p>
 * Created by caojiajun on 2024/4/24
 */
public class HMGetCommander extends Hash0Commander {

    public HMGetCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HMGET;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length >= 3;
    }

    @Override
    public Reply runToCompletion(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];

        //meta
        ValueWrapper<KeyMeta> valueWrapper = keyMetaServer.runToCompletion(slot, key);
        if (valueWrapper == null) {
            return null;
        }
        KeyMeta keyMeta = valueWrapper.get();
        if (keyMeta == null) {
            Reply[] replies = new Reply[objects.length - 2];
            Arrays.fill(replies, BulkReply.NIL_REPLY);
            return new MultiBulkReply(replies);
        }
        if (keyMeta.getKeyType() != KeyType.hash) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisHash> writeBufferValue = hashWriteBuffer.get(cacheKey);
        if (writeBufferValue != null) {
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            //
            byte[][] fields = new byte[objects.length - 2][];
            System.arraycopy(objects, 2, fields, 0, objects.length - 2);
            //
            return toReply2(fields, writeBufferValue.getValue().hgetAll());
        }

        if (cacheConfig.isHashLocalCacheEnable()) {
            HashLRUCache hashLRUCache = cacheConfig.getHashLRUCache();

            RedisHash hash = hashLRUCache.getForRead(slot, cacheKey);
            if (hash != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                //
                byte[][] fields = new byte[objects.length - 2][];
                System.arraycopy(objects, 2, fields, 0, objects.length - 2);
                //
                return toReply2(fields, hash.hgetAll());
            }
        }
        return null;
    }

    @Override
    protected Reply execute(int slot, Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];

        //meta
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(slot, key);
        if (keyMeta == null) {
            Reply[] replies = new Reply[objects.length - 2];
            Arrays.fill(replies, BulkReply.NIL_REPLY);
            return new MultiBulkReply(replies);
        }
        if (keyMeta.getKeyType() != KeyType.hash) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[][] fields = new byte[objects.length - 2][];
        System.arraycopy(objects, 2, fields, 0, objects.length - 2);

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisHash> writeBufferValue = hashWriteBuffer.get(cacheKey);
        if (writeBufferValue != null) {
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            return toReply2(fields, writeBufferValue.getValue().hgetAll());
        }

        if (cacheConfig.isHashLocalCacheEnable()) {
            HashLRUCache hashLRUCache = cacheConfig.getHashLRUCache();

            RedisHash hash = hashLRUCache.getForRead(slot, cacheKey);
            if (hash != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return toReply2(fields, hash.hgetAll());
            }

            boolean hotKey = hashLRUCache.isHotKey(key, redisCommand());
            if (hotKey) {
                hash = loadLRUCache(slot, keyMeta, key);
                hashLRUCache.putAllForRead(slot, cacheKey, hash);
                KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                return toReply2(fields, hash.hgetAll());
            }
        }

        //
        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        //
        List<BytesKey> list = new ArrayList<>(objects.length - 2);
        byte[][] subKeys = new byte[objects.length - 2][];
        for (int i=2; i<objects.length; i++) {
            subKeys[i-2] = keyDesign.hashFieldSubKey(keyMeta, key, objects[i]);
            list.add(new BytesKey(subKeys[i-2]));
        }
        List<KeyValue> keyValues = kvClient.batchGet(slot, subKeys);
        Map<BytesKey, byte[]> map = new HashMap<>();
        for (KeyValue keyValue : keyValues) {
            map.put(new BytesKey(keyValue.getKey()), keyValue.getValue());
        }
        Reply[] replies = new Reply[list.size()];
        for (int i=0; i<replies.length; i++) {
            BytesKey bytesKey = list.get(i);
            byte[] bytes = map.get(bytesKey);
            if (bytes == null) {
                replies[i] = BulkReply.NIL_REPLY;
            } else {
                replies[i] = new BulkReply(bytes);
            }
        }
        return new MultiBulkReply(replies);
    }

    private Reply toReply2(byte[][] fields, Map<BytesKey, byte[]> map) {
        Reply[] replies = new Reply[fields.length];
        int i=0;
        for (byte[] field : fields) {
            byte[] bytes = map.get(new BytesKey(field));
            if (bytes == null) {
                replies[i] = BulkReply.NIL_REPLY;
            } else {
                replies[i] = new BulkReply(bytes);
            }
            i ++;
        }
        return new MultiBulkReply(replies);
    }

}
