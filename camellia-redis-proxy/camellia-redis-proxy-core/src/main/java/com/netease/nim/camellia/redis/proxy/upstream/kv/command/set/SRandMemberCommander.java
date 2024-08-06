package com.netease.nim.camellia.redis.proxy.upstream.kv.command.set;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * SRANDMEMBER key [count]
 * <p>
 * Created by caojiajun on 2024/8/5
 */
public class SRandMemberCommander extends Set0Commander {

    private static final byte[] script1 = ("local ret1 = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(ret1) == 1 then\n" +
            "  local ret = redis.call('srandmember', KEYS[1], ARGV[1]);\n" +
            "  redis.call('pexpire', KEYS[1], ARGV[2]);\n" +
            "  return {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

    private static final byte[] script2 = ("local ret1 = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(ret1) == 1 then\n" +
            "  local ret = redis.call('srandmember', KEYS[1]);\n" +
            "  redis.call('pexpire', KEYS[1], ARGV[1]);\n" +
            "  return {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

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
            RedisSet set = cacheConfig.getSetLRUCache().getForRead(key, cacheKey);
            if (set != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                Set<BytesKey> srandmember = set.srandmember(count);
                return toReply(srandmember, batch);
            }
            if (cacheConfig.getSetLRUCache().isHotKey(key)) {
                set = loadLRUCache(keyMeta, key);
                Set<BytesKey> srandmember = set.srandmember(count);
                cacheConfig.getSetLRUCache().putAllForRead(key, cacheKey, set);
                return toReply(srandmember, batch);
            }
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_2 || encodeVersion == EncodeVersion.version_3) {
            byte[][] args;
            Reply reply;
            if (batch) {
                args = new byte[2][];
                args[0] = Utils.stringToBytes(String.valueOf(count));
                args[1] = smembersCacheMillis();
                reply = sync(cacheRedisTemplate.sendLua(script1, new byte[][]{cacheKey}, args));
            } else {
                args = new byte[1][];
                args[0] = smembersCacheMillis();
                reply = sync(cacheRedisTemplate.sendLua(script2, new byte[][]{cacheKey}, args));
            }
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                if (replies[0] instanceof BulkReply) {
                    byte[] raw = ((BulkReply) replies[0]).getRaw();
                    if (Utils.bytesToString(raw).equalsIgnoreCase("1")) {
                        KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                        return replies[1];
                    }
                }
            }
        }

        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        Set<BytesKey> set = srandmemberFromKv(keyMeta, key, count);

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
