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
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * SADD key member [member ...]
 * <p>
 * Created by caojiajun on 2024/8/5
 */
public class SAddCommander extends Set0Commander {

    private static final byte[] script = ("local ret1 = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(ret1) == 1 then\n" +
            "  local ret = redis.call('sadd', KEYS[1], unpack(ARGV));\n" +
            "  return {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

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
    protected Reply execute(Command command) {
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
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            EncodeVersion encodeVersion = keyDesign.hashKeyMetaVersion();
            if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_2) {
                int count = memberSet.size();
                byte[] extra = BytesUtils.toBytes(count);
                keyMeta = new KeyMeta(encodeVersion, KeyType.set, System.currentTimeMillis(), -1, extra);
            } else if (encodeVersion == EncodeVersion.version_1 || encodeVersion == EncodeVersion.version_3) {
                keyMeta = new KeyMeta(encodeVersion, KeyType.set, System.currentTimeMillis(), -1);
            } else {
                return ErrorReply.INTERNAL_ERROR;
            }
            keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
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

        if (first) {
            set = new RedisSet(new HashSet<>(memberSet));
            setWriteBuffer.put(cacheKey, set);
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
        } else {
            WriteBufferValue<RedisSet> bufferValue = setWriteBuffer.get(cacheKey);
            if (bufferValue != null) {
                set = bufferValue.getValue();
                existsMemberSet = set.sadd(memberSet);
                result = setWriteBuffer.put(cacheKey, set);
                KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            }
        }

        if (cacheConfig.isSetLocalCacheEnable()) {
            SetLRUCache setLRUCache = cacheConfig.getSetLRUCache();

            if (first) {
                set = new RedisSet(new HashSet<>(memberSet));
                setLRUCache.putAllForWrite(key, cacheKey, set);
            } else {
                Set<BytesKey> existsSet = setLRUCache.sadd(key, cacheKey, memberSet);
                if (existsSet == null) {
                    boolean hotKey = setLRUCache.isHotKey(key);
                    if (hotKey) {
                        set = loadLRUCache(keyMeta, key);
                        //
                        setLRUCache.putAllForWrite(key, cacheKey, set);
                        //
                        existsSet = set.sadd(memberSet);
                    }
                } else {
                    KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                }
                if (existsMemberSet == null && existsSet != null) {
                    existsMemberSet = existsSet;
                }
            }

            if (result == null) {
                if (set == null) {
                    set = setLRUCache.getForWrite(key, cacheKey);
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
            return saddVersion0(keyMeta, key, cacheKey, first, memberSize, existsMemberSize, memberSet, result);
        }

        if (encodeVersion == EncodeVersion.version_1) {
            return saddVersion1(keyMeta, key, cacheKey, first, memberSize, existsMemberSize, memberSet, result);
        }

        if (encodeVersion == EncodeVersion.version_2) {
            if (!first) {
                int ret = checkAndUpdateCache(cacheKey, memberSet, memberSize);
                if (existsMemberSize < 0 && ret >= 0) {
                    existsMemberSize = ret;
                }
            }
            return saddVersion0(keyMeta, key, cacheKey, first, memberSize, existsMemberSize, memberSet, result);
        }

        if (encodeVersion == EncodeVersion.version_3) {
            if (!first) {
                int ret = checkAndUpdateCache(cacheKey, memberSet, memberSize);
                if (existsMemberSize < 0 && ret >= 0) {
                    existsMemberSize = ret;
                }
            }
            return saddVersion1(keyMeta, key, cacheKey, first, memberSize, existsMemberSize, memberSet, result);
        }

        return ErrorReply.INTERNAL_ERROR;
    }

    private int checkAndUpdateCache(byte[] cacheKey, Set<BytesKey> memberSet, int memberSize) {
        byte[][] args = new byte[memberSize][];
        int i = 0;
        for (BytesKey bytesKey : memberSet) {
            args[i] = bytesKey.getKey();
            i++;
        }
        int ret = -1;
        Reply reply = sync(cacheRedisTemplate.sendLua(script, new byte[][]{cacheKey}, args));
        if (reply instanceof MultiBulkReply) {
            Reply[] replies = ((MultiBulkReply) reply).getReplies();
            if (replies[0] instanceof BulkReply) {
                byte[] raw = ((BulkReply) replies[0]).getRaw();
                if (Utils.bytesToString(raw).equalsIgnoreCase("1")) {
                    if (replies[1] instanceof IntegerReply) {
                        ret = (((IntegerReply) replies[1]).getInteger()).intValue();
                    }
                }
            }
        }
        if (ret >= 0) {
            return memberSize - ret;
        }
        return ret;
    }

    private Reply saddVersion0(KeyMeta keyMeta, byte[] key, byte[] cacheKey, boolean first, int memberSize,
                               int existsMemberSize, Set<BytesKey> memberSet, Result result) {
        if (first) {
            writeMembers(keyMeta, key, cacheKey, memberSet, result);
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
            List<KeyValue> keyValues = kvClient.batchGet(subKeyArray);
            for (KeyValue keyValue : keyValues) {
                if (keyValue == null || keyValue.getKey() == null) {
                    continue;
                }
                existsMemberSize ++;
                memberSet.remove(new BytesKey(keyValue.getKey()));
            }
        }

        int add = memberSize - existsMemberSize;

        updateKeyMeta(keyMeta, key, add);

        if (add <= 0) {
            writeMembers(keyMeta, key, cacheKey, Collections.emptySet(), result);
        } else {
            writeMembers(keyMeta, key, cacheKey, memberSet, result);
        }

        return IntegerReply.parse(memberSize - existsMemberSize);
    }

    private Reply saddVersion1(KeyMeta keyMeta, byte[] key, byte[] cacheKey, boolean first, int memberSize,
                               int existsMemberSize, Set<BytesKey> memberSet, Result result) {
        if (first) {
            writeMembers(keyMeta, key, cacheKey, memberSet, result);
        } else {
            if (memberSize == existsMemberSize) {
                writeMembers(keyMeta, key, cacheKey, Collections.emptySet(), result);
            } else {
                writeMembers(keyMeta, key, cacheKey, memberSet, result);
            }
        }
        return IntegerReply.parse(memberSize);
    }

}
