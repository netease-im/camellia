package com.netease.nim.camellia.redis.proxy.kv.core.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.kv.core.command.Commander;
import com.netease.nim.camellia.redis.proxy.kv.core.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.kv.core.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.kv.core.meta.KeyMetaVersion;
import com.netease.nim.camellia.redis.proxy.kv.core.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.kv.core.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.kv.core.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 *
 * HSET key field value [field value ...]
 * <p>
 * Created by caojiajun on 2024/4/7
 */
public class HSetCommander extends Commander {

    private static final byte[] script = ("local ret1 = '1';\n" +
            "local ret2 = '1';\n" +
            "local ret3 = '1';\n" +
            "local arg1 = redis.call('exists', KEYS[1]);\n" +
            "if tonumber(arg1) == 1 then\n" +
            "\tret1 = '2';\n" +
            "\tret2 = redis.call('hset', KEYS[1], ARGV[1], ARGV[2]);\n" +
            "end\n" +
            "local arg2 = redis.call('pttl', KEYS[2]);\n" +
            "if tonumber(arg2) > 0 then\n" +
            "\tret3 = '2';\n" +
            "\tredis.call('psetex', KEYS[2], arg2, ARGV[2]);\n" +
            "end\n" +
            "return {ret1, ret2, ret3};").getBytes(StandardCharsets.UTF_8);
    private static final byte[] two = "2".getBytes(StandardCharsets.UTF_8);

    public HSetCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.HSET;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        if (objects.length < 4) {
            return false;
        }
        return objects.length % 2 == 0;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];

        Map<BytesKey, byte[]> fieldMap = new HashMap<>();
        for (int i=2; i<objects.length; i+=2) {
            byte[] field = objects[i];
            byte[] value = objects[i + 1];
            fieldMap.put(new BytesKey(field), value);
        }

        boolean first = false;

        //check meta
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            KeyMetaVersion keyMetaVersion = keyStruct.hashKeyMetaVersion();
            if (keyMetaVersion == KeyMetaVersion.version_0) {
                int count = fieldMap.size();
                byte[] extra = BytesUtils.toBytes(count);
                keyMeta = new KeyMeta(keyMetaVersion, KeyType.hash, System.currentTimeMillis(), -1, extra);
            } else if (keyMetaVersion == KeyMetaVersion.version_1) {
                keyMeta = new KeyMeta(keyMetaVersion, KeyType.hash, System.currentTimeMillis(), -1);
            } else {
                return ErrorReply.INTERNAL_ERROR;
            }
            keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
            first = true;
        } else {
            if (keyMeta.getKeyType() != KeyType.hash) {
                return ErrorReply.WRONG_TYPE;
            }
        }

        if (first) {
            List<KeyValue> list = new ArrayList<>();
            for (Map.Entry<BytesKey, byte[]> entry : fieldMap.entrySet()) {
                byte[] field = entry.getKey().getKey();
                byte[] value = entry.getValue();
                byte[] storeKey = keyStruct.hashFieldStoreKey(keyMeta, key, field);//store-key
                KeyValue keyValue = new KeyValue(storeKey, value);
                list.add(keyValue);
            }
            kvClient.batchPut(list);
            return IntegerReply.parse(list.size());
        }

        //param parse
        byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);
        List<Command> commands = new ArrayList<>(fieldMap.size());
        List<KeyValue> list = new ArrayList<>(fieldMap.size());
        List<byte[]> fieldList = new ArrayList<>(fieldMap.size());
        for (Map.Entry<BytesKey, byte[]> entry : fieldMap.entrySet()) {
            byte[] field = entry.getKey().getKey();
            fieldList.add(field);
            byte[] value = entry.getValue();
            byte[] storeKey = keyStruct.hashFieldStoreKey(keyMeta, key, field);//store-key
            KeyValue keyValue = new KeyValue(storeKey, value);
            list.add(keyValue);
            byte[] hashFieldCacheKey = keyStruct.hashFieldCacheKey(keyMeta, key, field);//cache-key
            Command cmd = new Command(new byte[][]{RedisCommand.EVAL.raw(), script, two, cacheKey, hashFieldCacheKey, field, value});
            commands.add(cmd);
        }

        int existsFields = 0;
        List<byte[]> unknownFields = new ArrayList<>();
        //cache
        List<Reply> replyList = sync(cacheRedisTemplate.sendCommand(commands));
        int index = 0;
        for (Reply reply : replyList) {
            if (reply instanceof ErrorReply) {
                return reply;
            }
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                if (replies[0] instanceof BulkReply) {
                    byte[] raw = ((BulkReply) replies[0]).getRaw();
                    if (Utils.bytesToString(raw).equalsIgnoreCase("2")) {
                        if (replies[1] instanceof IntegerReply) {
                            Long integer = ((IntegerReply) replies[1]).getInteger();
                            if (integer == 0) {
                                existsFields ++;
                                index ++;
                                continue;
                            } else if (integer == 1) {
                                index ++;
                                continue;
                            }
                        }
                    }
                }
                if (replies[2] instanceof BulkReply) {
                    byte[] raw = ((BulkReply) replies[2]).getRaw();
                    if (Utils.bytesToString(raw).equalsIgnoreCase("2")) {
                        existsFields ++;
                        index ++;
                        continue;
                    }
                }
            }
            unknownFields.add(fieldList.get(index));
            index ++;
        }

        if (keyMeta.getKeyMetaVersion() == KeyMetaVersion.version_0) {
            int existsCount = existsFields;
            if (!unknownFields.isEmpty()) {
                byte[][] storeKeys = new byte[unknownFields.size()][];
                for (int i=0; i<unknownFields.size(); i++) {
                    byte[] field = unknownFields.get(i);
                    storeKeys[i] = keyStruct.hashFieldStoreKey(keyMeta, key, field);
                }
                boolean[] exists = kvClient.exists(storeKeys);
                for (boolean exist : exists) {
                    if (exist) {
                        existsCount ++;
                    }
                }
            }
            kvClient.batchPut(list);
            int add = list.size() - existsCount;
            if (add > 0) {
                int size = BytesUtils.toInt(keyMeta.getExtra());
                size = size + add;
                keyMeta = new KeyMeta(keyMeta.getKeyMetaVersion(), keyMeta.getKeyType(),
                        keyMeta.getKeyVersion(), keyMeta.getExpireTime(), BytesUtils.toBytes(size));
                keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
            }
            return IntegerReply.parse(add);
        } else if (keyMeta.getKeyMetaVersion() == KeyMetaVersion.version_1) {
            //store
            kvClient.batchPut(list);
            return IntegerReply.parse(list.size());//可能不准
        } else {
            return ErrorReply.INTERNAL_ERROR;
        }
    }

}
