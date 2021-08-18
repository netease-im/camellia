package com.netease.nim.camellia.redis;

import com.netease.nim.camellia.core.client.annotation.ReadOp;
import com.netease.nim.camellia.core.client.annotation.ShadingParam;
import com.netease.nim.camellia.core.client.annotation.WriteOp;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.resource.ResourceWrapper;
import com.netease.nim.camellia.redis.util.CamelliaRedisInitializr;
import com.netease.nim.camellia.redis.util.LogUtil;
import redis.clients.jedis.*;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2019/7/22.
 */
public class CamelliaRedisImpl implements ICamelliaRedis {

    private Resource resource;
    private ICamelliaRedis redis;

    public CamelliaRedisImpl(Resource resource) {
        if (resource == null) return;
        this.resource = resource;
        CamelliaRedisEnv env;
        if (resource instanceof ResourceWrapper) {
            env = ((ResourceWrapper) resource).getEnv();
        } else {
            throw new IllegalArgumentException("not ResourceWrapper");
        }
        redis = CamelliaRedisInitializr.init(resource, env);
    }

    @Override
    public Jedis getJedis(byte[] key) {
        return redis.getJedis(key);
    }

    @WriteOp
    @Override
    public String set(@ShadingParam byte[] key, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.set(key, value);
    }

    @ReadOp
    @Override
    public byte[] get(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.get(key);
    }

    @WriteOp
    @Override
    public String set(@ShadingParam String key, String value) {
        LogUtil.debugLog(resource, key);
        return redis.set(key, value);
    }

    @WriteOp
    @Override
    public String set(@ShadingParam String key, String value, SetParams params) {
        LogUtil.debugLog(resource, key);
        return redis.set(key, value, params);
    }

    @WriteOp
    @Override
    public String set(@ShadingParam byte[] key, byte[] value, SetParams params) {
        LogUtil.debugLog(resource, key);
        return redis.set(key, value, params);
    }

    @ReadOp
    @Override
    public String get(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.get(key);
    }

    @ReadOp
    @Override
    public Boolean exists(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.exists(key);
    }

    @WriteOp
    @Override
    public Long persist(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.persist(key);
    }

    @ReadOp
    @Override
    public String type(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.type(key);
    }

    @WriteOp
    @Override
    public Long expire(@ShadingParam String key, int seconds) {
        LogUtil.debugLog(resource, key);
        return redis.expire(key, seconds);
    }

    @WriteOp
    @Override
    public Long pexpire(@ShadingParam String key, long milliseconds) {
        LogUtil.debugLog(resource, key);
        return redis.pexpire(key, milliseconds);
    }

    @WriteOp
    @Override
    public Long expireAt(@ShadingParam String key, long unixTime) {
        LogUtil.debugLog(resource, key);
        return redis.expireAt(key, unixTime);
    }

    @WriteOp
    @Override
    public Long pexpireAt(@ShadingParam String key, long millisecondsTimestamp) {
        LogUtil.debugLog(resource, key);
        return redis.pexpireAt(key, millisecondsTimestamp);
    }

    @ReadOp
    @Override
    public Long ttl(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.ttl(key);
    }

    @ReadOp
    @Override
    public Long pttl(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.pttl(key);
    }

    @WriteOp
    @Override
    public Boolean setbit(@ShadingParam String key, long offset, boolean value) {
        LogUtil.debugLog(resource, key);
        return redis.setbit(key, offset, value);
    }

    @WriteOp
    @Override
    public Boolean setbit(@ShadingParam String key, long offset, String value) {
        LogUtil.debugLog(resource, key);
        return redis.setbit(key, offset, value);
    }

    @ReadOp
    @Override
    public Boolean getbit(@ShadingParam String key, long offset) {
        LogUtil.debugLog(resource, key);
        return redis.getbit(key, offset);
    }

    @WriteOp
    @Override
    public Long setrange(@ShadingParam String key, long offset, String value) {
        LogUtil.debugLog(resource, key);
        return redis.setrange(key, offset, value);
    }

    @ReadOp
    @Override
    public String getrange(@ShadingParam String key, long startOffset, long endOffset) {
        LogUtil.debugLog(resource, key);
        return redis.getrange(key, startOffset, endOffset);
    }

    @WriteOp
    @Override
    public String getSet(@ShadingParam String key, String value) {
        LogUtil.debugLog(resource, key);
        return redis.getSet(key, value);
    }

    @WriteOp
    @Override
    public Long setnx(@ShadingParam String key, String value) {
        LogUtil.debugLog(resource, key);
        return redis.setnx(key, value);
    }

    @WriteOp
    @Override
    public String setex(@ShadingParam String key, int seconds, String value) {
        LogUtil.debugLog(resource, key);
        return redis.setex(key, seconds, value);
    }

    @WriteOp
    @Override
    public String psetex(@ShadingParam String key, long milliseconds, String value) {
        LogUtil.debugLog(resource, key);
        return redis.psetex(key, milliseconds, value);
    }

    @WriteOp
    @Override
    public Long decrBy(@ShadingParam String key, long integer) {
        LogUtil.debugLog(resource, key);
        return redis.decrBy(key, integer);
    }

    @WriteOp
    @Override
    public Long decr(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.decr(key);
    }

    @WriteOp
    @Override
    public Long incrBy(@ShadingParam String key, long integer) {
        LogUtil.debugLog(resource, key);
        return redis.incrBy(key, integer);
    }

    @WriteOp
    @Override
    public Double incrByFloat(@ShadingParam String key, double value) {
        LogUtil.debugLog(resource, key);
        return redis.incrByFloat(key, value);
    }

    @WriteOp
    @Override
    public Long incr(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.incr(key);
    }

    @WriteOp
    @Override
    public Long append(@ShadingParam String key, String value) {
        LogUtil.debugLog(resource, key);
        return redis.append(key, value);
    }

    @ReadOp
    @Override
    public String substr(@ShadingParam String key, int start, int end) {
        LogUtil.debugLog(resource, key);
        return redis.substr(key, start, end);
    }

    @WriteOp
    @Override
    public Long hset(@ShadingParam String key, String field, String value) {
        LogUtil.debugLog(resource, key);
        return redis.hset(key, field, value);
    }

    @ReadOp
    @Override
    public String hget(@ShadingParam String key, String field) {
        LogUtil.debugLog(resource, key);
        return redis.hget(key, field);
    }

    @WriteOp
    @Override
    public Long hsetnx(@ShadingParam String key, String field, String value) {
        LogUtil.debugLog(resource, key);
        return redis.hsetnx(key, field, value);
    }

    @WriteOp
    @Override
    public String hmset(@ShadingParam String key, Map<String, String> hash) {
        LogUtil.debugLog(resource, key);
        return redis.hmset(key, hash);
    }

    @ReadOp
    @Override
    public List<String> hmget(@ShadingParam String key, String... fields) {
        LogUtil.debugLog(resource, key);
        return redis.hmget(key, fields);
    }

    @WriteOp
    @Override
    public Long hincrBy(@ShadingParam String key, String field, long value) {
        LogUtil.debugLog(resource, key);
        return redis.hincrBy(key, field, value);
    }

    @WriteOp
    @Override
    public Double hincrByFloat(@ShadingParam String key, String field, double value) {
        LogUtil.debugLog(resource, key);
        return redis.hincrByFloat(key, field, value);
    }

    @ReadOp
    @Override
    public Boolean hexists(@ShadingParam String key, String field) {
        LogUtil.debugLog(resource, key);
        return redis.hexists(key, field);
    }

    @WriteOp
    @Override
    public Long hdel(@ShadingParam String key, String... field) {
        LogUtil.debugLog(resource, key);
        return redis.hdel(key, field);
    }

    @ReadOp
    @Override
    public Long hlen(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.hlen(key);
    }

    @ReadOp
    @Override
    public Set<String> hkeys(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.hkeys(key);
    }

    @ReadOp
    @Override
    public List<String> hvals(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.hvals(key);
    }

    @ReadOp
    @Override
    public Map<String, String> hgetAll(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.hgetAll(key);
    }

    @WriteOp
    @Override
    public Long rpush(@ShadingParam String key, String... string) {
        LogUtil.debugLog(resource, key);
        return redis.rpush(key, string);
    }

    @WriteOp
    @Override
    public Long lpush(@ShadingParam String key, String... string) {
        LogUtil.debugLog(resource, key);
        return redis.lpush(key, string);
    }

    @ReadOp
    @Override
    public Long llen(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.llen(key);
    }

    @ReadOp
    @Override
    public List<String> lrange(@ShadingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.lrange(key, start, end);
    }

    @ReadOp
    @Override
    public String ltrim(@ShadingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.ltrim(key, start, end);
    }

    @ReadOp
    @Override
    public String lindex(@ShadingParam String key, long index) {
        LogUtil.debugLog(resource, key);
        return redis.lindex(key, index);
    }

    @WriteOp
    @Override
    public String lset(@ShadingParam String key, long index, String value) {
        LogUtil.debugLog(resource, key);
        return redis.lset(key, index, value);
    }

    @WriteOp
    @Override
    public Long lrem(@ShadingParam String key, long count, String value) {
        LogUtil.debugLog(resource, key);
        return redis.lrem(key, count, value);
    }

    @WriteOp
    @Override
    public String lpop(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.lpop(key);
    }

    @WriteOp
    @Override
    public String rpop(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.rpop(key);
    }

    @WriteOp
    @Override
    public Long sadd(@ShadingParam String key, String... member) {
        LogUtil.debugLog(resource, key);
        return redis.sadd(key, member);
    }

    @ReadOp
    @Override
    public Set<String> smembers(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.smembers(key);
    }

    @WriteOp
    @Override
    public Long srem(@ShadingParam String key, String... member) {
        LogUtil.debugLog(resource, key);
        return redis.srem(key, member);
    }

    @WriteOp
    @Override
    public String spop(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.spop(key);
    }

    @WriteOp
    @Override
    public Set<String> spop(@ShadingParam String key, long count) {
        LogUtil.debugLog(resource, key);
        return redis.spop(key, count);
    }

    @ReadOp
    @Override
    public Long scard(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.scard(key);
    }

    @ReadOp
    @Override
    public Boolean sismember(@ShadingParam String key, String member) {
        LogUtil.debugLog(resource, key);
        return redis.sismember(key, member);
    }

    @ReadOp
    @Override
    public String srandmember(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.srandmember(key);
    }

    @ReadOp
    @Override
    public List<String> srandmember(@ShadingParam String key, int count) {
        LogUtil.debugLog(resource, key);
        return redis.srandmember(key, count);
    }

    @ReadOp
    @Override
    public Long strlen(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.strlen(key);
    }

    @WriteOp
    @Override
    public Long zadd(@ShadingParam String key, double score, String member) {
        LogUtil.debugLog(resource, key);
        return redis.zadd(key, score, member);
    }

    @WriteOp
    @Override
    public Long zadd(@ShadingParam String key, double score, String member, ZAddParams params) {
        LogUtil.debugLog(resource, key);
        return redis.zadd(key, score, member, params);
    }

    @WriteOp
    @Override
    public Long zadd(@ShadingParam String key, Map<String, Double> scoreMembers) {
        LogUtil.debugLog(resource, key);
        return redis.zadd(key, scoreMembers);
    }

    @WriteOp
    @Override
    public Long zadd(@ShadingParam String key, Map<String, Double> scoreMembers, ZAddParams params) {
        LogUtil.debugLog(resource, key);
        return redis.zadd(key, scoreMembers, params);
    }

    @ReadOp
    @Override
    public Set<String> zrange(@ShadingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zrange(key, start, end);
    }

    @WriteOp
    @Override
    public Long zrem(@ShadingParam String key, String... member) {
        LogUtil.debugLog(resource, key);
        return redis.zrem(key, member);
    }

    @WriteOp
    @Override
    public Double zincrby(@ShadingParam String key, double score, String member) {
        LogUtil.debugLog(resource, key);
        return redis.zincrby(key, score, member);
    }

    @WriteOp
    @Override
    public Double zincrby(@ShadingParam String key, double score, String member, ZIncrByParams params) {
        LogUtil.debugLog(resource, key);
        return redis.zincrby(key, score, member, params);
    }

    @ReadOp
    @Override
    public Long zrank(@ShadingParam String key, String member) {
        LogUtil.debugLog(resource, key);
        return redis.zrank(key, member);
    }

    @ReadOp
    @Override
    public Long zrevrank(@ShadingParam String key, String member) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrank(key, member);
    }

    @ReadOp
    @Override
    public Set<String> zrevrange(@ShadingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrange(key, start, end);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeWithScores(@ShadingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeWithScores(key, start, end);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeWithScores(@ShadingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeWithScores(key, start, end);
    }

    @ReadOp
    @Override
    public Long zcard(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.zcard(key);
    }

    @ReadOp
    @Override
    public Double zscore(@ShadingParam String key, String member) {
        LogUtil.debugLog(resource, key);
        return redis.zscore(key, member);
    }

    @ReadOp
    @Override
    public List<String> sort(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.sort(key);
    }

    @ReadOp
    @Override
    public List<String> sort(@ShadingParam String key, SortingParams sortingParameters) {
        LogUtil.debugLog(resource, key);
        return redis.sort(key, sortingParameters);
    }

    @ReadOp
    @Override
    public Long zcount(@ShadingParam String key, double min, double max) {
        LogUtil.debugLog(resource, key);
        return redis.zcount(key, min, max);
    }

    @ReadOp
    @Override
    public Long zcount(@ShadingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        return redis.zcount(key, min, max);
    }

    @ReadOp
    @Override
    public Set<String> zrangeByScore(@ShadingParam String key, double min, double max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScore(key, min, max);
    }

    @ReadOp
    @Override
    public Set<String> zrangeByScore(@ShadingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScore(key, min, max);
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByScore(@ShadingParam String key, double max, double min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScore(key, max, min);
    }

    @ReadOp
    @Override
    public Set<String> zrangeByScore(@ShadingParam String key, double min, double max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScore(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByScore(@ShadingParam String key, String max, String min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScore(key, max, min);
    }

    @ReadOp
    @Override
    public Set<String> zrangeByScore(@ShadingParam String key, String min, String max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScore(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByScore(@ShadingParam String key, double max, double min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScore(key, max, min, offset, count);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShadingParam String key, double min, double max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScoreWithScores(key, min, max);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShadingParam String key, double max, double min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScoreWithScores(key, max, min);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShadingParam String key, double min, double max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByScore(@ShadingParam String key, String max, String min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScore(key, max, min, offset, count);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShadingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScoreWithScores(key, min, max);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShadingParam String key, String max, String min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScoreWithScores(key, max, min);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShadingParam String key, String min, String max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShadingParam String key, double max, double min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShadingParam String key, String max, String min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @WriteOp
    @Override
    public Long zremrangeByRank(@ShadingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zremrangeByRank(key, start, end);
    }

    @WriteOp
    @Override
    public Long zremrangeByScore(@ShadingParam String key, double start, double end) {
        LogUtil.debugLog(resource, key);
        return redis.zremrangeByScore(key, start, end);
    }

    @WriteOp
    @Override
    public Long zremrangeByScore(@ShadingParam String key, String start, String end) {
        LogUtil.debugLog(resource, key);
        return redis.zremrangeByScore(key, start, end);
    }

    @ReadOp
    @Override
    public Long zlexcount(@ShadingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        return redis.zlexcount(key, min, max);
    }

    @ReadOp
    @Override
    public Set<String> zrangeByLex(@ShadingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByLex(key, min, max);
    }

    @ReadOp
    @Override
    public Set<String> zrangeByLex(@ShadingParam String key, String min, String max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByLex(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByLex(@ShadingParam String key, String max, String min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByLex(key, max, min);
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByLex(@ShadingParam String key, String max, String min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByLex(key, max, min, offset, count);
    }

    @WriteOp
    @Override
    public Long zremrangeByLex(@ShadingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        return redis.zremrangeByLex(key, min, max);
    }

    @WriteOp
    @Override
    public Long linsert(@ShadingParam String key, ListPosition where, String pivot, String value) {
        LogUtil.debugLog(resource, key);
        return redis.linsert(key, where, pivot, value);
    }

    @WriteOp
    @Override
    public Long lpushx(@ShadingParam String key, String... string) {
        LogUtil.debugLog(resource, key);
        return redis.lpushx(key, string);
    }

    @WriteOp
    @Override
    public Long rpushx(@ShadingParam String key, String... string) {
        LogUtil.debugLog(resource, key);
        return redis.rpushx(key, string);
    }

    @WriteOp
    @Override
    public Long del(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.del(key);
    }

    @ReadOp
    @Override
    public String echo(@ShadingParam String string) {
        LogUtil.debugLog(resource, string);
        return redis.echo(string);
    }

    @ReadOp
    @Override
    public Long bitcount(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.bitcount(key);
    }

    @ReadOp
    @Override
    public Long bitcount(@ShadingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.bitcount(key, start, end);
    }

    @ReadOp
    @Override
    public Long bitpos(@ShadingParam String key, boolean value) {
        LogUtil.debugLog(resource, key);
        return redis.bitpos(key, value);
    }

    @ReadOp
    @Override
    public Long bitpos(@ShadingParam String key, boolean value, BitPosParams params) {
        LogUtil.debugLog(resource, key);
        return redis.bitpos(key, value, params);
    }

    @ReadOp
    @Override
    public Long bitpos(@ShadingParam byte[] key, boolean value) {
        LogUtil.debugLog(resource, key);
        return redis.bitpos(key, value);
    }

    @ReadOp
    @Override
    public Long bitpos(@ShadingParam byte[] key, boolean value, BitPosParams params) {
        LogUtil.debugLog(resource, key);
        return redis.bitpos(key, value, params);
    }

    @ReadOp
    @Override
    public ScanResult<Map.Entry<String, String>> hscan(@ShadingParam String key, String cursor) {
        LogUtil.debugLog(resource, key);
        return redis.hscan(key, cursor);
    }

    @ReadOp
    @Override
    public ScanResult<Map.Entry<String, String>> hscan(@ShadingParam String key, String cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        return redis.hscan(key, cursor, params);
    }

    @ReadOp
    @Override
    public ScanResult<String> sscan(@ShadingParam String key, String cursor) {
        LogUtil.debugLog(resource, key);
        return redis.sscan(key, cursor);
    }

    @ReadOp
    @Override
    public ScanResult<String> sscan(@ShadingParam String key, String cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        return redis.sscan(key, cursor, params);
    }

    @ReadOp
    @Override
    public ScanResult<Tuple> zscan(@ShadingParam String key, String cursor) {
        LogUtil.debugLog(resource, key);
        return redis.zscan(key, cursor);
    }

    @ReadOp
    @Override
    public ScanResult<Tuple> zscan(@ShadingParam String key, String cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        return redis.zscan(key, cursor, params);
    }

    @WriteOp
    @Override
    public Long pfadd(@ShadingParam String key, String... elements) {
        LogUtil.debugLog(resource, key);
        return redis.pfadd(key, elements);
    }

    @ReadOp
    @Override
    public long pfcount(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.pfcount(key);
    }

    @WriteOp
    @Override
    public Long geoadd(@ShadingParam String key, double longitude, double latitude, String member) {
        LogUtil.debugLog(resource, key);
        return redis.geoadd(key, longitude, latitude, member);
    }

    @WriteOp
    @Override
    public Long geoadd(@ShadingParam String key, Map<String, GeoCoordinate> memberCoordinateMap) {
        LogUtil.debugLog(resource, key);
        return redis.geoadd(key, memberCoordinateMap);
    }

    @ReadOp
    @Override
    public Double geodist(@ShadingParam String key, String member1, String member2) {
        LogUtil.debugLog(resource, key);
        return redis.geodist(key, member1, member2);
    }

    @ReadOp
    @Override
    public Double geodist(@ShadingParam String key, String member1, String member2, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        return redis.geodist(key, member1, member2, unit);
    }

    @ReadOp
    @Override
    public List<String> geohash(@ShadingParam String key, String... members) {
        LogUtil.debugLog(resource, key);
        return redis.geohash(key, members);
    }

    @ReadOp
    @Override
    public List<GeoCoordinate> geopos(@ShadingParam String key, String... members) {
        LogUtil.debugLog(resource, key);
        return redis.geopos(key, members);
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadius(@ShadingParam String key, double longitude, double latitude, double radius, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        return redis.georadius(key, longitude, latitude, radius, unit);
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadius(@ShadingParam String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        return redis.georadius(key, longitude, latitude, radius, unit, param);
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadiusByMember(@ShadingParam String key, String member, double radius, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        return redis.georadiusByMember(key, member, radius, unit);
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadiusByMember(@ShadingParam String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        return redis.georadiusByMember(key, member, radius, unit, param);
    }

    @ReadOp
    @Override
    public List<Long> bitfield(@ShadingParam String key, String... arguments) {
        LogUtil.debugLog(resource, key);
        return redis.bitfield(key, arguments);
    }

    @WriteOp
    @Override
    public Long del(@ShadingParam(type = ShadingParam.Type.Collection) byte[]... keys) {
        LogUtil.debugLog(resource, keys);
        return redis.del(keys);
    }

    @ReadOp
    @Override
    public Long exists(@ShadingParam(type = ShadingParam.Type.Collection) byte[]... keys) {
        LogUtil.debugLog(resource, keys);
        return redis.exists(keys);
    }

    @ReadOp
    @Override
    public Map<byte[], byte[]> mget(@ShadingParam(type = ShadingParam.Type.Collection) byte[]... keys) {
        LogUtil.debugLog(resource, keys);
        return redis.mget(keys);
    }

    @WriteOp
    @Override
    public String mset(@ShadingParam(type = ShadingParam.Type.Collection) Map<byte[], byte[]> keysvalues) {
        LogUtil.debugLog(resource, keysvalues);
        return redis.mset(keysvalues);
    }

    @WriteOp
    @Override
    public Long del(@ShadingParam(type = ShadingParam.Type.Collection) String... keys) {
        LogUtil.debugLog(resource, keys);
        return redis.del(keys);
    }

    @ReadOp
    @Override
    public Long exists(@ShadingParam(type = ShadingParam.Type.Collection) String... keys) {
        LogUtil.debugLog(resource, keys);
        return redis.exists(keys);
    }

    @ReadOp
    @Override
    public Map<String, String> mget(@ShadingParam(type = ShadingParam.Type.Collection) String... keys) {
        LogUtil.debugLog(resource, keys);
        return redis.mget(keys);
    }

    @ReadOp
    @Override
    public Boolean exists(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.exists(key);
    }

    @WriteOp
    @Override
    public Long persist(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.persist(key);
    }

    @ReadOp
    @Override
    public String type(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.type(key);
    }

    @WriteOp
    @Override
    public Long expire(@ShadingParam byte[] key, int seconds) {
        LogUtil.debugLog(resource, key);
        return redis.expire(key, seconds);
    }

    @WriteOp
    @Override
    public Long pexpire(@ShadingParam byte[] key, long milliseconds) {
        LogUtil.debugLog(resource, key);
        return redis.pexpire(key, milliseconds);
    }

    @WriteOp
    @Override
    public Long expireAt(@ShadingParam byte[] key, long unixTime) {
        LogUtil.debugLog(resource, key);
        return redis.expireAt(key, unixTime);
    }

    @WriteOp
    @Override
    public Long pexpireAt(@ShadingParam byte[] key, long millisecondsTimestamp) {
        LogUtil.debugLog(resource, key);
        return redis.pexpireAt(key, millisecondsTimestamp);
    }

    @ReadOp
    @Override
    public Long ttl(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.ttl(key);
    }

    @ReadOp
    @Override
    public Long pttl(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.pttl(key);
    }

    @WriteOp
    @Override
    public Boolean setbit(@ShadingParam byte[] key, long offset, boolean value) {
        LogUtil.debugLog(resource, key);
        return redis.setbit(key, offset, value);
    }

    @WriteOp
    @Override
    public Boolean setbit(@ShadingParam byte[] key, long offset, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.setbit(key, offset, value);
    }

    @ReadOp
    @Override
    public Boolean getbit(@ShadingParam byte[] key, long offset) {
        LogUtil.debugLog(resource, key);
        return redis.getbit(key, offset);
    }

    @WriteOp
    @Override
    public Long setrange(@ShadingParam byte[] key, long offset, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.setrange(key, offset, value);
    }

    @ReadOp
    @Override
    public byte[] getrange(@ShadingParam byte[] key, long startOffset, long endOffset) {
        LogUtil.debugLog(resource, key);
        return redis.getrange(key, startOffset, endOffset);
    }

    @WriteOp
    @Override
    public byte[] getSet(@ShadingParam byte[] key, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.getSet(key, value);
    }

    @WriteOp
    @Override
    public Long setnx(@ShadingParam byte[] key, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.setnx(key, value);
    }

    @WriteOp
    @Override
    public String setex(@ShadingParam byte[] key, int seconds, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.setex(key, seconds, value);
    }

    @WriteOp
    @Override
    public String psetex(@ShadingParam byte[] key, long milliseconds, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.psetex(key, milliseconds, value);
    }

    @WriteOp
    @Override
    public Long decrBy(@ShadingParam byte[] key, long integer) {
        LogUtil.debugLog(resource, key);
        return redis.decrBy(key, integer);
    }

    @WriteOp
    @Override
    public Long decr(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.decr(key);
    }

    @WriteOp
    @Override
    public Long incrBy(@ShadingParam byte[] key, long integer) {
        LogUtil.debugLog(resource, key);
        return redis.incrBy(key, integer);
    }

    @WriteOp
    @Override
    public Double incrByFloat(@ShadingParam byte[] key, double value) {
        LogUtil.debugLog(resource, key);
        return redis.incrByFloat(key, value);
    }

    @WriteOp
    @Override
    public Long incr(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.incr(key);
    }

    @WriteOp
    @Override
    public Long append(@ShadingParam byte[] key, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.append(key, value);
    }

    @ReadOp
    @Override
    public byte[] substr(@ShadingParam byte[] key, int start, int end) {
        LogUtil.debugLog(resource, key);
        return redis.substr(key, start, end);
    }

    @WriteOp
    @Override
    public Long hset(@ShadingParam byte[] key, byte[] field, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.hset(key, field, value);
    }

    @ReadOp
    @Override
    public byte[] hget(@ShadingParam byte[] key, byte[] field) {
        LogUtil.debugLog(resource, key);
        return redis.hget(key, field);
    }

    @WriteOp
    @Override
    public Long hsetnx(@ShadingParam byte[] key, byte[] field, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.hsetnx(key, field, value);
    }

    @WriteOp
    @Override
    public String hmset(@ShadingParam byte[] key, Map<byte[], byte[]> hash) {
        LogUtil.debugLog(resource, key);
        return redis.hmset(key, hash);
    }

    @ReadOp
    @Override
    public List<byte[]> hmget(@ShadingParam byte[] key, byte[]... fields) {
        LogUtil.debugLog(resource, key);
        return redis.hmget(key, fields);
    }

    @WriteOp
    @Override
    public Long hincrBy(@ShadingParam byte[] key, byte[] field, long value) {
        LogUtil.debugLog(resource, key);
        return redis.hincrBy(key, field, value);
    }

    @WriteOp
    @Override
    public Double hincrByFloat(@ShadingParam byte[] key, byte[] field, double value) {
        LogUtil.debugLog(resource, key);
        return redis.hincrByFloat(key, field, value);
    }

    @ReadOp
    @Override
    public Boolean hexists(@ShadingParam byte[] key, byte[] field) {
        LogUtil.debugLog(resource, key);
        return redis.hexists(key, field);
    }

    @WriteOp
    @Override
    public Long hdel(@ShadingParam byte[] key, byte[]... field) {
        LogUtil.debugLog(resource, key);
        return redis.hdel(key, field);
    }

    @ReadOp
    @Override
    public Long hlen(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.hlen(key);
    }

    @ReadOp
    @Override
    public Set<byte[]> hkeys(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.hkeys(key);
    }

    @ReadOp
    @Override
    public List<byte[]> hvals(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.hvals(key);
    }

    @ReadOp
    @Override
    public Map<byte[], byte[]> hgetAll(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.hgetAll(key);
    }

    @WriteOp
    @Override
    public Long rpush(@ShadingParam byte[] key, byte[]... args) {
        LogUtil.debugLog(resource, key);
        return redis.rpush(key, args);
    }

    @WriteOp
    @Override
    public Long lpush(@ShadingParam byte[] key, byte[]... args) {
        LogUtil.debugLog(resource, key);
        return redis.lpush(key, args);
    }

    @ReadOp
    @Override
    public Long llen(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.llen(key);
    }

    @ReadOp
    @Override
    public List<byte[]> lrange(@ShadingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.lrange(key, start, end);
    }

    @ReadOp
    @Override
    public String ltrim(@ShadingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.ltrim(key, start, end);
    }

    @ReadOp
    @Override
    public byte[] lindex(@ShadingParam byte[] key, long index) {
        LogUtil.debugLog(resource, key);
        return redis.lindex(key, index);
    }

    @WriteOp
    @Override
    public String lset(@ShadingParam byte[] key, long index, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.lset(key, index, value);
    }

    @WriteOp
    @Override
    public Long lrem(@ShadingParam byte[] key, long count, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.lrem(key, count, value);
    }

    @WriteOp
    @Override
    public byte[] lpop(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.lpop(key);
    }

    @WriteOp
    @Override
    public byte[] rpop(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.rpop(key);
    }

    @WriteOp
    @Override
    public Long sadd(@ShadingParam byte[] key, byte[]... member) {
        LogUtil.debugLog(resource, key);
        return redis.sadd(key, member);
    }

    @ReadOp
    @Override
    public Set<byte[]> smembers(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.smembers(key);
    }

    @WriteOp
    @Override
    public Long srem(@ShadingParam byte[] key, byte[]... member) {
        LogUtil.debugLog(resource, key);
        return redis.srem(key, member);
    }

    @WriteOp
    @Override
    public byte[] spop(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.spop(key);
    }

    @WriteOp
    @Override
    public Set<byte[]> spop(@ShadingParam byte[] key, long count) {
        LogUtil.debugLog(resource, key);
        return redis.spop(key, count);
    }

    @ReadOp
    @Override
    public Long scard(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.scard(key);
    }

    @ReadOp
    @Override
    public Boolean sismember(@ShadingParam byte[] key, byte[] member) {
        LogUtil.debugLog(resource, key);
        return redis.sismember(key, member);
    }

    @ReadOp
    @Override
    public byte[] srandmember(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.srandmember(key);
    }

    @ReadOp
    @Override
    public List<byte[]> srandmember(@ShadingParam byte[] key, int count) {
        LogUtil.debugLog(resource, key);
        return redis.srandmember(key, count);
    }

    @ReadOp
    @Override
    public Long strlen(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.strlen(key);
    }

    @WriteOp
    @Override
    public Long zadd(@ShadingParam byte[] key, double score, byte[] member) {
        LogUtil.debugLog(resource, key);
        return redis.zadd(key, score, member);
    }

    @WriteOp
    @Override
    public Long zadd(@ShadingParam byte[] key, double score, byte[] member, ZAddParams params) {
        LogUtil.debugLog(resource, key);
        return redis.zadd(key, score, member, params);
    }

    @WriteOp
    @Override
    public Long zadd(@ShadingParam byte[] key, Map<byte[], Double> scoreMembers) {
        LogUtil.debugLog(resource, key);
        return redis.zadd(key, scoreMembers);
    }

    @WriteOp
    @Override
    public Long zadd(@ShadingParam byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
        LogUtil.debugLog(resource, key);
        return redis.zadd(key, scoreMembers, params);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrange(@ShadingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zrange(key, start, end);
    }

    @WriteOp
    @Override
    public Long zrem(@ShadingParam byte[] key, byte[]... member) {
        LogUtil.debugLog(resource, key);
        return redis.zrem(key, member);
    }

    @WriteOp
    @Override
    public Double zincrby(@ShadingParam byte[] key, double score, byte[] member) {
        LogUtil.debugLog(resource, key);
        return redis.zincrby(key, score, member);
    }

    @WriteOp
    @Override
    public Double zincrby(@ShadingParam byte[] key, double score, byte[] member, ZIncrByParams params) {
        LogUtil.debugLog(resource, key);
        return redis.zincrby(key, score, member, params);
    }

    @ReadOp
    @Override
    public Long zrank(@ShadingParam byte[] key, byte[] member) {
        LogUtil.debugLog(resource, key);
        return redis.zrank(key, member);
    }

    @ReadOp
    @Override
    public Long zrevrank(@ShadingParam byte[] key, byte[] member) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrank(key, member);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrange(@ShadingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrange(key, start, end);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeWithScores(@ShadingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeWithScores(key, start, end);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeWithScores(@ShadingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeWithScores(key, start, end);
    }

    @ReadOp
    @Override
    public Long zcard(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.zcard(key);
    }

    @ReadOp
    @Override
    public Double zscore(@ShadingParam byte[] key, byte[] member) {
        LogUtil.debugLog(resource, key);
        return redis.zscore(key, member);
    }

    @ReadOp
    @Override
    public List<byte[]> sort(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.sort(key);
    }

    @ReadOp
    @Override
    public List<byte[]> sort(@ShadingParam byte[] key, SortingParams sortingParameters) {
        LogUtil.debugLog(resource, key);
        return redis.sort(key, sortingParameters);
    }

    @ReadOp
    @Override
    public Long zcount(@ShadingParam byte[] key, double min, double max) {
        LogUtil.debugLog(resource, key);
        return redis.zcount(key, min, max);
    }

    @ReadOp
    @Override
    public Long zcount(@ShadingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        return redis.zcount(key, min, max);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByScore(@ShadingParam byte[] key, double min, double max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScore(key, min, max);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByScore(@ShadingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScore(key, min, max);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByScore(@ShadingParam byte[] key, double max, double min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScore(key, max, min);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByScore(@ShadingParam byte[] key, double min, double max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScore(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByScore(@ShadingParam byte[] key, byte[] max, byte[] min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScore(key, max, min);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByScore(@ShadingParam byte[] key, byte[] min, byte[] max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScore(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByScore(@ShadingParam byte[] key, double max, double min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScore(key, max, min, offset, count);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShadingParam byte[] key, double min, double max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScoreWithScores(key, min, max);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShadingParam byte[] key, double max, double min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScoreWithScores(key, max, min);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShadingParam byte[] key, double min, double max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByScore(@ShadingParam byte[] key, byte[] max, byte[] min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScore(key, max, min, offset, count);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShadingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScoreWithScores(key, min, max);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShadingParam byte[] key, byte[] max, byte[] min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScoreWithScores(key, max, min);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShadingParam byte[] key, byte[] min, byte[] max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShadingParam byte[] key, double max, double min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShadingParam byte[] key, byte[] max, byte[] min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @WriteOp
    @Override
    public Long zremrangeByRank(@ShadingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zremrangeByRank(key, start, end);
    }

    @WriteOp
    @Override
    public Long zremrangeByScore(@ShadingParam byte[] key, double start, double end) {
        LogUtil.debugLog(resource, key);
        return redis.zremrangeByScore(key, start, end);
    }

    @WriteOp
    @Override
    public Long zremrangeByScore(@ShadingParam byte[] key, byte[] start, byte[] end) {
        LogUtil.debugLog(resource, key);
        return redis.zremrangeByScore(key, start, end);
    }

    @ReadOp
    @Override
    public Long zlexcount(@ShadingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        return redis.zlexcount(key, min, max);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByLex(@ShadingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByLex(key, min, max);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByLex(@ShadingParam byte[] key, byte[] min, byte[] max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByLex(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByLex(@ShadingParam byte[] key, byte[] max, byte[] min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByLex(key, max, min);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByLex(@ShadingParam byte[] key, byte[] max, byte[] min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByLex(key, max, min, offset, count);
    }

    @WriteOp
    @Override
    public Long zremrangeByLex(@ShadingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        return redis.zremrangeByLex(key, min, max);
    }

    @WriteOp
    @Override
    public Long linsert(@ShadingParam byte[] key, ListPosition where, byte[] pivot, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.linsert(key, where, pivot, value);
    }

    @WriteOp
    @Override
    public Long lpushx(@ShadingParam byte[] key, byte[]... arg) {
        LogUtil.debugLog(resource, key);
        return redis.lpushx(key, arg);
    }

    @WriteOp
    @Override
    public Long rpushx(@ShadingParam byte[] key, byte[]... arg) {
        LogUtil.debugLog(resource, key);
        return redis.rpushx(key, arg);
    }

    @WriteOp
    @Override
    public Long del(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.del(key);
    }


    @ReadOp
    @Override
    public Long bitcount(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.bitcount(key);
    }

    @ReadOp
    @Override
    public Long bitcount(@ShadingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.bitcount(key, start, end);
    }

    @WriteOp
    @Override
    public Long pfadd(@ShadingParam byte[] key, byte[]... elements) {
        LogUtil.debugLog(resource, key);
        return redis.pfadd(key, elements);
    }

    @ReadOp
    @Override
    public long pfcount(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.pfcount(key);
    }

    @WriteOp
    @Override
    public Long geoadd(@ShadingParam byte[] key, double longitude, double latitude, byte[] member) {
        LogUtil.debugLog(resource, key);
        return redis.geoadd(key, longitude, latitude, member);
    }

    @WriteOp
    @Override
    public Long geoadd(@ShadingParam byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
        LogUtil.debugLog(resource, key);
        return redis.geoadd(key, memberCoordinateMap);
    }

    @ReadOp
    @Override
    public Double geodist(@ShadingParam byte[] key, byte[] member1, byte[] member2) {
        LogUtil.debugLog(resource, key);
        return redis.geodist(key, member1, member2);
    }

    @ReadOp
    @Override
    public Double geodist(@ShadingParam byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        return redis.geodist(key, member1, member2, unit);
    }

    @ReadOp
    @Override
    public List<byte[]> geohash(@ShadingParam byte[] key, byte[]... members) {
        LogUtil.debugLog(resource, key);
        return redis.geohash(key, members);
    }

    @ReadOp
    @Override
    public List<GeoCoordinate> geopos(@ShadingParam byte[] key, byte[]... members) {
        LogUtil.debugLog(resource, key);
        return redis.geopos(key, members);
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadius(@ShadingParam byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        return redis.georadius(key, longitude, latitude, radius, unit);
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadius(@ShadingParam byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        return redis.georadius(key, longitude, latitude, radius, unit, param);
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadiusByMember(@ShadingParam byte[] key, byte[] member, double radius, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        return redis.georadiusByMember(key, member, radius, unit);
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadiusByMember(@ShadingParam byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        return redis.georadiusByMember(key, member, radius, unit, param);
    }

    @ReadOp
    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(@ShadingParam byte[] key, byte[] cursor) {
        LogUtil.debugLog(resource, key);
        return redis.hscan(key, cursor);
    }

    @ReadOp
    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(@ShadingParam byte[] key, byte[] cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        return redis.hscan(key, cursor, params);
    }

    @ReadOp
    @Override
    public ScanResult<byte[]> sscan(@ShadingParam byte[] key, byte[] cursor) {
        LogUtil.debugLog(resource, key);
        return redis.sscan(key, cursor);
    }

    @ReadOp
    @Override
    public ScanResult<byte[]> sscan(@ShadingParam byte[] key, byte[] cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        return redis.sscan(key, cursor, params);
    }

    @ReadOp
    @Override
    public ScanResult<Tuple> zscan(@ShadingParam byte[] key, byte[] cursor) {
        LogUtil.debugLog(resource, key);
        return redis.zscan(key, cursor);
    }

    @ReadOp
    @Override
    public ScanResult<Tuple> zscan(@ShadingParam byte[] key, byte[] cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        return redis.zscan(key, cursor, params);
    }

    @ReadOp
    @Override
    public List<Long> bitfield(@ShadingParam byte[] key, byte[]... arguments) {
        LogUtil.debugLog(resource, key);
        return redis.bitfield(key, arguments);
    }

    @Override
    public Object eval(byte[] script, int keyCount, byte[]... params) {
        throw new CamelliaRedisException("not invoke here");
    }

    @Override
    public Object evalsha(byte[] sha1, int keyCount, byte[]... params) {
        throw new CamelliaRedisException("not invoke here");
    }

    @ReadOp
    @Override
    public byte[] dump(@ShadingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.dump(key);
    }

    @ReadOp
    @Override
    public byte[] dump(@ShadingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.dump(key);
    }

    @WriteOp
    @Override
    public String restore(@ShadingParam byte[] key, int ttl, byte[] serializedValue) {
        LogUtil.debugLog(resource, key);
        return redis.restore(key, ttl, serializedValue);
    }

    @WriteOp
    @Override
    public String restore(@ShadingParam String key, int ttl, byte[] serializedValue) {
        LogUtil.debugLog(resource, key);
        return redis.restore(key, ttl, serializedValue);
    }
}
