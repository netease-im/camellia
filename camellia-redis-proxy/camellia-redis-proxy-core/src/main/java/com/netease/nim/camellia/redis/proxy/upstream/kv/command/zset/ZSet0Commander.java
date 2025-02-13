package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.monitor.KvCacheMonitor;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSetIndexLRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetTuple;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.Index;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.*;

/**
 * Created by caojiajun on 2024/6/3
 */
public abstract class ZSet0Commander extends Commander {

    private byte[] zsetMemberCacheMillis;

    public ZSet0Commander(CommanderConfig commanderConfig) {
        super(commanderConfig);
        zsetMemberCacheMillis = Utils.stringToBytes(String.valueOf(cacheConfig.zsetMemberCacheMillis()));
        ProxyDynamicConf.registerCallback(() -> zsetMemberCacheMillis = Utils.stringToBytes(String.valueOf(cacheConfig.zsetMemberCacheMillis())));
    }

    protected final RedisZSet loadLRUCache(int slot, KeyMeta keyMeta, byte[] key) {
        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0) {
            List<ZSetTuple> list = zrangeAllFromKv(slot, keyMeta, key);
            Map<BytesKey, Double> memberMap = new HashMap<>(list.size());
            for (ZSetTuple tuple : list) {
                memberMap.put(tuple.getMember(), tuple.getScore());
            }
            return new RedisZSet(memberMap);
        }
        if (encodeVersion == EncodeVersion.version_1) {
            byte[][] objects = new byte[5][];
            objects[0] = RedisCommand.ZRANGE.raw();
            objects[1] = keyDesign.cacheKey(keyMeta, key);
            objects[2] = Utils.stringToBytes("0");
            objects[3] = Utils.stringToBytes("-1");
            objects[4] = RedisKeyword.WITHSCORES.getRaw();
            Reply reply = sync(storageRedisTemplate.sendCommand(new Command(objects)));
            if (reply instanceof MultiBulkReply) {
                //
                boolean forRead = redisCommand().getType() == RedisCommand.Type.READ;
                reply = checkReplyWithIndex(slot, keyMeta, key, (MultiBulkReply) reply, true, forRead);
                //
                if (reply instanceof MultiBulkReply) {
                    Reply[] replies = ((MultiBulkReply) reply).getReplies();
                    Map<BytesKey, Double> memberMap = new HashMap<>(replies.length / 2);
                    for (int i = 0; i < replies.length; i += 2) {
                        BulkReply member = (BulkReply) replies[i];
                        BulkReply score = (BulkReply) replies[i+1];
                        memberMap.put(new BytesKey(member.getRaw()), Double.parseDouble(Utils.bytesToString(score.getRaw())));
                    }
                    return new RedisZSet(memberMap);
                }
            }
        }
        return null;
    }

    protected final Reply zrangeVersion1(int slot, KeyMeta keyMeta, byte[] key, byte[] cacheKey, byte[][] objects, boolean withScores) {
        byte[][] cmd = new byte[objects.length][];
        System.arraycopy(objects, 0, cmd, 0, objects.length);
        cmd[1] = cacheKey;

        Reply reply = sync(storageRedisTemplate.sendCommand(new Command(cmd)));
        if (reply instanceof ErrorReply) {
            return reply;
        }
        if (reply instanceof MultiBulkReply) {
            return checkReplyWithIndex(slot, keyMeta, key, (MultiBulkReply) reply, withScores, true);
        }
        return ErrorReply.INTERNAL_ERROR;
    }

    protected final List<ZSetTuple> zrangeAllFromKv(int slot, KeyMeta keyMeta, byte[] key) {
        List<ZSetTuple> list = new ArrayList<>();
        byte[] startKey = keyDesign.zsetMemberSubKey1(keyMeta, key, new byte[0]);
        byte[] prefix = startKey;
        int limit = kvConfig.scanBatch();
        int zsetMaxSize = kvConfig.zsetMaxSize();
        while (true) {
            List<KeyValue> scan = kvClient.scanByPrefix(slot, startKey, prefix, limit, Sort.ASC, false);
            if (scan.isEmpty()) {
                return list;
            }
            for (KeyValue keyValue : scan) {
                if (keyValue == null || keyValue.getKey() == null) {
                    continue;
                }
                startKey = keyValue.getKey();
                byte[] member = keyDesign.decodeZSetMemberBySubKey1(keyValue.getKey(), key);
                list.add(new ZSetTuple(new BytesKey(member), Utils.bytesToDouble(keyValue.getValue())));
            }
            if (scan.size() < limit) {
                return list;
            }
            //
            if (list.size() >= zsetMaxSize) {
                ErrorLogCollector.collect(ZSet0Commander.class, "redis.zset.size exceed " + zsetMaxSize + ", key = " + Utils.bytesToString(key));
                return list;
            }
        }
    }

    protected final byte[] zsetMemberCacheMillis() {
        return zsetMemberCacheMillis;
    }

    private Reply checkReplyWithIndex(int slot, KeyMeta keyMeta, byte[] key, MultiBulkReply reply, boolean withScores, boolean forRead) {
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

        int indexTotal = missingMemberMap.size();

        byte[] cacheKey = keyDesign.cacheKey(keyMeta, key);

        boolean localCacheEnable = cacheConfig.isZSetLocalCacheEnable();

        int localCacheHit = 0;

        if (localCacheEnable) {
            ZSetIndexLRUCache lruCache = cacheConfig.getZSetIndexLRUCache();
            if (!missingMemberMap.isEmpty()) {
                Set<BytesKey> set = new HashSet<>(missingMemberMap.keySet());
                for (BytesKey ref : set) {
                    byte[] raw;
                    if (forRead) {
                        raw = lruCache.getForRead(slot, cacheKey, ref);
                    } else {
                        raw = lruCache.getForWrite(slot, cacheKey, ref);
                    }
                    if (raw != null && raw.length > 0) {
                        memberMap.put(ref, raw);
                        missingMemberMap.remove(ref);
                        localCacheHit ++;
                    }
                }
            }
        }

        int redisCacheHit = 0;

        if (!missingMemberMap.isEmpty()) {
            BytesKey[] bytesKeys = missingMemberMap.keySet().toArray(new BytesKey[0]);
            List<Command> commandList = new ArrayList<>(bytesKeys.length * 2);
            for (BytesKey bytesKey : bytesKeys) {
                Index index = missingMemberMap.get(bytesKey);
                if (index == null) continue;
                byte[] memberCacheKey = keyDesign.zsetMemberIndexCacheKey(keyMeta, key, index);
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
                        redisCacheHit ++;
                        memberMap.put(member, raw);
                        missingMemberMap.remove(member);
                        //
                        if (localCacheEnable) {
                            ZSetIndexLRUCache lruCache = cacheConfig.getZSetIndexLRUCache();
                            if (forRead) {
                                lruCache.putForRead(slot, cacheKey, member, raw);
                            } else {
                                lruCache.putForWrite(slot, cacheKey, member, raw);
                            }
                        }
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
                byte[] subKey = keyDesign.zsetIndexSubKey(keyMeta, key, index);
                batchGetKeys[i] = subKey;
                i++;
                reverseMap.put(new BytesKey(subKey), entry.getKey());
            }
            List<Command> buildCacheCommands = new ArrayList<>(batchGetKeys.length);
            List<KeyValue> keyValues = kvClient.batchGet(slot, batchGetKeys);
            for (KeyValue keyValue : keyValues) {
                BytesKey bytesKey = reverseMap.get(new BytesKey(keyValue.getKey()));
                if (bytesKey == null) continue;
                memberMap.put(bytesKey, keyValue.getValue());

                //
                Index index = missingMemberMap.get(bytesKey);
                byte[] memberCacheKey = keyDesign.zsetMemberIndexCacheKey(keyMeta, key, index);
                buildCacheCommands.add(new Command(new byte[][]{RedisCommand.PSETEX.raw(), memberCacheKey, zsetMemberCacheMillis(), keyValue.getValue()}));
                //
                if (localCacheEnable) {
                    ZSetIndexLRUCache lruCache = cacheConfig.getZSetIndexLRUCache();
                    if (forRead) {
                        lruCache.putForRead(slot, cacheKey, bytesKey, keyValue.getValue());
                    } else {
                        lruCache.putForWrite(slot, cacheKey, bytesKey, keyValue.getValue());
                    }
                }

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

        KvCacheMonitor.localCache(cacheConfig.getNamespace(), "zset_index", localCacheHit);
        KvCacheMonitor.redisCache(cacheConfig.getNamespace(), "zset_index", redisCacheHit);
        KvCacheMonitor.kvStore(cacheConfig.getNamespace(), "zset_index", indexTotal - localCacheHit - redisCacheHit);

        if (!missingMemberMap.isEmpty()) {
            ErrorLogCollector.collect(ZSet0Commander.class, "zrange kv index missing");
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
                    list.add(new BulkReply(bytes));
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
                    list.add(new BulkReply(bytes));
                }
            }
        }
        return new MultiBulkReply(list.toArray(new Reply[0]));
    }
}
