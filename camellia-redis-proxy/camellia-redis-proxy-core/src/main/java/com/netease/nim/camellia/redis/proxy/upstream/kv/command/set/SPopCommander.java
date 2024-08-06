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
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * SPOP key [count]
 * <p>
 * Created by caojiajun on 2024/8/5
 */
public class SPopCommander extends Set0Commander {

    private static final byte[] script = ("local ret1 = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(ret1) == 1 then\n" +
            "  local ret = redis.call('spop', KEYS[1], ARGV[1]);\n" +
            "  return {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

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
                if (set != null) {
                    result = setWriteBuffer.put(cacheKey, new RedisSet(new HashSet<>(set.smembers())));
                }
            }
        }

        if (result == null) {
            result = NoOpResult.INSTANCE;
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        if (spop == null) {
            if (encodeVersion == EncodeVersion.version_2 || encodeVersion == EncodeVersion.version_3) {
                spop = spopFromCache(cacheKey, count);
            }
        } else {
            if (encodeVersion == EncodeVersion.version_2 || encodeVersion == EncodeVersion.version_3) {
                Reply reply = srem(cacheKey, spop);
                if (reply != null) {
                    return reply;
                }
            }
        }

        if (spop == null) {
            spop = srandmemberFromKv(keyMeta, key, count);
        }

        removeMembers(keyMeta, key, cacheKey, spop, result);

        if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_2) {
            updateKeyMeta(keyMeta, key, spop.size() * -1);
        }

        return toReply(spop, batch);
    }

    private Reply srem(byte[] cacheKey, Set<BytesKey> spop) {
        byte[][] cmd = new byte[spop.size() + 2][];
        cmd[0] = RedisCommand.SREM.raw();
        cmd[1] = cacheKey;
        int i = 2;
        for (BytesKey bytesKey : spop) {
            cmd[i] = bytesKey.getKey();
            i++;
        }
        Reply reply = sync(cacheRedisTemplate.sendCommand(new Command(cmd)));
        if (reply instanceof ErrorReply) {
            return reply;
        }
        return null;
    }

    private Set<BytesKey> spopFromCache(byte[] cacheKey, int count) {
        byte[][] args = new byte[2][];
        args[0] = Utils.stringToBytes(String.valueOf(count));
        args[1] = smembersCacheMillis();
        Reply reply = sync(cacheRedisTemplate.sendLua(script, new byte[][]{cacheKey}, args));
        if (reply instanceof MultiBulkReply) {
            Reply[] replies = ((MultiBulkReply) reply).getReplies();
            if (replies[0] instanceof BulkReply) {
                byte[] raw = ((BulkReply) replies[0]).getRaw();
                if (Utils.bytesToString(raw).equalsIgnoreCase("1")) {
                    if (replies[1] instanceof MultiBulkReply) {
                        Reply[] replies1 = ((MultiBulkReply) replies[1]).getReplies();
                        Set<BytesKey> spop = new HashSet<>();
                        for (Reply reply1 : replies1) {
                            if (reply1 instanceof BulkReply) {
                                spop.add(new BytesKey(((BulkReply) reply1).getRaw()));
                            }
                        }
                        return spop;
                    }
                }
            }
        }
        return null;
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
