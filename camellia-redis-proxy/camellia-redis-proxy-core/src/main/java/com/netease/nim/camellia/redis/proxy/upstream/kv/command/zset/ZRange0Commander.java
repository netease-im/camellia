package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.Index;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2024/5/7
 */
public abstract class ZRange0Commander extends Commander {

    public ZRange0Commander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    protected final Reply zrangeVersion1(KeyMeta keyMeta, byte[] key, byte[][] objects, byte[] script) {
        byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);
        byte[][] args = new byte[objects.length - 2][];
        System.arraycopy(objects, 2, args, 0, args.length);
        Reply reply = zrangeFromRedis(cacheKey, script, args);
        if (reply != null) {
            return reply;
        }
        List<ZSetTuple> tuples = zrangeAllFromKv(keyMeta, key);
        byte[][] cmd = new byte[tuples.size() + 2][];
        cmd[0] = RedisCommand.ZADD.raw();
        cmd[1] = cacheKey;
        int i = 2;
        for (ZSetTuple tuple : tuples) {
            cmd[i] = tuple.getScore().getKey();
            cmd[i + 1] = tuple.getMember().getKey();
            i += 2;
        }
        Command zaddCmd = new Command(cmd);
        Command pexireCmd = new Command(new byte[][]{RedisCommand.PEXPIRE.raw(), cacheKey, zsetRangeCacheMillis()});
        List<Command> list = new ArrayList<>(2);
        list.add(zaddCmd);
        list.add(pexireCmd);
        List<Reply> replyList = sync(cacheRedisTemplate.sendCommand(list));
        for (Reply reply1 : replyList) {
            if (reply1 instanceof ErrorReply) {
                return reply1;
            }
        }
        reply = zrangeFromRedis(cacheKey, script, args);
        if (reply != null) {
            return reply;
        }
        return ErrorReply.INTERNAL_ERROR;
    }

    protected final Reply zrangeVersion2(KeyMeta keyMeta, byte[] key, byte[][] objects, boolean withScores, byte[] script) {
        byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);
        byte[][] args = new byte[objects.length - 2][];
        System.arraycopy(objects, 2, args, 0, args.length);
        Reply reply = zrangeFromRedis(cacheKey, script, args);
        if (reply != null) {
            if (reply instanceof MultiBulkReply) {
                return checkReplyWithIndex(keyMeta, key, (MultiBulkReply) reply, withScores);
            }
            return reply;
        }
        List<ZSetTuple> tuples = zrangeAllFromKv(keyMeta, key);
        byte[][] cmd = new byte[tuples.size() + 2][];
        cmd[0] = RedisCommand.ZADD.raw();
        cmd[1] = cacheKey;
        List<Command> refCommands = new ArrayList<>();
        int i = 2;
        for (ZSetTuple tuple : tuples) {
            cmd[i] = tuple.getScore().getKey();
            byte[] member = tuple.getMember().getKey();
            Index index = Index.fromRaw(member);
            cmd[i + 1] = index.getRef();
            if (index.isIndex()) {
                byte[] zsetMemberIndexCacheKey = keyStruct.zsetMemberIndexCacheKey(keyMeta, key, index);
                refCommands.add(new Command(new byte[][]{RedisCommand.PSETEX.raw(), zsetMemberIndexCacheKey, zsetMemberCacheMillis(), member}));
            }
            i += 2;
        }
        Command zaddCmd = new Command(cmd);
        Command pexireCmd = new Command(new byte[][]{RedisCommand.PEXPIRE.raw(), cacheKey, zsetRangeCacheMillis()});
        List<Command> list = new ArrayList<>(2+refCommands.size());
        list.add(zaddCmd);
        list.add(pexireCmd);
        list.addAll(refCommands);
        List<Reply> replyList = sync(cacheRedisTemplate.sendCommand(list));
        for (Reply reply1 : replyList) {
            if (reply1 instanceof ErrorReply) {
                return reply1;
            }
        }
        reply = zrangeFromRedis(cacheKey, script, args);
        if (reply != null) {
            if (reply instanceof MultiBulkReply) {
                return checkReplyWithIndex(keyMeta, key, (MultiBulkReply) reply, withScores);
            }
            return reply;
        }
        return ErrorReply.INTERNAL_ERROR;
    }

    protected final Reply zrangeVersion3(KeyMeta keyMeta, byte[] key, byte[][] objects, boolean withScores) {
        byte[] cacheKey = keyStruct.cacheKey(keyMeta, key);
        byte[][] cmd = new byte[objects.length][];
        cmd[0] = objects[0];
        cmd[1] = cacheKey;
        System.arraycopy(objects, 2, cmd, 2, objects.length - 2);
        Reply reply = sync(storeRedisTemplate.sendCommand(new Command(cmd)));
        if (reply instanceof ErrorReply) {
            return reply;
        }
        if (reply instanceof MultiBulkReply) {
            return checkReplyWithIndex(keyMeta, key, (MultiBulkReply) reply, withScores);
        }
        return ErrorReply.INTERNAL_ERROR;
    }

    protected final byte[] zsetMemberCacheMillis() {
        return Utils.stringToBytes(String.valueOf(cacheConfig.zsetMemberCacheMillis()));
    }

    protected final byte[] zsetRangeCacheMillis() {
        return Utils.stringToBytes(String.valueOf(cacheConfig.zsetRangeCacheMillis()));
    }

    protected final List<ZSetTuple> zrangeAllFromKv(KeyMeta keyMeta, byte[] key) {
        List<ZSetTuple> list = new ArrayList<>();
        byte[] startKey = keyStruct.zsetMemberSubKey1(keyMeta, key, new byte[0]);
        byte[] prefix = startKey;
        int limit = kvConfig.scanBatch();
        int zsetMaxSize = kvConfig.zsetMaxSize();
        while (true) {
            List<KeyValue> scan = kvClient.scan(startKey, prefix, limit, Sort.ASC, false);
            if (scan.isEmpty()) {
                break;
            }
            for (KeyValue keyValue : scan) {
                byte[] member = keyStruct.decodeZSetMemberBySubKey1(keyValue.getKey(), key);
                list.add(new ZSetTuple(new BytesKey(member), new BytesKey(keyValue.getValue())));
                startKey = keyValue.getKey();
                if (list.size() >= zsetMaxSize) {
                    break;
                }
            }
            if (scan.size() < limit) {
                break;
            }
        }
        return list;
    }

    protected final Reply zrangeFromRedis(byte[] cacheKey, byte[] script, byte[][] args) {
        Reply reply = sync(cacheRedisTemplate.sendLua(script, new byte[][]{cacheKey}, args));
        if (reply instanceof ErrorReply) {
            return reply;
        }
        if (reply instanceof MultiBulkReply) {
            Reply[] replies = ((MultiBulkReply) reply).getReplies();
            if (replies[0] instanceof BulkReply) {
                byte[] raw = ((BulkReply) replies[0]).getRaw();
                if (Utils.bytesToString(raw).equalsIgnoreCase("2")) {
                    return replies[1];
                }
            }
        }
        return null;
    }

    protected final Reply checkReplyWithIndex(KeyMeta keyMeta, byte[] key, MultiBulkReply reply, boolean withScores) {
        Reply[] replies = reply.getReplies();
        int step = withScores ? 2: 1;
        int size = withScores ? replies.length/2 : replies.length;
        Map<BytesKey, byte[]> memberMap = new HashMap<>(size);
        Map<BytesKey, Index> missingMemberMap = new HashMap<>();
        for (int i=0; i<replies.length; i+=step) {
            Reply reply1 = replies[i];
            if (reply1 instanceof BulkReply) {
                byte[] ref = ((BulkReply) reply1).getRaw();
                Index index = Index.fromRef(ref);
                if (!index.isIndex()) {
                    memberMap.put(new BytesKey(ref), index.getRaw());
                } else {
                    missingMemberMap.put(new BytesKey(ref), index);
                }
            }
        }
        if (!missingMemberMap.isEmpty()) {
            BytesKey[] bytesKeys = missingMemberMap.keySet().toArray(new BytesKey[0]);
            List<Command> commandList = new ArrayList<>(bytesKeys.length);
            for (BytesKey bytesKey : bytesKeys) {
                Index index = missingMemberMap.get(bytesKey);
                byte[] memberCacheKey = keyStruct.zsetMemberIndexCacheKey(keyMeta, key, index);
                Command cmd1 = new Command(new byte[][]{RedisCommand.GET.raw(), memberCacheKey});
                Command cmd2 = new Command(new byte[][]{RedisCommand.PEXPIRE.raw(), memberCacheKey, zsetMemberCacheMillis()});
                commandList.add(cmd1);
                commandList.add(cmd2);
            }
            List<Reply> replyList = sync(cacheRedisTemplate.sendCommand(commandList));
            for (int i = 0; i < replyList.size(); i++) {
                Reply subReply = replyList.get(i);
                if (subReply instanceof ErrorReply) {
                    return reply;
                }
                if (i % 2 == 1) {
                    continue;
                }
                BytesKey member = bytesKeys[i/2];
                if (subReply instanceof BulkReply) {
                    byte[] raw = ((BulkReply) subReply).getRaw();
                    if (raw != null && raw.length > 0) {
                        memberMap.put(member, raw);
                        missingMemberMap.remove(member);
                    }
                }
            }
        }
        if (!missingMemberMap.isEmpty()) {
            byte[][] batchGetKeys = new byte[missingMemberMap.size()][];
            int i=0;
            Map<BytesKey, BytesKey> reverseMap = new HashMap<>(missingMemberMap.size());
            for (Map.Entry<BytesKey, Index> entry : missingMemberMap.entrySet()) {
                Index index = entry.getValue();
                byte[] subKey = keyStruct.zsetIndexSubKey(keyMeta, key, index);
                batchGetKeys[i] = subKey;
                i++;
                reverseMap.put(new BytesKey(subKey), entry.getKey());
            }
            List<Command> buildCacheCommands = new ArrayList<>(batchGetKeys.length);
            List<KeyValue> keyValues = kvClient.batchGet(batchGetKeys);
            for (KeyValue keyValue : keyValues) {
                BytesKey bytesKey = reverseMap.get(new BytesKey(keyValue.getKey()));
                memberMap.put(bytesKey, keyValue.getValue());

                Index index = missingMemberMap.get(bytesKey);
                byte[] memberCacheKey = keyStruct.zsetMemberIndexCacheKey(keyMeta, key, index);
                buildCacheCommands.add(new Command(new byte[][]{RedisCommand.PSETEX.raw(), memberCacheKey, zsetMemberCacheMillis(), keyValue.getValue()}));

                missingMemberMap.remove(bytesKey);
            }
            if (!buildCacheCommands.isEmpty()) {
                List<Reply> replyList = sync(cacheRedisTemplate.sendCommand(buildCacheCommands));
                for (Reply reply1 : replyList) {
                    if (reply1 instanceof ErrorReply) {
                        return reply1;
                    }
                }
            }
        }
        if (!missingMemberMap.isEmpty()) {
            ErrorLogCollector.collect(ZRange0Commander.class, "zrange kv index missing");
        }
        List<Reply> list = new ArrayList<>(replies.length);
        if (withScores) {
            for (int i=0; i<replies.length; i+=2) {
                Reply memberReply = replies[i];
                Reply scoreReply = replies[i+1];
                if (memberReply instanceof BulkReply) {
                    byte[] data = ((BulkReply) memberReply).getRaw();
                    byte[] bytes = memberMap.get(new BytesKey(data));
                    if (bytes == null) {
                        continue;
                    }
                    list.add(memberReply);
                    list.add(scoreReply);
                }
            }
        } else {
            for (Reply reply1 : replies) {
                if (reply1 instanceof BulkReply) {
                    byte[] data = ((BulkReply) reply1).getRaw();
                    byte[] bytes = memberMap.get(new BytesKey(data));
                    if (bytes == null) {
                        continue;
                    }
                    list.add(reply1);
                }
            }
        }
        return new MultiBulkReply(list.toArray(new Reply[0]));
    }

}
