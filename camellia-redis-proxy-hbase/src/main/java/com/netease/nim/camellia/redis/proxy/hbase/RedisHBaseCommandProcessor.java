package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.util.ParamUtils;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.util.Bytes;
import redis.clients.jedis.Tuple;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2020/2/25.
 */
public class RedisHBaseCommandProcessor implements IRedisHBaseCommandProcessor {

    private final RedisHBaseZSetMixClient zSetMixClient;
    private final RedisHBaseCommonMixClient commonMixClient;

    public RedisHBaseCommandProcessor(CamelliaRedisTemplate redisTemplate, CamelliaHBaseTemplate hBaseTemplate) {
        //做一下预热
        try {
            redisTemplate.exists("warm");
            hBaseTemplate.get(RedisHBaseConfiguration.hbaseTableName(), new Get(Bytes.toBytes("warm")));
        } catch (Exception e) {
            throw new IllegalArgumentException("warm fail, please check", e);
        }
        //
        this.zSetMixClient = new RedisHBaseZSetMixClient(redisTemplate, hBaseTemplate);
        this.commonMixClient = new RedisHBaseCommonMixClient(redisTemplate, this.zSetMixClient);
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
        return new IntegerReply(zcard);
    }

    @Override
    public IntegerReply zcount(byte[] key, byte[] min, byte[] max) {
        Long zcount = zSetMixClient.zcount(key, min, max);
        return new IntegerReply(zcount);
    }

    @Override
    public MultiBulkReply zrange(byte[] key, byte[] start, byte[] stop, byte[] withscores) {
        if (withscores != null) {
            if (Utils.checkStringIgnoreCase(withscores, RedisKeyword.WITHSCORES.name())) {
                Set<Tuple> tuples = zSetMixClient.zrangeWithScores(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
                return ParamUtils.tuples2MultiBulkReply(tuples);
            }
            throw new IllegalArgumentException(Utils.syntaxError);
        } else {
            Set<byte[]> zrange = zSetMixClient.zrange(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
            return ParamUtils.collection2MultiBulkReply(zrange);
        }
    }

    @Override
    public MultiBulkReply zrevrange(byte[] key, byte[] start, byte[] stop, byte[] withscores) {
        if (withscores != null) {
            if (Utils.checkStringIgnoreCase(withscores, RedisKeyword.WITHSCORES.name())) {
                Set<Tuple> tuples = zSetMixClient.zrevrangeWithScores(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
                return ParamUtils.tuples2MultiBulkReply(tuples);
            }
            throw new IllegalArgumentException(Utils.syntaxError);
        } else {
            Set<byte[]> zrange = zSetMixClient.zrevrange(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
            return ParamUtils.collection2MultiBulkReply(zrange);
        }
    }

    @Override
    public MultiBulkReply zrangebyscore(byte[] key, byte[] min, byte[] max, byte[][] args) {
        ParamUtils.ZRangeParams params = ParamUtils.parseZRangeParams(args);
        if (!params.withLimit && !params.withScores) {
            return ParamUtils.collection2MultiBulkReply(zSetMixClient.zrangeByScore(key, min, max));
        }
        if (params.withScores && !params.withLimit) {
            return ParamUtils.tuples2MultiBulkReply(zSetMixClient.zrangeByScoreWithScores(key, min, max));
        }
        if (!params.withScores) {
            return ParamUtils.collection2MultiBulkReply(zSetMixClient.zrangeByScore(key, min, max, params.offset, params.count));
        }
        Set<Tuple> tuples = zSetMixClient.zrangeByScoreWithScores(key, min, max, params.offset, params.count);
        return ParamUtils.tuples2MultiBulkReply(tuples);
    }

    @Override
    public MultiBulkReply zrevrangebyscore(byte[] key, byte[] min, byte[] max, byte[][] args) {
        ParamUtils.ZRangeParams params = ParamUtils.parseZRangeParams(args);
        if (!params.withLimit && !params.withScores) {
            return ParamUtils.collection2MultiBulkReply(zSetMixClient.zrevrangeByScore(key, min, max));
        }
        if (params.withScores && !params.withLimit) {
            return ParamUtils.tuples2MultiBulkReply(zSetMixClient.zrevrangeByScoreWithScores(key, min, max));
        }
        if (!params.withScores) {
            return ParamUtils.collection2MultiBulkReply(zSetMixClient.zrevrangeByScore(key, min, max, params.offset, params.count));
        }
        Set<Tuple> tuples = zSetMixClient.zrevrangeByScoreWithScores(key, min, max, params.offset, params.count);
        return ParamUtils.tuples2MultiBulkReply(tuples);
    }

    @Override
    public MultiBulkReply zrangebylex(byte[] key, byte[] min, byte[] max, byte[][] args) {
        ParamUtils.ZRangeParams params = ParamUtils.parseZRangeParams(args);
        if (params.withLimit) {
            Set<byte[]> bytes = zSetMixClient.zrangeByLex(key, min, max, params.offset, params.count);
            return ParamUtils.collection2MultiBulkReply(bytes);
        } else {
            return ParamUtils.collection2MultiBulkReply(zSetMixClient.zrangeByLex(key, min, max));
        }
    }

    @Override
    public MultiBulkReply zrevrangebylex(byte[] key, byte[] min, byte[] max, byte[][] args) {
        ParamUtils.ZRangeParams params = ParamUtils.parseZRangeParams(args);
        if (!params.withLimit) {
            return ParamUtils.collection2MultiBulkReply(zSetMixClient.zrevrangeByLex(key, min, max));
        } else {
            Set<byte[]> bytes = zSetMixClient.zrevrangeByLex(key, min, max, params.offset, params.count);
            return ParamUtils.collection2MultiBulkReply(bytes);
        }
    }

    @Override
    public IntegerReply zremrangebyrank(byte[] key, byte[] start, byte[] stop) {
        Long zremrangeByRank = zSetMixClient.zremrangeByRank(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
        return new IntegerReply(zremrangeByRank);
    }

    @Override
    public IntegerReply zremrangebyscore(byte[] key, byte[] min, byte[] max) {
        Long zremrangeByScore = zSetMixClient.zremrangeByScore(key, min, max);
        return new IntegerReply(zremrangeByScore);
    }

    @Override
    public IntegerReply zremrangebylex(byte[] key, byte[] min, byte[] max) {
        Long zremrangeByLex = zSetMixClient.zremrangeByLex(key, min, max);
        return new IntegerReply(zremrangeByLex);
    }

    @Override
    public IntegerReply zrank(byte[] key, byte[] member) {
        Long zrank = zSetMixClient.zrank(key, member);
        return new IntegerReply(zrank);
    }

    @Override
    public IntegerReply zrevrank(byte[] key, byte[] member) {
        Long zrevrank = zSetMixClient.zrevrank(key, member);
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
        return new IntegerReply(zlexcount);
    }
}
