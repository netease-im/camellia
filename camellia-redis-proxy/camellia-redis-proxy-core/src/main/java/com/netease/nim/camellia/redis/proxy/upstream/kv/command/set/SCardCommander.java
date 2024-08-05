package com.netease.nim.camellia.redis.proxy.upstream.kv.command.set;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.IntegerReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;

/**
 * SCARD key
 * <p>
 * Created by caojiajun on 2024/8/5
 */
public class SCardCommander extends Commander {

    public SCardCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.SCARD;
    }

    @Override
    protected boolean parse(Command command) {
        return command.getObjects().length == 2;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
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
            return IntegerReply.parse(set.scard());
        }
        if (cacheConfig.isSetLocalCacheEnable()) {
            RedisSet set = cacheConfig.getSetLRUCache().getForRead(key, cacheKey);
            if (set != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return IntegerReply.parse(set.scard());
            }
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_2) {
            return IntegerReply.parse(BytesUtils.toInt(keyMeta.getExtra()));
        }

        if (encodeVersion == EncodeVersion.version_1) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            long size = getSizeFromKv(keyMeta, key);
            return IntegerReply.parse(size);
        }
        if (encodeVersion == EncodeVersion.version_3) {
            Reply reply = sync(cacheRedisTemplate.sendCommand(new Command(new byte[][]{RedisCommand.SCARD.raw(), cacheKey})));
            if (reply instanceof IntegerReply) {
                Long size = ((IntegerReply) reply).getInteger();
                if (size != null && size > 0) {
                    KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                    return reply;
                }
            }
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            long size = getSizeFromKv(keyMeta, key);
            return IntegerReply.parse(size);
        } else {
            return ErrorReply.INTERNAL_ERROR;
        }
    }

    private long getSizeFromKv(KeyMeta keyMeta, byte[] key) {
        byte[] startKey = keyDesign.setMemberSubKey(keyMeta, key, new byte[0]);
        return kvClient.countByPrefix(startKey, startKey, false);
    }
}
