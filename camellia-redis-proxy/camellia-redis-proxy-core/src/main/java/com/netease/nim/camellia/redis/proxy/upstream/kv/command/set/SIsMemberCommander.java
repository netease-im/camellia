package com.netease.nim.camellia.redis.proxy.upstream.kv.command.set;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.SetLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;

/**
 * SISMEMBER key member
 * <p>
 * Created by caojiajun on 2024/8/5
 */
public class SIsMemberCommander extends Set0Commander {

    private static final byte[] script = ("local ret1 = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(ret1) == 1 then\n" +
            "  local ret = redis.call('sismember', KEYS[1], ARGV[1]);\n" +
            "  redis.call('pexpire', KEYS[1], ARGV[2]);\n" +
            "  return {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

    public SIsMemberCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.SISMEMBER;
    }

    @Override
    protected boolean parse(Command command) {
        return command.getObjects().length == 3;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        byte[] member = objects[2];
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            return IntegerReply.REPLY_0;
        }
        if (keyMeta.getKeyType() != KeyType.set) {
            return ErrorReply.WRONG_TYPE;
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        WriteBufferValue<RedisSet> bufferValue = setWriteBuffer.get(cacheKey);
        if (bufferValue != null) {
            RedisSet set = bufferValue.getValue();
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            boolean sismeber = set.sismeber(new BytesKey(member));
            return IntegerReply.parse(sismeber ? 1 : 0);
        }
        if (cacheConfig.isSetLocalCacheEnable()) {
            SetLRUCache setLRUCache = cacheConfig.getSetLRUCache();

            RedisSet set = setLRUCache.getForRead(key, cacheKey);

            if (set != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                boolean sismeber = set.sismeber(new BytesKey(member));
                return IntegerReply.parse(sismeber ? 1 : 0);
            }

            boolean hotKey = setLRUCache.isHotKey(key);

            if (hotKey) {
                set = loadLRUCache(keyMeta, key);
                setLRUCache.putAllForRead(key, cacheKey, set);
                //
                KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                boolean sismeber = set.sismeber(new BytesKey(member));
                return IntegerReply.parse(sismeber ? 1 : 0);
            }
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_2 || encodeVersion == EncodeVersion.version_3) {
            Reply reply = sync(cacheRedisTemplate.sendLua(script, new byte[][]{cacheKey}, new byte[][]{member, smembersCacheMillis()}));
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                if (replies[0] instanceof BulkReply) {
                    byte[] raw = ((BulkReply) replies[0]).getRaw();
                    if (Utils.bytesToString(raw).equalsIgnoreCase("1")) {
                        if (replies[1] instanceof IntegerReply) {
                            KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                            return replies[1];
                        }
                    }
                }
            }
        }

        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());

        byte[] subKey = keyDesign.setMemberSubKey(keyMeta, key, member);
        KeyValue keyValue = kvClient.get(subKey);
        if (keyValue == null || keyValue.getKey() == null) {
            return IntegerReply.REPLY_0;
        }
        return IntegerReply.REPLY_1;
    }
}
