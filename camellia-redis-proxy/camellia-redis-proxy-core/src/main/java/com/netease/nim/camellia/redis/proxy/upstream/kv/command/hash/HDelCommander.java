package com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

        int fieldSize = objects.length - 2;

        if (!cacheConfig.isValueCacheEnable()) {
            byte[][] subKeys = new byte[fieldSize][];
            for (int i=2; i<objects.length; i++) {
                subKeys[i-2] = keyStruct.hashFieldSubKey(keyMeta, key, objects[i]);
            }
            if (keyMeta.getEncodeVersion() == EncodeVersion.version_0) {
                int delCount = 0;
                boolean[] exists = kvClient.exists(subKeys);
                for (boolean exist : exists) {
                    if (exist) {
                        delCount ++;
                    }
                }
                if (delCount > 0) {
                    int size = BytesUtils.toInt(keyMeta.getExtra());
                    byte[] extra = BytesUtils.toBytes(size - delCount);
                    keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
                    keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
                }
                kvClient.batchDelete(subKeys);
                return IntegerReply.parse(delCount);
            } else if (keyMeta.getEncodeVersion() == EncodeVersion.version_1) {
                kvClient.batchDelete(subKeys);
                return IntegerReply.parse(fieldSize);
            } else {
                return ErrorReply.INTERNAL_ERROR;
            }
        }

        byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);

        {
            if (fieldSize == 1) {
                byte[] field = objects[2];

                byte[] hashFieldCacheKey = keyStruct.hashFieldCacheKey(keyMeta, key, field);
                Command cmd1 = new Command(new byte[][]{RedisCommand.HDEL.raw(), cacheKey, field});
                Command cmd2 = new Command(new byte[][]{RedisCommand.DEL.raw(), hashFieldCacheKey});
                List<Command> commands = new ArrayList<>(2);
                commands.add(cmd1);
                commands.add(cmd2);
                List<Reply> replies = sync(cacheRedisTemplate.sendCommand(commands));
                boolean hit = false;
                for (Reply reply : replies) {
                    if (reply instanceof ErrorReply) {
                        return reply;
                    }
                    if (reply instanceof IntegerReply) {
                        if (((IntegerReply) reply).getInteger() > 0) {
                            hit = true;
                        }
                    }
                }

                byte[] hashFieldSubKey = keyStruct.hashFieldSubKey(keyMeta, key, field);

                if (!hit && keyMeta.getEncodeVersion() == EncodeVersion.version_0) {
                    hit = kvClient.exists(hashFieldSubKey);
                }

                if (hit && keyMeta.getEncodeVersion() == EncodeVersion.version_0) {
                    int size = BytesUtils.toInt(keyMeta.getExtra());
                    byte[] extra = BytesUtils.toBytes(size - 1);
                    keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
                    keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
                }

                kvClient.delete(hashFieldSubKey);
                if (hit || keyMeta.getEncodeVersion() == EncodeVersion.version_1) {
                    return IntegerReply.REPLY_1;
                } else {
                    return IntegerReply.REPLY_0;
                }
            }
        }

        List<Command> commands = new ArrayList<>(fieldSize + 1);

        byte[][] args = new byte[fieldSize][];
        System.arraycopy(objects, 2, args, 0, args.length);
        Command luaCmd = cacheRedisTemplate.luaCommand(script, new byte[][]{cacheKey}, args);
        commands.add(luaCmd);

        byte[][] subKeys = new byte[fieldSize][];

        for (int i=2; i<objects.length; i++) {
            byte[] hashFieldCacheKey = keyStruct.hashFieldCacheKey(keyMeta, key, objects[i]);
            Command cmd = new Command(new byte[][]{RedisCommand.DEL.raw(), hashFieldCacheKey});
            commands.add(cmd);
            subKeys[i-2] = keyStruct.hashFieldSubKey(keyMeta, key, objects[i]);
        }
        List<Reply> replyList = sync(cacheRedisTemplate.sendCommand(commands));
        for (Reply reply : replyList) {
            if (reply instanceof ErrorReply) {
                return reply;
            }
        }
        Reply luaReply = replyList.get(0);
        Reply result = null;
        if (luaReply instanceof MultiBulkReply) {
            Reply[] replies = ((MultiBulkReply) luaReply).getReplies();
            String type = Utils.bytesToString(((BulkReply) replies[0]).getRaw());
            if (type.equalsIgnoreCase("1")) {//cache hit
                result = replies[1];
            }
        }

        if (result != null) {
            kvClient.batchDelete(subKeys);
            return result;
        }
        if (keyMeta.getEncodeVersion() == EncodeVersion.version_0) {
            int cacheHitCount = 0;
            List<byte[]> cacheMissFieldStoreKey = new ArrayList<>();
            for (int i=1; i<replyList.size(); i++) {
                Reply reply = replyList.get(i);
                if (reply instanceof IntegerReply) {
                    Long integer = ((IntegerReply) reply).getInteger();
                    if (integer == 0) {
                        cacheMissFieldStoreKey.add(subKeys[i-1]);
                    } else {
                        cacheHitCount ++;
                    }
                } else {
                    cacheMissFieldStoreKey.add(subKeys[i-1]);
                }
            }
            if (cacheMissFieldStoreKey.isEmpty()) {
                return IntegerReply.parse(fieldSize);
            }
            int delCount = cacheHitCount;
            boolean[] exists = kvClient.exists(cacheMissFieldStoreKey.toArray(new byte[0][0]));
            for (boolean exist : exists) {
                if (exist) {
                    delCount ++;
                }
            }

            int size = BytesUtils.toInt(keyMeta.getExtra());
            byte[] extra = BytesUtils.toBytes(size - delCount);
            keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
            keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);

            kvClient.batchDelete(subKeys);
            return IntegerReply.parse(delCount);
        } else if (keyMeta.getEncodeVersion() == EncodeVersion.version_1) {
            kvClient.batchDelete(subKeys);
            return IntegerReply.parse(fieldSize);
        } else {
            return ErrorReply.INTERNAL_ERROR;
        }
    }
}
