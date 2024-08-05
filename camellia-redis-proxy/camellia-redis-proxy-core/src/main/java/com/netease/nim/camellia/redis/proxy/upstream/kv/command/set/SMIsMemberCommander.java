package com.netease.nim.camellia.redis.proxy.upstream.kv.command.set;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * SMISMEMBER key member [member ...]
 * <p>
 * Created by caojiajun on 2024/8/5
 */
public class SMIsMemberCommander extends Set0Commander {

    private static final byte[] script = ("local ret1 = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(ret1) == 1 then\n" +
            "  local ret = redis.call('smismember', KEYS[1], unpack(ARGV));\n" +
            "  return {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

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
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        List<BytesKey> members = new ArrayList<>();
        for (int i=2; i<objects.length; i++) {
            members.add(new BytesKey(objects[i]));
        }
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
            Map<BytesKey, Boolean> smismember = set.smismember(members);
            return toReply(smismember, members);
        }
        if (cacheConfig.isSetLocalCacheEnable()) {
            RedisSet set = cacheConfig.getSetLRUCache().getForRead(key, cacheKey);
            if (set != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                Map<BytesKey, Boolean> smismember = set.smismember(members);
                return toReply(smismember, members);
            }
            if (cacheConfig.getSetLRUCache().isHotKey(key)) {
                set = loadLRUCache(keyMeta, key);
                Map<BytesKey, Boolean> smismember = set.smismember(members);
                cacheConfig.getSetLRUCache().putAllForRead(key, cacheKey, set);
                return toReply(smismember, members);
            }
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        if (encodeVersion == EncodeVersion.version_2 || encodeVersion == EncodeVersion.version_3) {
            byte[][] args = new byte[members.size()][];
            int i = 0;
            for (BytesKey member : members) {
                args[i] = member.getKey();
                i++;
            }
            Reply reply = sync(cacheRedisTemplate.sendLua(script, new byte[][]{cacheKey}, args));
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                if (replies[0] instanceof BulkReply) {
                    byte[] raw = ((BulkReply) replies[0]).getRaw();
                    if (Utils.bytesToString(raw).equalsIgnoreCase("1")) {
                        if (replies[1] instanceof MultiBulkReply) {
                            KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                            cacheRedisTemplate.sendPExpire(cacheKey, cacheConfig.smembersCacheMillis());
                            return replies[1];
                        }
                    }
                }
            }
        }

        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        Map<BytesKey, Boolean> smismember = smismemberFromKv(keyMeta, key, members);
        return toReply(smismember, members);
    }

    public Map<BytesKey, Boolean> smismemberFromKv(KeyMeta keyMeta, byte[] key, List<BytesKey> members) {
        byte[][] subKeys = new byte[members.size()][];
        int i = 0;
        for (BytesKey member : members) {
            subKeys[i] = keyDesign.setMemberSubKey(keyMeta, key, member.getKey());
            i ++;
        }
        List<KeyValue> keyValues = kvClient.batchGet(subKeys);
        Map<BytesKey, Boolean> map = new HashMap<>();
        for (KeyValue keyValue : keyValues) {
            if (keyValue == null || keyValue.getKey() == null) {
                continue;
            }
            byte[] member = keyDesign.decodeSetMemberBySubKey(keyValue.getKey(), key);
            map.put(new BytesKey(member), true);
        }
        return map;
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
