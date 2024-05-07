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
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ZADD key score member [score member   ...]
 * <p>
 * Created by caojiajun on 2024/4/11
 */
public class ZAddCommander extends Commander {

    private static final byte[] script1 = ("local ret1 = redis.call('exists', KEYS[1]);\n" +
            "if ret1 then\n" +
            "  local ret = redis.call('zadd', KEYS[1], unpack(ARGV));\n" +
            "  return {'2', ret};\n" +
            "end\n" +
            "return {'1'};").getBytes(StandardCharsets.UTF_8);


    public ZAddCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZADD;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length >= 4 && (objects.length % 2 == 0);
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        Map<BytesKey, byte[]> memberMap = new HashMap<>();
        for (int i=2; i<objects.length; i+=2) {
            byte[] score = objects[i];
            byte[] member = objects[i+1];
            memberMap.put(new BytesKey(member), score);
        }
        int memberSize = memberMap.size();

        boolean first = false;
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        EncodeVersion encodeVersion;
        if (keyMeta == null) {
            encodeVersion = keyStruct.zsetKeyMetaVersion();
            if (encodeVersion == EncodeVersion.version_0) {
                if (!kvClient.supportReverseScan()) {
                    return ErrorReply.KV_STORAGE_NOT_SUPPORTED_ENCODE;
                }
            }
            if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_1 || encodeVersion == EncodeVersion.version_2) {
                byte[] extra = BytesUtils.toBytes(memberSize);
                keyMeta = new KeyMeta(encodeVersion, KeyType.zset, System.currentTimeMillis(), -1, extra);
            } else {
                keyMeta = new KeyMeta(encodeVersion, KeyType.zset, System.currentTimeMillis(), -1);
            }
            keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
            first = true;
        } else {
            if (keyMeta.getKeyType() != KeyType.zset) {
                return ErrorReply.WRONG_TYPE;
            }
            encodeVersion = keyMeta.getEncodeVersion();
        }

        if (encodeVersion == EncodeVersion.version_0) {
            return zaddVersion0(keyMeta, key, first, memberSize, memberMap);
        }
        if (encodeVersion == EncodeVersion.version_1) {
            return zaddVersion1(keyMeta, key, first, memberSize, memberMap);
        }
        if (encodeVersion == EncodeVersion.version_2) {
            return zaddVersion2(keyMeta, key, first, memberSize, memberMap);
        }
        if (encodeVersion == EncodeVersion.version_3) {
            return zaddVersion3(keyMeta, key, memberSize, memberMap);
        }
        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zaddVersion0(KeyMeta keyMeta, byte[] key, boolean first, int memberSize, Map<BytesKey, byte[]> memberMap) {
        if (first) {
            List<KeyValue> list = new ArrayList<>(memberSize*2);
            for (Map.Entry<BytesKey, byte[]> entry : memberMap.entrySet()) {
                byte[] member = entry.getKey().getKey();
                byte[] score = entry.getValue();
                byte[] subKey1 = keyStruct.zsetMemberSubKey1(keyMeta, key, member);
                byte[] subKey2 = keyStruct.zsetMemberSubKey2(keyMeta, key, member, score);
                KeyValue keyValue1 = new KeyValue(subKey1, score);
                KeyValue keyValue2 = new KeyValue(subKey2, new byte[0]);
                list.add(keyValue1);
                list.add(keyValue2);
            }
            kvClient.batchPut(list);
            return IntegerReply.parse(memberSize);
        } else {
            byte[][] existsKeys = new byte[memberSize][];
            int j=0;
            List<KeyValue> list = new ArrayList<>(memberSize * 2);
            for (Map.Entry<BytesKey, byte[]> entry : memberMap.entrySet()) {
                byte[] member = entry.getKey().getKey();
                byte[] score = entry.getValue();
                byte[] subKey1 = keyStruct.zsetMemberSubKey1(keyMeta, key, member);
                byte[] subKey2 = keyStruct.zsetMemberSubKey2(keyMeta, key, member, score);
                KeyValue keyValue1 = new KeyValue(subKey1, score);
                KeyValue keyValue2 = new KeyValue(subKey2, new byte[0]);
                list.add(keyValue1);
                list.add(keyValue2);
                existsKeys[j] = subKey1;
                j++;
            }
            boolean[] exists = kvClient.exists(existsKeys);
            int existsCount = Utils.count(exists);
            int add = memberSize - existsCount;
            kvClient.batchPut(list);
            if (add > 0) {
                int count = BytesUtils.toInt(keyMeta.getExtra()) + add;
                byte[] extra = BytesUtils.toBytes(count);
                keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
                keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
            }
            return IntegerReply.parse(add);
        }
    }

    private Reply zaddVersion1(KeyMeta keyMeta, byte[] key, boolean first, int memberSize, Map<BytesKey, byte[]> memberMap) {
        if (first) {
            List<KeyValue> list = new ArrayList<>(memberSize);
            for (Map.Entry<BytesKey, byte[]> entry : memberMap.entrySet()) {
                byte[] member = entry.getKey().getKey();
                byte[] score = entry.getValue();
                byte[] subKey1 = keyStruct.zsetMemberSubKey1(keyMeta, key, member);
                KeyValue keyValue1 = new KeyValue(subKey1, score);
                list.add(keyValue1);
            }
            kvClient.batchPut(list);
            return IntegerReply.parse(memberSize);
        } else {
            byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);
            byte[][] args = new byte[memberSize + 2][];
            int i = 0;
            for (Map.Entry<BytesKey, byte[]> entry : memberMap.entrySet()) {
                args[i] = entry.getValue();
                args[i+1] = entry.getKey().getKey();
            }
            Reply reply = sync(cacheRedisTemplate.sendLua(script1, new byte[][]{cacheKey}, args));
            if (reply instanceof ErrorReply) {
                return reply;
            }
            int add = -1;
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                if (replies[0] instanceof BulkReply) {
                    byte[] raw = ((BulkReply) replies[0]).getRaw();
                    if (Utils.bytesToString(raw).equalsIgnoreCase("2")) {
                        if (replies[1] instanceof IntegerReply) {
                            add = (((IntegerReply) replies[1]).getInteger()).intValue();
                        }
                    }
                }
            }
            List<KeyValue> list = new ArrayList<>(memberSize);
            if (add < 0) {
                byte[][] existsKeys = new byte[memberSize][];
                int j = 0;
                for (Map.Entry<BytesKey, byte[]> entry : memberMap.entrySet()) {
                    byte[] member = entry.getKey().getKey();
                    byte[] score = entry.getValue();
                    byte[] subKey1 = keyStruct.zsetMemberSubKey1(keyMeta, key, member);
                    KeyValue keyValue1 = new KeyValue(subKey1, score);
                    list.add(keyValue1);
                    existsKeys[j] = subKey1;
                    j++;
                }
                boolean[] exists = kvClient.exists(existsKeys);
                int existsCount = Utils.count(exists);
                add = memberSize - existsCount;
            } else {
                for (Map.Entry<BytesKey, byte[]> entry : memberMap.entrySet()) {
                    byte[] member = entry.getKey().getKey();
                    byte[] score = entry.getValue();
                    byte[] subKey1 = keyStruct.zsetMemberSubKey1(keyMeta, key, member);
                    KeyValue keyValue1 = new KeyValue(subKey1, score);
                    list.add(keyValue1);
                }
            }
            kvClient.batchPut(list);
            if (add > 0) {
                long count = BytesUtils.toInt(keyMeta.getExtra()) + add;
                byte[] extra = BytesUtils.toBytes(count);
                keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
                keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
            }
            return IntegerReply.parse(add);
        }
    }

    private Reply zaddVersion2(KeyMeta keyMeta, byte[] key, boolean first, int memberSize, Map<BytesKey, byte[]> memberMap) {
        List<KeyValue> list = new ArrayList<>(memberSize*2);
        if (first) {
            for (Map.Entry<BytesKey, byte[]> entry : memberMap.entrySet()) {
                byte[] member = entry.getKey().getKey();
                byte[] score = entry.getValue();
                byte[] subKey1 = keyStruct.zsetMemberSubKey1(keyMeta, key, member);
                KeyValue keyValue1 = new KeyValue(subKey1, score);
                list.add(keyValue1);
                Index index = Index.fromRaw(member);
                if (index.isIndex()) {
                    byte[] subKey2 = keyStruct.zsetIndexSubKey(keyMeta, key, index);
                    KeyValue keyValue2 = new KeyValue(subKey2, member);
                    list.add(keyValue2);
                }
            }
            kvClient.batchPut(list);
            return IntegerReply.parse(memberSize);
        } else {
            List<Command> cmdList = new ArrayList<>();
            byte[][] zsetIndexCmd = new byte[memberSize*2][];
            int i=0;
            for (Map.Entry<BytesKey, byte[]> entry : memberMap.entrySet()) {
                byte[] member = entry.getKey().getKey();
                byte[] score = entry.getValue();
                byte[] subKey1 = keyStruct.zsetMemberSubKey1(keyMeta, key, member);
                KeyValue keyValue1 = new KeyValue(subKey1, score);
                list.add(keyValue1);
                Index index = Index.fromRaw(member);
                if (index.isIndex()) {
                    byte[] subKey2 = keyStruct.zsetIndexSubKey(keyMeta, key, index);
                    KeyValue keyValue2 = new KeyValue(subKey2, member);
                    list.add(keyValue2);
                    byte[] zsetMemberIndexCacheKey = keyStruct.zsetMemberIndexCacheKey(keyMeta, key, index);
                    cmdList.add(new Command(new byte[][]{RedisCommand.PSETEX.raw(), zsetMemberIndexCacheKey, zsetMemberCacheMillis(), member}));
                }
                zsetIndexCmd[i] = score;
                zsetIndexCmd[i+1] = index.getRef();
                i+=2;
            }
            byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);
            Command zsetIndexLuaCmd = cacheRedisTemplate.luaCommand(script1, new byte[][]{cacheKey}, zsetIndexCmd);
            List<Command> commands = new ArrayList<>(cmdList.size() + 1);
            commands.add(zsetIndexLuaCmd);
            commands.addAll(cmdList);
            List<Reply> replyList = sync(cacheRedisTemplate.sendCommand(commands));
            Reply reply = replyList.get(0);
            int add = -1;
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                byte[] raw = ((BulkReply) replies[0]).getRaw();
                if (Utils.bytesToString(raw).equalsIgnoreCase("2")) {
                    if (replies[1] instanceof IntegerReply) {
                        add = ((IntegerReply) replies[1]).getInteger().intValue();
                    }
                }
            }
            if (add < 0) {
                byte[][] existsKeys = new byte[memberSize][];
                int j = 0;
                for (Map.Entry<BytesKey, byte[]> entry : memberMap.entrySet()) {
                    byte[] member = entry.getKey().getKey();
                    byte[] subKey1 = keyStruct.zsetMemberSubKey1(keyMeta, key, member);
                    existsKeys[j] = subKey1;
                    j++;
                }
                boolean[] exists = kvClient.exists(existsKeys);
                int existsCount = Utils.count(exists);
                add = memberSize - existsCount;
            }
            if (add > 0) {
                long count = BytesUtils.toInt(keyMeta.getExtra()) + add;
                byte[] extra = BytesUtils.toBytes(count);
                keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
                keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
            }
            kvClient.batchPut(list);
            return IntegerReply.parse(add);
        }
    }

    private Reply zaddVersion3(KeyMeta keyMeta, byte[] key, int memberSize, Map<BytesKey, byte[]> memberMap) {
        byte[][] rewriteCmd = new byte[memberSize*2+2][];
        rewriteCmd[0] = RedisCommand.ZADD.raw();
        rewriteCmd[1] = keyStruct.cacheKey(keyMeta, key);
        int i=2;
        List<KeyValue> list = new ArrayList<>(memberSize);
        List<Command> memberIndexCacheWriteCommands = new ArrayList<>();
        for (Map.Entry<BytesKey, byte[]> entry : memberMap.entrySet()) {
            byte[] member = entry.getKey().getKey();
            Index index = Index.fromRaw(member);
            if (index.isIndex()) {
                byte[] indexSubKey = keyStruct.zsetIndexSubKey(keyMeta, key, index);
                list.add(new KeyValue(indexSubKey, member));
                byte[] zsetMemberIndexCacheKey = keyStruct.zsetMemberIndexCacheKey(keyMeta, key, index);
                memberIndexCacheWriteCommands.add(new Command(new byte[][]{RedisCommand.PSETEX.raw(), zsetMemberIndexCacheKey, zsetMemberCacheMillis(), member}));
            }
            rewriteCmd[i] = entry.getValue();
            rewriteCmd[i+1] = index.getRef();
            i+=2;
        }
        if (!list.isEmpty()) {
            kvClient.batchPut(list);
        }
        if (!memberIndexCacheWriteCommands.isEmpty()) {
            List<Reply> replyList = sync(cacheRedisTemplate.sendCommand(memberIndexCacheWriteCommands));
            for (Reply reply : replyList) {
                if (reply instanceof ErrorReply) {
                    return reply;
                }
            }
        }
        return sync(storeRedisTemplate.sendCommand(new Command(rewriteCmd)));
    }

    private byte[] zsetMemberCacheMillis() {
        return Utils.stringToBytes(String.valueOf(cacheConfig.zsetMemberCacheMillis()));
    }


}
