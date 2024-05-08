package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.Index;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ZREM key member [member ...]
 * <p>
 * Created by caojiajun on 2024/5/8
 */
public class ZRemCommander extends Commander {

    private static final byte[] script = ("local ret1 = redis.call('exists', KEYS[1]);\n" +
            "if ret1 then\n" +
            "  local ret = redis.call('zrem', KEYS[1], unpack(ARGV));\n" +
            "  return {'2', ret};\n" +
            "end\n" +
            "return {'1'};").getBytes(StandardCharsets.UTF_8);

    public ZRemCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZREM;
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
            return MultiBulkReply.EMPTY;
        }
        if (keyMeta.getKeyType() != KeyType.zset) {
            return ErrorReply.WRONG_TYPE;
        }
        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0) {
            return zremVersion0(keyMeta, key, objects);
        }
        if (encodeVersion == EncodeVersion.version_1) {
            return zremVersion1(keyMeta, key, objects);
        }
        if (encodeVersion == EncodeVersion.version_2) {
            return zremVersion2(keyMeta, key, objects);
        }
        if (encodeVersion == EncodeVersion.version_3) {
            return zremVersion3(keyMeta, key, objects);
        }
        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zremVersion0(KeyMeta keyMeta, byte[] key, byte[][] objects) {
        byte[][] storeKeys = new byte[objects.length - 2][];
        for (int i=2; i<objects.length; i++) {
            byte[] member = objects[i];
            byte[] zsetMemberSubKey1 = keyStruct.zsetMemberSubKey1(keyMeta, key, member);
            storeKeys[i-2] = zsetMemberSubKey1;
        }
        List<byte[]> delStoreKeys = new ArrayList<>(storeKeys.length * 2);
        List<KeyValue> keyValues = kvClient.batchGet(storeKeys);
        for (KeyValue keyValue : keyValues) {
            if (keyValue == null || keyValue.getValue() == null) {
                continue;
            }
            byte[] member = keyStruct.decodeZSetMemberBySubKey1(keyValue.getKey(), key);
            byte[] score = keyValue.getValue();
            delStoreKeys.add(keyValue.getKey());
            delStoreKeys.add(keyStruct.zsetMemberSubKey2(keyMeta, key, member, score));
        }
        int deleteCount = delStoreKeys.size() / 2;
        kvClient.batchDelete(delStoreKeys.toArray(new byte[0][0]));
        updateKeyMeta(keyMeta, key, deleteCount);
        return IntegerReply.parse(deleteCount);
    }

    private Reply zremVersion1(KeyMeta keyMeta, byte[] key, byte[][] objects) {
        byte[][] args = new byte[objects.length - 2][];
        System.arraycopy(objects, 2, args, 0, args.length);
        byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);
        Reply reply = sync(cacheRedisTemplate.sendLua(script, new byte[][]{cacheKey}, args));
        if (reply instanceof ErrorReply) {
            return reply;
        }
        int deleteCount = -1;
        if (reply instanceof MultiBulkReply) {
            Reply[] replies = ((MultiBulkReply) reply).getReplies();
            if (replies[0] instanceof BulkReply) {
                byte[] raw = ((BulkReply) replies[0]).getRaw();
                if (Utils.bytesToString(raw).equalsIgnoreCase("2")) {
                    if (replies[1] instanceof IntegerReply) {
                        deleteCount = (((IntegerReply) replies[1]).getInteger()).intValue();
                    }
                }
            }
        }

        byte[][] storeKeys = new byte[objects.length - 2][];
        for (int i=2; i<objects.length; i++) {
            byte[] member = objects[i];
            byte[] zsetMemberSubKey1 = keyStruct.zsetMemberSubKey1(keyMeta, key, member);
            storeKeys[i-2] = zsetMemberSubKey1;
        }

        if (deleteCount < 0) {
            List<byte[]> existsStoreKeys = new ArrayList<>(args.length);
            List<KeyValue> keyValues = kvClient.batchGet(storeKeys);
            for (KeyValue keyValue : keyValues) {
                if (keyValue == null || keyValue.getValue() == null) {
                    continue;
                }
                existsStoreKeys.add(keyValue.getKey());
            }
            deleteCount = existsStoreKeys.size();
            kvClient.batchDelete(existsStoreKeys.toArray(new byte[0][0]));
        } else {
            kvClient.batchDelete(storeKeys);
        }
        updateKeyMeta(keyMeta, key, deleteCount);
        return IntegerReply.parse(deleteCount);
    }

    private Reply zremVersion2(KeyMeta keyMeta, byte[] key, byte[][] objects) {
        byte[][] args = new byte[objects.length - 2][];
        System.arraycopy(objects, 2, args, 0, args.length);
        byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);

        List<byte[]> cacheDelCmd = new ArrayList<>(args.length);
        List<byte[]> deleteIndexKeys = new ArrayList<>(args.length);
        List<byte[]> deleteSubKeys = new ArrayList<>(args.length);
        cacheDelCmd.add(RedisCommand.DEL.raw());
        for (int i=2; i<objects.length; i++) {
            byte[] member = objects[i];
            Index index = Index.fromRaw(member);
            if (index.isIndex()) {
                cacheDelCmd.add(keyStruct.zsetMemberIndexCacheKey(keyMeta, key, index));
                deleteIndexKeys.add(keyStruct.zsetIndexSubKey(keyMeta, key, index));
            }
            args[i-2] = index.getRef();

            byte[] zsetMemberSubKey1 = keyStruct.zsetMemberSubKey1(keyMeta, key, member);
            deleteSubKeys.add(zsetMemberSubKey1);
        }
        List<Command> commandList = new ArrayList<>(2);
        commandList.add(cacheRedisTemplate.luaCommand(script, new byte[][]{cacheKey}, args));
        if (cacheDelCmd.size() > 1) {
            commandList.add(new Command(cacheDelCmd.toArray(new byte[0][0])));
        }

        List<Reply> replyList = sync(cacheRedisTemplate.sendCommand(commandList));
        for (Reply reply : replyList) {
            if (reply instanceof ErrorReply) {
                return reply;
            }
        }
        Reply reply = replyList.get(0);
        if (reply instanceof ErrorReply) {
            return reply;
        }
        int deleteCount = -1;
        if (reply instanceof MultiBulkReply) {
            Reply[] replies = ((MultiBulkReply) reply).getReplies();
            if (replies[0] instanceof BulkReply) {
                byte[] raw = ((BulkReply) replies[0]).getRaw();
                if (Utils.bytesToString(raw).equalsIgnoreCase("2")) {
                    if (replies[1] instanceof IntegerReply) {
                        deleteCount = (((IntegerReply) replies[1]).getInteger()).intValue();
                    }
                }
            }
        }
        deleteIndexKeys.addAll(deleteSubKeys);
        if (deleteCount < 0) {
            List<byte[]> existsStoreKeys = new ArrayList<>(args.length);
            List<KeyValue> keyValues = kvClient.batchGet(deleteSubKeys.toArray(new byte[0][0]));
            for (KeyValue keyValue : keyValues) {
                if (keyValue == null || keyValue.getValue() == null) {
                    continue;
                }
                existsStoreKeys.add(keyValue.getKey());
            }
            deleteCount = existsStoreKeys.size();
        }
        kvClient.batchDelete(deleteIndexKeys.toArray(new byte[0][0]));
        updateKeyMeta(keyMeta, key, deleteCount);
        return IntegerReply.parse(deleteCount);
    }

    private Reply zremVersion3(KeyMeta keyMeta, byte[] key, byte[][] objects) {
        byte[][] cmd = new byte[objects.length][];
        System.arraycopy(objects, 0, cmd, 0, cmd.length);
        byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);
        cmd[1] = cacheKey;
        List<byte[]> cacheDelCmd = new ArrayList<>(cmd.length);
        List<byte[]> deleteIndexKeys = new ArrayList<>(cmd.length);
        cacheDelCmd.add(RedisCommand.DEL.raw());
        for (int i=2; i<objects.length; i++) {
            byte[] member = objects[i];
            Index index = Index.fromRaw(member);
            if (index.isIndex()) {
                cacheDelCmd.add(keyStruct.zsetMemberIndexCacheKey(keyMeta, key, index));
                deleteIndexKeys.add(keyStruct.zsetIndexSubKey(keyMeta, key, index));
            }
            cmd[i] = index.getRef();
        }

        List<Command> commandList = new ArrayList<>(2);
        commandList.add(new Command(cmd));
        commandList.add(new Command(new byte[][]{RedisCommand.ZCARD.raw(), cacheKey}));
        if (cacheDelCmd.size() > 1) {
            commandList.add(new Command(cacheDelCmd.toArray(new byte[0][0])));
        }

        List<CompletableFuture<Reply>> futures = cacheRedisTemplate.sendCommand(commandList);
        if (!deleteIndexKeys.isEmpty()) {
            kvClient.batchDelete(deleteIndexKeys.toArray(new byte[0][0]));
        }
        List<Reply> replyList = sync(futures);
        for (Reply reply : replyList) {
            if (reply instanceof ErrorReply) {
                return reply;
            }
        }
        Reply reply = replyList.get(1);
        if (reply instanceof IntegerReply) {
            if (((IntegerReply) reply).getInteger() == 0) {
                keyMetaServer.deleteKeyMeta(key);
            }
        }
        return replyList.get(0);
    }

    private void updateKeyMeta(KeyMeta keyMeta, byte[] key, int deleteCount) {
        if (deleteCount > 0) {
            int count = BytesUtils.toInt(keyMeta.getExtra()) - deleteCount;
            if (count <= 0) {
                keyMetaServer.deleteKeyMeta(key);
                return;
            }
            byte[] extra = BytesUtils.toBytes(count);
            keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
            keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
        }
    }
}
