package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * ZREMRANGEBYRANK key start stop
 * <p>
 * Created by caojiajun on 2024/5/8
 */
public class ZRemRangeByRankCommander extends ZRemRange0Commander {

    private static final byte[] script = ("local ret1 = redis.call('exists', KEYS[1]);\n" +
            "if ret1 then\n" +
            "  local ret = redis.call('zrange', KEYS[1], unpack(ARGV));\n" +
            "  return {'2', ret};\n" +
            "end\n" +
            "return {'1'};").getBytes(StandardCharsets.UTF_8);

    public ZRemRangeByRankCommander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    @Override
    public RedisCommand redisCommand() {
        return RedisCommand.ZREMRANGEBYRANK;
    }

    @Override
    protected boolean parse(Command command) {
        byte[][] objects = command.getObjects();
        return objects.length == 4;
    }

    @Override
    protected Reply execute(Command command) {
        byte[][] objects = command.getObjects();
        byte[] key = objects[1];
        KeyMeta keyMeta = keyMetaServer.getKeyMeta(key);
        if (keyMeta == null) {
            return IntegerReply.REPLY_0;
        }
        if (keyMeta.getKeyType() != KeyType.zset) {
            return ErrorReply.WRONG_TYPE;
        }
        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0) {
            return zremrangeByRank(keyMeta, key, objects);
        }
        byte[][] zrangeArgs = new byte[objects.length][];
        System.arraycopy(objects, 0, zrangeArgs, 0, zrangeArgs.length);
        zrangeArgs[0] = RedisCommand.ZRANGE.raw();
        if (encodeVersion == EncodeVersion.version_1) {
            return zremrangeVersion1(keyMeta, key, zrangeArgs, script);
        }
        if (encodeVersion == EncodeVersion.version_2) {
            return zremrangeVersion2(keyMeta, key, zrangeArgs, script);
        }
        if (encodeVersion == EncodeVersion.version_3) {
            return zremrangeVersion3(keyMeta, key, zrangeArgs);
        }
        return ErrorReply.INTERNAL_ERROR;
    }

    private Reply zremrangeByRank(KeyMeta keyMeta, byte[] key, byte[][] objects) {
        int start = (int) Utils.bytesToNum(objects[2]);
        int stop = (int) Utils.bytesToNum(objects[3]);

        int size = BytesUtils.toInt(keyMeta.getExtra());

        if (start < 0) {
            start += size;
        }
        if (stop < 0) {
            stop += size;
        }
        if (start < 0) {
            start = 0;
        }
        if (stop < 0 || start > stop) {
            return MultiBulkReply.EMPTY;
        }
        byte[] startKey = keyDesign.zsetMemberSubKey1(keyMeta, key, new byte[0]);
        int ret = zremrangeByRank0(keyMeta, key, startKey, startKey, start, stop);
        if (ret > 0) {
            size = size - ret;
            updateKeyMeta(keyMeta, key, size);
        }
        return IntegerReply.parse(ret);
    }

    private int zremrangeByRank0(KeyMeta keyMeta, byte[] key, byte[] startKey, byte[] prefix, int start, int stop) {
        int targetSize = stop - start;
        List<byte[]> list = new ArrayList<>(Math.min(targetSize, 512));
        int scanBatch = kvConfig.scanBatch();
        int count = 0;
        while (true) {
            int limit = Math.min(targetSize - list.size(), scanBatch);
            List<KeyValue> scan = kvClient.scanByPrefix(startKey, prefix, limit, Sort.ASC, false);
            if (scan.isEmpty()) {
                return deleteKv(list);
            }
            for (KeyValue keyValue : scan) {
                if (keyValue == null || keyValue.getValue() == null) {
                    continue;
                }
                startKey = keyValue.getKey();
                if (count >= start) {
                    byte[] member = keyDesign.decodeZSetMemberBySubKey1(keyValue.getKey(), key);
                    byte[] score = Utils.doubleToBytes(BytesUtils.toDouble(keyValue.getValue()));
                    list.add(keyValue.getKey());
                    list.add(keyDesign.zsetMemberSubKey2(keyMeta, key, member, score));
                }
                if (count >= stop) {
                    return deleteKv(list);
                }
                count++;
            }
            if (scan.size() < limit) {
                return deleteKv(list);
            }
        }
    }

    private int deleteKv(List<byte[]> list) {
        kvClient.batchDelete(list.toArray(new byte[0][0]));
        return list.size() / 2;
    }
}
