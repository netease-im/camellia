package com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.NoOpResult;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.WriteBufferValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.HashLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * HDEL key field [field ...]
 * <p>
 * Created by caojiajun on 2024/4/11
 */
public class HDelCommander extends Commander {

    private static final byte[] script = ("local arg1 = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(arg1) == 1 then\n" +
            "\tlocal ret = redis.call('hdel', KEYS[1], unpack(ARGV));\n" +
            "\treturn {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

    public HDelCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HDEL;
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
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            return IntegerReply.REPLY_0;
        }
        if (keyMeta.getKeyType() != KeyType.hash) {
            return ErrorReply.WRONG_TYPE;
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();

        Set<BytesKey> fields = new HashSet<>(objects.length - 2);
        for (int i = 2; i < objects.length; i++) {
            fields.add(new BytesKey(objects[i]));
        }

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        int delCount = -1;

        Result result = null;
        boolean deleteAll = false;
        WriteBufferValue<Map<BytesKey, byte[]>> writeBufferValue = hashWriteBuffer.get(cacheKey);
        if (writeBufferValue != null) {
            KvCacheMonitor.writeBuffer(cacheConfig.getNamespace(), redisCommand().strRaw());
            Map<BytesKey, byte[]> map = writeBufferValue.getValue();
            delCount = 0;
            for (BytesKey field : fields) {
                byte[] remove = map.remove(field);
                if (remove != null) {
                    delCount ++;
                }
            }
            result = hashWriteBuffer.put(cacheKey, map);
            if (delCount == 0) {
                if (encodeVersion == EncodeVersion.version_1 || encodeVersion == EncodeVersion.version_3) {
                    return IntegerReply.parse(fields.size());
                }
                return IntegerReply.REPLY_0;
            }
            deleteAll = map.isEmpty();
        }

        if (cacheConfig.isHashLocalCacheEnable()) {
            HashLRUCache hashLRUCache = cacheConfig.getHashLRUCache();
            HashLRUCache.LRUCacheWriteResult cacheWriteResult = hashLRUCache.hdel(cacheKey, fields);
            if (cacheWriteResult != null && result == null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                delCount = cacheWriteResult.getInfluencedFields();
                result = hashWriteBuffer.put(cacheKey, cacheWriteResult.getCache());
            }
        }
        if (delCount < 0) {
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
        }
        if (delCount == 0) {
            if (encodeVersion == EncodeVersion.version_1 || encodeVersion == EncodeVersion.version_3) {
                return IntegerReply.parse(fields.size());
            }
            return IntegerReply.REPLY_0;
        }

        if (result == null) {
            result = NoOpResult.INSTANCE;
        }

        int fieldSize = fields.size();

        if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_1) {
            byte[][] subKeys = new byte[fieldSize][];
            int i=0;
            for (BytesKey field : fields) {
                subKeys[i] = keyDesign.hashFieldSubKey(keyMeta, key, field.getKey());
                i++;
            }
            if (encodeVersion == EncodeVersion.version_0) {
                if (delCount < 0) {
                    boolean[] exists = kvClient.exists(subKeys);
                    delCount = Utils.count(exists);
                }
                if (delCount > 0) {
                    int size = BytesUtils.toInt(keyMeta.getExtra()) - delCount;
                    if (size <= 0) {
                        keyMetaServer.deleteKeyMeta(key);
                    } else {
                        byte[] extra = BytesUtils.toBytes(size);
                        keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
                        keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
                    }
                }
                batchDeleteSubKeys(cacheKey, result, subKeys);
                return IntegerReply.parse(delCount);
            } else {
                if (!result.isKvWriteDelayEnable()) {
                    if (checkHLenZero(key, keyMeta)) {
                        keyMetaServer.deleteKeyMeta(key);
                    }
                } else {
                    if (deleteAll) {
                        keyMetaServer.deleteKeyMeta(key);
                    }
                }
                batchDeleteSubKeys(cacheKey, result, subKeys);
                return IntegerReply.parse(fieldSize);
            }
        }

        List<Command> commands = new ArrayList<>(fieldSize + 1);

        byte[][] args = new byte[fieldSize][];
        System.arraycopy(objects, 2, args, 0, args.length);
        Command luaCmd = cacheRedisTemplate.luaCommand(script, new byte[][]{cacheKey}, args);
        commands.add(luaCmd);

        byte[][] subKeys = new byte[fieldSize][];

        int i=0;
        for (BytesKey field : fields) {
            byte[] hashFieldCacheKey = keyDesign.hashFieldCacheKey(keyMeta, key, field.getKey());
            Command cmd = new Command(new byte[][]{RedisCommand.DEL.raw(), hashFieldCacheKey});
            commands.add(cmd);
            subKeys[i] = keyDesign.hashFieldSubKey(keyMeta, key, field.getKey());
            i++;
        }
        List<Reply> replyList = sync(cacheRedisTemplate.sendCommand(commands));
        for (Reply reply : replyList) {
            if (reply instanceof ErrorReply) {
                return reply;
            }
        }
        Reply luaReply = replyList.get(0);
        Reply reply = null;
        if (luaReply instanceof MultiBulkReply) {
            Reply[] replies = ((MultiBulkReply) luaReply).getReplies();
            String type = Utils.bytesToString(((BulkReply) replies[0]).getRaw());
            if (type.equalsIgnoreCase("1")) {//cache hit
                reply = replies[1];
            }
        }

        if (delCount < 0) {
            if (reply instanceof IntegerReply) {
                delCount = ((IntegerReply) reply).getInteger().intValue();
            }
        }

        if (encodeVersion == EncodeVersion.version_2) {
            if (delCount < 0) {
                boolean[] exists = kvClient.exists(subKeys);
                delCount = Utils.count(exists);
            }
            if (delCount > 0) {
                int size = BytesUtils.toInt(keyMeta.getExtra()) - delCount;
                if (size <= 0) {
                    keyMetaServer.deleteKeyMeta(key);
                } else {
                    byte[] extra = BytesUtils.toBytes(size);
                    keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
                    keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
                }
            }
            batchDeleteSubKeys(cacheKey, result, subKeys);
            return IntegerReply.parse(delCount);
        } else if (keyMeta.getEncodeVersion() == EncodeVersion.version_3) {
            if (!result.isKvWriteDelayEnable()) {
                if (checkHLenZero(key, keyMeta)) {
                    keyMetaServer.deleteKeyMeta(key);
                }
            } else {
                if (deleteAll) {
                    keyMetaServer.deleteKeyMeta(key);
                }
            }
            batchDeleteSubKeys(cacheKey, result, subKeys);
            return IntegerReply.parse(fieldSize);
        } else {
            return ErrorReply.INTERNAL_ERROR;
        }
    }

    private void batchDeleteSubKeys(byte[] cacheKey, Result result, byte[][] subKeys) {
        if (!result.isKvWriteDelayEnable()) {
            kvClient.batchDelete(subKeys);
        } else {
            submitAsyncWriteTask(cacheKey, result, () -> kvClient.batchDelete(subKeys));
        }
    }

    private boolean checkHLenZero(byte[] key, KeyMeta keyMeta) {
        byte[] startKey = keyDesign.hashFieldSubKey(keyMeta, key, new byte[0]);
        List<KeyValue> scan = kvClient.scanByPrefix(startKey, startKey, 1, Sort.ASC, false);
        return scan.isEmpty();
    }
}
