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
 * HMGET key field [field ...]
 * <p>
 * Created by caojiajun on 2024/4/24
 */
public class HMGetCommander extends Hash0Commander {

    private static final byte[] script1 = ("local arg = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(arg) == 1 then\n" +
            "\tlocal ret = redis.call('hmget', KEYS[1], unpack(ARGV));\n" +
            "\treturn {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

    private static final byte[] script2 = ("local arg = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(arg) == 1 then\n" +
            "\tlocal ret = redis.call('get', KEYS[1], ARGV[1]);\n" +
            "\tredis.call('pexpire', KEYS[1], ARGV[2]);\n" +
            "\treturn {'1', ret};\n" +
            "end\n" +
            "return {'2'};").getBytes(StandardCharsets.UTF_8);

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
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];

        //meta
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
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

            RedisHash hash = hashLRUCache.getForRead(key, cacheKey);
            if (hash != null) {
                KvCacheMonitor.localCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return toReply2(fields, hash.hgetAll());
            }

            boolean hotKey = hashLRUCache.isHotKey(key);
            if (hotKey) {
                hash = loadLRUCache(keyMeta, key);
                hashLRUCache.putAllForRead(key, cacheKey, hash);
                KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
                return toReply2(fields, hash.hgetAll());
            }
        }

        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_1) {
            //
            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());
            //
            List<BytesKey> list = new ArrayList<>(objects.length - 2);
            byte[][] subKeys = new byte[objects.length - 2][];
            for (int i=2; i<objects.length; i++) {
                subKeys[i-2] = keyDesign.hashFieldSubKey(keyMeta, key, objects[i]);
                list.add(new BytesKey(subKeys[i-2]));
            }
            List<KeyValue> keyValues = kvClient.batchGet(subKeys);
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

        {
            Reply reply = sync(cacheRedisTemplate.sendLua(script1, new byte[][]{cacheKey}, fields));
            if (reply instanceof ErrorReply) {
                return reply;
            }
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                String type = Utils.bytesToString(((BulkReply) replies[0]).getRaw());
                if (type.equalsIgnoreCase("1")) {
                    cacheRedisTemplate.sendPExpire(key, cacheConfig.hgetallCacheMillis());
                    KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                    return replies[1];
                }
            }
        }
        {
            List<Command> commandList = new ArrayList<>(fields.length);
            for (byte[] field : fields) {
                byte[] hashFieldCacheKey = keyDesign.hashFieldCacheKey(keyMeta, key, field);
                Command cmd = cacheRedisTemplate.luaCommand(script2, new byte[][]{hashFieldCacheKey}, new byte[][]{hgetCacheMillis()});
                commandList.add(cmd);
            }
            Map<BytesKey, Reply> hitMap = new HashMap<>();
            List<byte[]> cacheMissingFields = new ArrayList<>();
            List<Reply> replyList = sync(cacheRedisTemplate.sendCommand(commandList));
            for (int i = 0; i < replyList.size(); i++) {
                byte[] field = fields[i];
                Reply reply = replyList.get(i);
                if (reply instanceof ErrorReply) {
                    return reply;
                }
                if (reply instanceof MultiBulkReply) {
                    Reply[] replies = ((MultiBulkReply) reply).getReplies();
                    String type = Utils.bytesToString(((BulkReply) replies[0]).getRaw());
                    if (type.equalsIgnoreCase("1")) {
                        hitMap.put(new BytesKey(field), replies[1]);
                        continue;
                    }
                }
                cacheMissingFields.add(field);
            }
            if (cacheMissingFields.isEmpty()) {
                KvCacheMonitor.redisCache(cacheConfig.getNamespace(), redisCommand().strRaw());
                return toReply1(fields, hitMap);
            }

            KvCacheMonitor.kvStore(cacheConfig.getNamespace(), redisCommand().strRaw());

            byte[][] subKeys = new byte[cacheMissingFields.size()][];
            int i=0;
            for (byte[] cacheMissingField : cacheMissingFields) {
                byte[] subKey = keyDesign.hashFieldSubKey(keyMeta, key, cacheMissingField);
                subKeys[i] = subKey;
                i ++;
            }
            List<KeyValue> keyValues = kvClient.batchGet(subKeys);
            List<Command> commands = new ArrayList<>(keyValues.size());
            for (KeyValue keyValue : keyValues) {
                if (keyValue == null) {
                    continue;
                }
                byte[] k = keyValue.getKey();
                byte[] v = keyValue.getValue();
                if (v == null) {
                    continue;
                }
                byte[] field = keyDesign.decodeHashFieldBySubKey(k, key);
                hitMap.put(new BytesKey(field), new BulkReply(v));

                byte[] hashFieldCacheKey = keyDesign.hashFieldCacheKey(keyMeta, key, field);
                Command cmd = new Command(new byte[][]{RedisCommand.PSETEX.raw(), hashFieldCacheKey, v});
                commands.add(cmd);
            }
            if (!commands.isEmpty()) {
                List<Reply> cacheSetReplies = sync(cacheRedisTemplate.sendCommand(commands));
                for (Reply cacheSetReply : cacheSetReplies) {
                    if (cacheSetReply instanceof ErrorReply) {
                        return cacheSetReply;
                    }
                }
            }
            return toReply1(fields, hitMap);
        }
    }

    private Reply toReply1(byte[][] fields, Map<BytesKey, Reply> map) {
        Reply[] replies = new Reply[fields.length];
        int i=0;
        for (byte[] field : fields) {
            Reply reply = map.get(new BytesKey(field));
            if (reply == null) {
                replies[i] = BulkReply.NIL_REPLY;
            } else {
                replies[i] = reply;
            }
            i ++;
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
