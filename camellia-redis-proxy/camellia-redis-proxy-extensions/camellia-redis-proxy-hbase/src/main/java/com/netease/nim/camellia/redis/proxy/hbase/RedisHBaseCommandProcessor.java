package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.hbase.monitor.RedisHBaseMonitor;
import com.netease.nim.camellia.redis.proxy.hbase.util.ParamUtils;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2020/2/25.
 */
public class RedisHBaseCommandProcessor implements IRedisHBaseCommandProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RedisHBaseCommandProcessor.class);
    private final RedisHBaseZSetMixClient zSetMixClient;
    private final RedisHBaseCommonMixClient commonMixClient;
    private final RedisHBaseStringMixClient stringMixClient;
    private final RedisHBaseHashMixClient hashMixClient;

    public RedisHBaseCommandProcessor(CamelliaRedisTemplate redisTemplate, CamelliaHBaseTemplate hBaseTemplate) {
        //做一下预热
        try {
            redisTemplate.exists("warm");
            hBaseTemplate.get(RedisHBaseConfiguration.hbaseTableName(), new Get(Bytes.toBytes("warm")));
        } catch (Exception e) {
            logger.error("warm fail", e);
            throw new IllegalArgumentException("warm fail, please check", e);
        }

        HBaseAsyncWriteExecutor hBaseAsyncWriteExecutor = new HBaseAsyncWriteExecutor(hBaseTemplate,
                RedisHBaseConfiguration.hbaseAsyncWritePoolSize(), RedisHBaseConfiguration::hbaseAsyncWriteQueueSize);
        //
        this.zSetMixClient = new RedisHBaseZSetMixClient(redisTemplate, hBaseTemplate, hBaseAsyncWriteExecutor);
        this.stringMixClient = new RedisHBaseStringMixClient(redisTemplate, hBaseTemplate, hBaseAsyncWriteExecutor);
        this.hashMixClient = new RedisHBaseHashMixClient(redisTemplate, hBaseTemplate, hBaseAsyncWriteExecutor);
        this.commonMixClient = new RedisHBaseCommonMixClient(redisTemplate, this.zSetMixClient, this.stringMixClient, this.hashMixClient);
    }

    @Override
    public StatusReply ping() {
        return StatusReply.PONG;
    }

    @Override
    public BulkReply echo(byte[] echo) {
        return new BulkReply(echo);
    }

    @Override
    public IntegerReply del(byte[][] keys) {
        if (keys == null || keys.length == 0) {
            throw Utils.illegalArgumentException();
        }
        Long del = commonMixClient.del(keys);
        return new IntegerReply(del);
    }

    @Override
    public IntegerReply exists(byte[][] keys) {
        Long exists = commonMixClient.exists(keys);
        return new IntegerReply(exists);
    }

    @Override
    public StatusReply type(byte[] key) {
        String type = commonMixClient.type(key);
        return new StatusReply(type);
    }

    @Override
    public IntegerReply expire(byte[] key, byte[] seconds) {
        Long expire = commonMixClient.expire(key, (int) Utils.bytesToNum(seconds));
        return new IntegerReply(expire);
    }

    @Override
    public IntegerReply pexpire(byte[] key, byte[] millis) {
        Long expire = commonMixClient.pexpire(key, Utils.bytesToNum(millis));
        return new IntegerReply(expire);
    }

    @Override
    public IntegerReply expireat(byte[] key, byte[] timestamp) {
        Long expireAt = commonMixClient.expireAt(key, Utils.bytesToNum(timestamp));
        return new IntegerReply(expireAt);
    }

    @Override
    public IntegerReply pexpireat(byte[] key, byte[] timestamp) {
        Long pexpireAt = commonMixClient.pexpireAt(key, Utils.bytesToNum(timestamp));
        return new IntegerReply(pexpireAt);
    }

    @Override
    public IntegerReply ttl(byte[] key) {
        Long ttl = commonMixClient.ttl(key);
        return new IntegerReply(ttl);
    }

    @Override
    public IntegerReply pttl(byte[] key) {
        Long pttl = commonMixClient.pttl(key);
        return new IntegerReply(pttl);
    }

    @Override
    public BulkReply get(byte[] key) {
        byte[] bytes = stringMixClient.get(key);
        return new BulkReply(bytes);
    }

    @Override
    public Reply set(byte[] key, byte[] value1, byte[][] args) {
        if (args == null || args.length == 0) {
            String set = stringMixClient.set(key, value1);
            return new StatusReply(set);
        }
        String nxxx = null;
        String expx = null;
        Long time = null;
        boolean needTime = false;
        for (byte[] arg : args) {
            if (needTime) {
                time = Utils.bytesToNum(arg);
                needTime = false;
                continue;
            }
            String argStr = new String(arg, Utils.utf8Charset);
            if (argStr.equalsIgnoreCase(RedisKeyword.NX.name())) {
                nxxx = RedisKeyword.NX.name();
            } else if (argStr.equalsIgnoreCase(RedisKeyword.XX.name())) {
                nxxx = RedisKeyword.XX.name();
            } else if (argStr.equalsIgnoreCase(RedisKeyword.EX.name())) {
                expx = RedisKeyword.EX.name();
                needTime = true;
            } else if (argStr.equalsIgnoreCase(RedisKeyword.PX.name())) {
                expx = RedisKeyword.PX.name();
                needTime = true;
            } else {
                return ErrorReply.SYNTAX_ERROR;
            }
        }
        if (needTime) {
            return ErrorReply.SYNTAX_ERROR;
        }
        if (nxxx != null && expx != null) {
            return Utils.commandNotSupport(RedisCommand.SET);
        } else if (nxxx != null) {
            return Utils.commandNotSupport(RedisCommand.SET);
        } else {
            if (expx.equalsIgnoreCase(RedisKeyword.EX.name())) {
                String setex = stringMixClient.setex(key, Math.toIntExact(time), value1);
                if (setex == null) return BulkReply.NIL_REPLY;
                return new StatusReply(setex);
            } else {
                String psetex = stringMixClient.psetex(key, time, value1);
                if (psetex == null) return BulkReply.NIL_REPLY;
                return new StatusReply(psetex);
            }
        }
    }

    @Override
    public StatusReply setex(byte[] key, byte[] seconds, byte[] value2) {
        String value = stringMixClient.setex(key, (int) Utils.bytesToNum(seconds), value2);
        return new StatusReply(value);
    }

    @Override
    public StatusReply mset(byte[][] kvs) {
        if (kvs == null || kvs.length % 2 != 0) {
            throw Utils.illegalArgumentException();
        }
        String mset = stringMixClient.mset(kvs);
        return new StatusReply(mset);
    }

    @Override
    public StatusReply psetex(byte[] key, byte[] millis, byte[] value2) {
        String value = stringMixClient.psetex(key, Utils.bytesToNum(millis), value2);
        return new StatusReply(value);
    }

    @Override
    public MultiBulkReply mget(byte[][] keys) {
        List<byte[]> mget = stringMixClient.mget(keys);
        Reply[] replies = new Reply[mget.size()];
        for (int i = 0; i< keys.length; i++) {
            byte[] value = mget.get(i);
            if (value == null) {
                replies[i] = BulkReply.NIL_REPLY;
            } else {
                replies[i] = new BulkReply(value);
            }
        }
        return new MultiBulkReply(replies);
    }

    @Override
    public IntegerReply zadd(byte[] key, byte[][] args) {
        if (args.length < 2) {
            throw Utils.illegalArgumentException();
        }
        for (byte[] arg : args) {
            if (Utils.checkStringIgnoreCase(arg, RedisKeyword.NX.name())) {
                throw new UnsupportedOperationException("not support nx");
            } else if (Utils.checkStringIgnoreCase(arg, RedisKeyword.XX.name())) {
                throw new UnsupportedOperationException("not support xx");
            } else if (Utils.checkStringIgnoreCase(arg, RedisKeyword.CH.name())) {
                throw new UnsupportedOperationException("not support ch");
            }
        }
        if (args.length % 2 != 0) {
            throw Utils.illegalArgumentException();
        }
        Map<byte[], Double> scoreMembers = new HashMap<>();
        for (int i=0; i<args.length; i=i+2) {
            double score = Utils.bytesToDouble(args[i]);
            byte[] value = args[i+1];
            scoreMembers.put(value, score);
        }
        Long zadd = zSetMixClient.zadd(key, scoreMembers);
        return new IntegerReply(zadd);
    }

    @Override
    public BulkReply zscore(byte[] key, byte[] member) {
        Double zscore = zSetMixClient.zscore(key, member);
        return new BulkReply(Utils.doubleToBytes(zscore));
    }

    @Override
    public BulkReply zincrby(byte[] key, byte[] increment, byte[] member) {
        Double zincrby = zSetMixClient.zincrby(key, Utils.bytesToDouble(increment), member);
        return new BulkReply(Utils.doubleToBytes(zincrby));
    }

    @Override
    public IntegerReply zcard(byte[] key) {
        Long zcard = zSetMixClient.zcard(key);
        RedisHBaseMonitor.incrCollectionSize("zcard", zcard == null ? 0 : zcard);
        return new IntegerReply(zcard);
    }

    @Override
    public IntegerReply zcount(byte[] key, byte[] min, byte[] max) {
        Long zcount = zSetMixClient.zcount(key, min, max);
        RedisHBaseMonitor.incrCollectionSize("zcount", zcount == null ? 0 : zcount);
        return new IntegerReply(zcount);
    }

    @Override
    public MultiBulkReply zrange(byte[] key, byte[] start, byte[] stop, byte[] withscores) {
        if (withscores != null) {
            if (Utils.checkStringIgnoreCase(withscores, RedisKeyword.WITHSCORES.name())) {
                Set<Tuple> tuples = zSetMixClient.zrangeWithScores(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
                RedisHBaseMonitor.incrCollectionSize("zrangeWithScores", tuples == null ? 0 : tuples.size());
                return ParamUtils.tuples2MultiBulkReply(tuples);
            }
            throw new IllegalArgumentException(Utils.syntaxError);
        } else {
            Set<byte[]> zrange = zSetMixClient.zrange(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
            RedisHBaseMonitor.incrCollectionSize("zrange", zrange == null ? 0 : zrange.size());
            return ParamUtils.collection2MultiBulkReply(zrange);
        }
    }

    @Override
    public MultiBulkReply zrevrange(byte[] key, byte[] start, byte[] stop, byte[] withscores) {
        if (withscores != null) {
            if (Utils.checkStringIgnoreCase(withscores, RedisKeyword.WITHSCORES.name())) {
                Set<Tuple> tuples = zSetMixClient.zrevrangeWithScores(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
                RedisHBaseMonitor.incrCollectionSize("zrevrangeWithScores", tuples == null ? 0 : tuples.size());
                return ParamUtils.tuples2MultiBulkReply(tuples);
            }
            throw new IllegalArgumentException(Utils.syntaxError);
        } else {
            Set<byte[]> zrange = zSetMixClient.zrevrange(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
            RedisHBaseMonitor.incrCollectionSize("zrevrange", zrange == null ? 0 : zrange.size());
            return ParamUtils.collection2MultiBulkReply(zrange);
        }
    }

    @Override
    public MultiBulkReply zrangebyscore(byte[] key, byte[] min, byte[] max, byte[][] args) {
        ParamUtils.ZRangeParams params = ParamUtils.parseZRangeParams(args);
        if (!params.withLimit && !params.withScores) {
            Set<byte[]> set = zSetMixClient.zrangeByScore(key, min, max);
            RedisHBaseMonitor.incrCollectionSize("zrangeByScore", set == null ? 0 : set.size());
            return ParamUtils.collection2MultiBulkReply(set);
        }
        if (params.withScores && !params.withLimit) {
            Set<Tuple> tuples = zSetMixClient.zrangeByScoreWithScores(key, min, max);
            RedisHBaseMonitor.incrCollectionSize("zrangeByScoreWithScores", tuples == null ? 0 : tuples.size());
            return ParamUtils.tuples2MultiBulkReply(tuples);
        }
        if (!params.withScores) {
            Set<byte[]> set = zSetMixClient.zrangeByScore(key, min, max, params.offset, params.count);
            RedisHBaseMonitor.incrCollectionSize("zrangeByScore", set == null ? 0 : set.size());
            return ParamUtils.collection2MultiBulkReply(set);
        }
        Set<Tuple> tuples = zSetMixClient.zrangeByScoreWithScores(key, min, max, params.offset, params.count);
        RedisHBaseMonitor.incrCollectionSize("zrangeByScoreWithScores", tuples == null ? 0 : tuples.size());
        return ParamUtils.tuples2MultiBulkReply(tuples);
    }

    @Override
    public MultiBulkReply zrevrangebyscore(byte[] key, byte[] min, byte[] max, byte[][] args) {
        ParamUtils.ZRangeParams params = ParamUtils.parseZRangeParams(args);
        if (!params.withLimit && !params.withScores) {
            Set<byte[]> set = zSetMixClient.zrevrangeByScore(key, min, max);
            RedisHBaseMonitor.incrCollectionSize("zrevrangeByScore", set == null ? 0 : set.size());
            return ParamUtils.collection2MultiBulkReply(set);
        }
        if (params.withScores && !params.withLimit) {
            Set<Tuple> tuples = zSetMixClient.zrevrangeByScoreWithScores(key, min, max);
            RedisHBaseMonitor.incrCollectionSize("zrevrangeByScoreWithScores", tuples == null ? 0 : tuples.size());
            return ParamUtils.tuples2MultiBulkReply(tuples);
        }
        if (!params.withScores) {
            Set<byte[]> set = zSetMixClient.zrevrangeByScore(key, min, max, params.offset, params.count);
            RedisHBaseMonitor.incrCollectionSize("zrevrangeByScore", set == null ? 0 : set.size());
            return ParamUtils.collection2MultiBulkReply(set);
        }
        Set<Tuple> tuples = zSetMixClient.zrevrangeByScoreWithScores(key, min, max, params.offset, params.count);
        RedisHBaseMonitor.incrCollectionSize("zrevrangeByScoreWithScores", tuples == null ? 0 : tuples.size());
        return ParamUtils.tuples2MultiBulkReply(tuples);
    }

    @Override
    public MultiBulkReply zrangebylex(byte[] key, byte[] min, byte[] max, byte[][] args) {
        ParamUtils.ZRangeParams params = ParamUtils.parseZRangeParams(args);
        if (params.withLimit) {
            Set<byte[]> set = zSetMixClient.zrangeByLex(key, min, max, params.offset, params.count);
            RedisHBaseMonitor.incrCollectionSize("zrangeByLex", set == null ? 0 : set.size());
            return ParamUtils.collection2MultiBulkReply(set);
        } else {
            Set<byte[]> set = zSetMixClient.zrangeByLex(key, min, max);
            RedisHBaseMonitor.incrCollectionSize("zrangeByLex", set == null ? 0 : set.size());
            return ParamUtils.collection2MultiBulkReply(set);
        }
    }

    @Override
    public MultiBulkReply zrevrangebylex(byte[] key, byte[] min, byte[] max, byte[][] args) {
        ParamUtils.ZRangeParams params = ParamUtils.parseZRangeParams(args);
        if (!params.withLimit) {
            Set<byte[]> set = zSetMixClient.zrevrangeByLex(key, min, max);
            RedisHBaseMonitor.incrCollectionSize("zrevrangeByLex", set == null ? 0 : set.size());
            return ParamUtils.collection2MultiBulkReply(set);
        } else {
            Set<byte[]> set = zSetMixClient.zrevrangeByLex(key, min, max, params.offset, params.count);
            RedisHBaseMonitor.incrCollectionSize("zrevrangeByLex", set == null ? 0 : set.size());
            return ParamUtils.collection2MultiBulkReply(set);
        }
    }

    @Override
    public IntegerReply zremrangebyrank(byte[] key, byte[] start, byte[] stop) {
        Long zremrangeByRank = zSetMixClient.zremrangeByRank(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
        RedisHBaseMonitor.incrCollectionSize("zremrangeByRank", zremrangeByRank == null ? 0 : zremrangeByRank);
        return new IntegerReply(zremrangeByRank);
    }

    @Override
    public IntegerReply zremrangebyscore(byte[] key, byte[] min, byte[] max) {
        Long zremrangeByScore = zSetMixClient.zremrangeByScore(key, min, max);
        RedisHBaseMonitor.incrCollectionSize("zremrangeByScore", zremrangeByScore == null ? 0 : zremrangeByScore);
        return new IntegerReply(zremrangeByScore);
    }

    @Override
    public IntegerReply zremrangebylex(byte[] key, byte[] min, byte[] max) {
        Long zremrangeByLex = zSetMixClient.zremrangeByLex(key, min, max);
        RedisHBaseMonitor.incrCollectionSize("zremrangeByLex", zremrangeByLex == null ? 0 : zremrangeByLex);
        return new IntegerReply(zremrangeByLex);
    }

    @Override
    public IntegerReply zrank(byte[] key, byte[] member) {
        Long zrank = zSetMixClient.zrank(key, member);
        RedisHBaseMonitor.incrCollectionSize("zrank", zrank == null ? 0 : zrank);
        return new IntegerReply(zrank);
    }

    @Override
    public IntegerReply zrevrank(byte[] key, byte[] member) {
        Long zrevrank = zSetMixClient.zrevrank(key, member);
        RedisHBaseMonitor.incrCollectionSize("zrevrank", zrevrank == null ? 0 : zrevrank);
        return new IntegerReply(zrevrank);
    }

    @Override
    public IntegerReply zrem(byte[] key, byte[][] members) {
        Long zrem = zSetMixClient.zrem(key, members);
        return new IntegerReply(zrem);
    }

    @Override
    public IntegerReply zlexcount(byte[] key, byte[] min, byte[] max) {
        Long zlexcount = zSetMixClient.zlexcount(key, min, max);
        RedisHBaseMonitor.incrCollectionSize("zlexcount", zlexcount == null ? 0 : zlexcount);
        return new IntegerReply(zlexcount);
    }

    @Override
    public IntegerReply hset(byte[] key, byte[] field, byte[] value) {
        Long hset = hashMixClient.hset(key, field, value);
        RedisHBaseMonitor.incrCollectionSize("hset", (hset != null && hset > 0) ? 1 : 0);
        return new IntegerReply(hset);
    }

    @Override
    public IntegerReply hsetnx(byte[] key, byte[] field, byte[] value) {
        Long hset = hashMixClient.hsetnx(key, field, value);
        RedisHBaseMonitor.incrCollectionSize("hsetnx", (hset != null && hset > 0) ? 1 : 0);
        return new IntegerReply(hset);
    }

    @Override
    public Reply hget(byte[] key, byte[] field) {
        byte[] hget = hashMixClient.hget(key, field);
        RedisHBaseMonitor.incrCollectionSize("hget", (hget != null) ? 1 : 0);
        return new BulkReply(hget);
    }

    @Override
    public IntegerReply hexists(byte[] key, byte[] field) {
        Boolean hexists = hashMixClient.hexists(key, field);
        return hexists ? IntegerReply.REPLY_1 : IntegerReply.REPLY_0;
    }

    @Override
    public IntegerReply hdel(byte[] key, byte[] field) {
        Long hdel = hashMixClient.hdel(key, field);
        RedisHBaseMonitor.incrCollectionSize("hdel", (hdel != null && hdel > 0) ? 1 : 0);
        return new IntegerReply(hdel);
    }

    @Override
    public IntegerReply hlen(byte[] key) {
        Long hlen = hashMixClient.hlen(key);
        RedisHBaseMonitor.incrCollectionSize("hlen", (hlen != null && hlen > 0) ? hlen : 0);
        return new IntegerReply(hlen);
    }

    @Override
    public StatusReply hmset(byte[] key, byte[][] kvs) {
        if (kvs == null || kvs.length % 2 != 0) {
            throw Utils.illegalArgumentException();
        }
        Map<byte[], byte[]> kvMap = new HashMap<>();
        for (int i=0; i< kvs.length; i+=2) {
            byte[] field = kvs[i];
            byte[] value = kvs[i+1];
            kvMap.put(field, value);
        }
        String hmset = hashMixClient.hmset(key, kvMap);
        RedisHBaseMonitor.incrCollectionSize("hmset", kvs.length / 2);
        return new StatusReply(hmset);
    }

    @Override
    public MultiBulkReply hmget(byte[] key, byte[][] fields) {
        if (fields == null || fields.length == 0) {
            throw Utils.illegalArgumentException();
        }
        List<byte[]> hmget = hashMixClient.hmget(key, fields);
        int size = 0;
        if (hmget != null) {
            for (byte[] bytes : hmget) {
                if (bytes != null) {
                    size++;
                }
            }
        }
        RedisHBaseMonitor.incrCollectionSize("hmget", size);
        return ParamUtils.collection2MultiBulkReply(hmget);
    }

    @Override
    public MultiBulkReply hkeys(byte[] key) {
        Set<byte[]> hkeys = hashMixClient.hkeys(key);
        int size = 0;
        if (hkeys != null) {
            for (byte[] bytes : hkeys) {
                if (bytes != null) {
                    size++;
                }
            }
        }
        RedisHBaseMonitor.incrCollectionSize("hkeys", size);
        return ParamUtils.collection2MultiBulkReply(hkeys);
    }

    @Override
    public MultiBulkReply hvals(byte[] key) {
        List<byte[]> hvals = hashMixClient.hvals(key);
        int size = 0;
        if (hvals != null) {
            for (byte[] bytes : hvals) {
                if (bytes != null) {
                    size++;
                }
            }
        }
        RedisHBaseMonitor.incrCollectionSize("hvals", size);
        return ParamUtils.collection2MultiBulkReply(hvals);
    }

    @Override
    public MultiBulkReply hgetall(byte[] key) {
        Map<byte[], byte[]> map = hashMixClient.hgetAll(key);
        Reply[] replies = new Reply[map.size()*2];
        int index = 0;
        for (Map.Entry<byte[], byte[]> entry : map.entrySet()) {
            replies[index] = new BulkReply(entry.getKey());
            replies[index + 1] = new BulkReply(entry.getValue());
            index += 2;
        }
        RedisHBaseMonitor.incrCollectionSize("hgetall", map.size());
        return new MultiBulkReply(replies);
    }
}
