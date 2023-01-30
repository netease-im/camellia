package com.netease.nim.camellia.redis;

import com.netease.nim.camellia.core.client.annotation.ReadOp;
import com.netease.nim.camellia.core.client.annotation.ShardingParam;
import com.netease.nim.camellia.core.client.annotation.WriteOp;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.resource.*;
import com.netease.nim.camellia.redis.util.CamelliaRedisInitializer;
import com.netease.nim.camellia.redis.util.LogUtil;
import redis.clients.jedis.*;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;

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
        redis = CamelliaRedisInitializer.init(resource, env);
    }

    @Override
    public List<Jedis> getJedisList() {
        return redis.getJedisList();
    }

    @Override
    public Jedis getJedis(byte[] key) {
        return redis.getJedis(key);
    }

    @WriteOp
    @Override
    public String set(@ShardingParam byte[] key, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.set(key, value);
    }

    @ReadOp
    @Override
    public byte[] get(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.get(key);
    }

    @WriteOp
    @Override
    public String set(@ShardingParam String key, String value) {
        LogUtil.debugLog(resource, key);
        return redis.set(key, value);
    }

    @ReadOp
    @Override
    public String get(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.get(key);
    }

    @ReadOp
    @Override
    public Boolean exists(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.exists(key);
    }

    @WriteOp
    @Override
    public Long persist(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.persist(key);
    }

    @ReadOp
    @Override
    public String type(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.type(key);
    }

    @WriteOp
    @Override
    public Long expire(@ShardingParam String key, int seconds) {
        LogUtil.debugLog(resource, key);
        return redis.expire(key, seconds);
    }

    @WriteOp
    @Override
    public Long pexpire(@ShardingParam String key, long milliseconds) {
        LogUtil.debugLog(resource, key);
        return redis.pexpire(key, milliseconds);
    }

    @WriteOp
    @Override
    public Long expireAt(@ShardingParam String key, long unixTime) {
        LogUtil.debugLog(resource, key);
        return redis.expireAt(key, unixTime);
    }

    @WriteOp
    @Override
    public Long pexpireAt(@ShardingParam String key, long millisecondsTimestamp) {
        LogUtil.debugLog(resource, key);
        return redis.pexpireAt(key, millisecondsTimestamp);
    }

    @ReadOp
    @Override
    public Long ttl(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.ttl(key);
    }

    @ReadOp
    @Override
    public Long pttl(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.pttl(key);
    }

    @WriteOp
    @Override
    public Boolean setbit(@ShardingParam String key, long offset, boolean value) {
        LogUtil.debugLog(resource, key);
        return redis.setbit(key, offset, value);
    }

    @WriteOp
    @Override
    public Boolean setbit(@ShardingParam String key, long offset, String value) {
        LogUtil.debugLog(resource, key);
        return redis.setbit(key, offset, value);
    }

    @ReadOp
    @Override
    public Boolean getbit(@ShardingParam String key, long offset) {
        LogUtil.debugLog(resource, key);
        return redis.getbit(key, offset);
    }

    @WriteOp
    @Override
    public Long setrange(@ShardingParam String key, long offset, String value) {
        LogUtil.debugLog(resource, key);
        return redis.setrange(key, offset, value);
    }

    @ReadOp
    @Override
    public String getrange(@ShardingParam String key, long startOffset, long endOffset) {
        LogUtil.debugLog(resource, key);
        return redis.getrange(key, startOffset, endOffset);
    }

    @WriteOp
    @Override
    public String getSet(@ShardingParam String key, String value) {
        LogUtil.debugLog(resource, key);
        return redis.getSet(key, value);
    }

    @WriteOp
    @Override
    public Long setnx(@ShardingParam String key, String value) {
        LogUtil.debugLog(resource, key);
        return redis.setnx(key, value);
    }

    @WriteOp
    @Override
    public String setex(@ShardingParam String key, int seconds, String value) {
        LogUtil.debugLog(resource, key);
        return redis.setex(key, seconds, value);
    }

    @WriteOp
    @Override
    public String psetex(@ShardingParam String key, long milliseconds, String value) {
        LogUtil.debugLog(resource, key);
        return redis.psetex(key, milliseconds, value);
    }

    @WriteOp
    @Override
    public Long decrBy(@ShardingParam String key, long integer) {
        LogUtil.debugLog(resource, key);
        return redis.decrBy(key, integer);
    }

    @WriteOp
    @Override
    public Long decr(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.decr(key);
    }

    @WriteOp
    @Override
    public Long incrBy(@ShardingParam String key, long integer) {
        LogUtil.debugLog(resource, key);
        return redis.incrBy(key, integer);
    }

    @WriteOp
    @Override
    public Double incrByFloat(@ShardingParam String key, double value) {
        LogUtil.debugLog(resource, key);
        return redis.incrByFloat(key, value);
    }

    @WriteOp
    @Override
    public Long incr(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.incr(key);
    }

    @WriteOp
    @Override
    public Long append(@ShardingParam String key, String value) {
        LogUtil.debugLog(resource, key);
        return redis.append(key, value);
    }

    @ReadOp
    @Override
    public String substr(@ShardingParam String key, int start, int end) {
        LogUtil.debugLog(resource, key);
        return redis.substr(key, start, end);
    }

    @WriteOp
    @Override
    public Long hset(@ShardingParam String key, String field, String value) {
        LogUtil.debugLog(resource, key);
        return redis.hset(key, field, value);
    }

    @ReadOp
    @Override
    public String hget(@ShardingParam String key, String field) {
        LogUtil.debugLog(resource, key);
        return redis.hget(key, field);
    }

    @WriteOp
    @Override
    public Long hsetnx(@ShardingParam String key, String field, String value) {
        LogUtil.debugLog(resource, key);
        return redis.hsetnx(key, field, value);
    }

    @WriteOp
    @Override
    public String hmset(@ShardingParam String key, Map<String, String> hash) {
        LogUtil.debugLog(resource, key);
        return redis.hmset(key, hash);
    }

    @ReadOp
    @Override
    public List<String> hmget(@ShardingParam String key, String... fields) {
        LogUtil.debugLog(resource, key);
        return redis.hmget(key, fields);
    }

    @WriteOp
    @Override
    public Long hincrBy(@ShardingParam String key, String field, long value) {
        LogUtil.debugLog(resource, key);
        return redis.hincrBy(key, field, value);
    }

    @WriteOp
    @Override
    public Double hincrByFloat(@ShardingParam String key, String field, double value) {
        LogUtil.debugLog(resource, key);
        return redis.hincrByFloat(key, field, value);
    }

    @ReadOp
    @Override
    public Boolean hexists(@ShardingParam String key, String field) {
        LogUtil.debugLog(resource, key);
        return redis.hexists(key, field);
    }

    @WriteOp
    @Override
    public Long hdel(@ShardingParam String key, String... field) {
        LogUtil.debugLog(resource, key);
        return redis.hdel(key, field);
    }

    @ReadOp
    @Override
    public Long hlen(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.hlen(key);
    }

    @ReadOp
    @Override
    public Set<String> hkeys(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.hkeys(key);
    }

    @ReadOp
    @Override
    public List<String> hvals(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.hvals(key);
    }

    @ReadOp
    @Override
    public Map<String, String> hgetAll(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.hgetAll(key);
    }

    @WriteOp
    @Override
    public Long rpush(@ShardingParam String key, String... string) {
        LogUtil.debugLog(resource, key);
        return redis.rpush(key, string);
    }

    @WriteOp
    @Override
    public Long lpush(@ShardingParam String key, String... string) {
        LogUtil.debugLog(resource, key);
        return redis.lpush(key, string);
    }

    @ReadOp
    @Override
    public Long llen(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.llen(key);
    }

    @ReadOp
    @Override
    public List<String> lrange(@ShardingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.lrange(key, start, end);
    }

    @ReadOp
    @Override
    public String ltrim(@ShardingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.ltrim(key, start, end);
    }

    @ReadOp
    @Override
    public String lindex(@ShardingParam String key, long index) {
        LogUtil.debugLog(resource, key);
        return redis.lindex(key, index);
    }

    @WriteOp
    @Override
    public String lset(@ShardingParam String key, long index, String value) {
        LogUtil.debugLog(resource, key);
        return redis.lset(key, index, value);
    }

    @WriteOp
    @Override
    public Long lrem(@ShardingParam String key, long count, String value) {
        LogUtil.debugLog(resource, key);
        return redis.lrem(key, count, value);
    }

    @WriteOp
    @Override
    public String lpop(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.lpop(key);
    }

    @WriteOp
    @Override
    public String rpop(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.rpop(key);
    }

    @WriteOp
    @Override
    public Long sadd(@ShardingParam String key, String... member) {
        LogUtil.debugLog(resource, key);
        return redis.sadd(key, member);
    }

    @ReadOp
    @Override
    public Set<String> smembers(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.smembers(key);
    }

    @WriteOp
    @Override
    public Long srem(@ShardingParam String key, String... member) {
        LogUtil.debugLog(resource, key);
        return redis.srem(key, member);
    }

    @WriteOp
    @Override
    public String spop(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.spop(key);
    }

    @WriteOp
    @Override
    public Set<String> spop(@ShardingParam String key, long count) {
        LogUtil.debugLog(resource, key);
        return redis.spop(key, count);
    }

    @ReadOp
    @Override
    public Long scard(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.scard(key);
    }

    @ReadOp
    @Override
    public Boolean sismember(@ShardingParam String key, String member) {
        LogUtil.debugLog(resource, key);
        return redis.sismember(key, member);
    }

    @ReadOp
    @Override
    public String srandmember(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.srandmember(key);
    }

    @ReadOp
    @Override
    public List<String> srandmember(@ShardingParam String key, int count) {
        LogUtil.debugLog(resource, key);
        return redis.srandmember(key, count);
    }

    @ReadOp
    @Override
    public Long strlen(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.strlen(key);
    }

    @WriteOp
    @Override
    public Long zadd(@ShardingParam String key, double score, String member) {
        LogUtil.debugLog(resource, key);
        return redis.zadd(key, score, member);
    }

    @WriteOp
    @Override
    public Long zadd(@ShardingParam String key, double score, String member, ZAddParams params) {
        LogUtil.debugLog(resource, key);
        return redis.zadd(key, score, member, params);
    }

    @WriteOp
    @Override
    public Long zadd(@ShardingParam String key, Map<String, Double> scoreMembers) {
        LogUtil.debugLog(resource, key);
        return redis.zadd(key, scoreMembers);
    }

    @WriteOp
    @Override
    public Long zadd(@ShardingParam String key, Map<String, Double> scoreMembers, ZAddParams params) {
        LogUtil.debugLog(resource, key);
        return redis.zadd(key, scoreMembers, params);
    }

    @ReadOp
    @Override
    public Set<String> zrange(@ShardingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zrange(key, start, end);
    }

    @WriteOp
    @Override
    public Long zrem(@ShardingParam String key, String... member) {
        LogUtil.debugLog(resource, key);
        return redis.zrem(key, member);
    }

    @WriteOp
    @Override
    public Double zincrby(@ShardingParam String key, double score, String member) {
        LogUtil.debugLog(resource, key);
        return redis.zincrby(key, score, member);
    }

    @WriteOp
    @Override
    public Double zincrby(@ShardingParam String key, double score, String member, ZIncrByParams params) {
        LogUtil.debugLog(resource, key);
        return redis.zincrby(key, score, member, params);
    }

    @ReadOp
    @Override
    public Long zrank(@ShardingParam String key, String member) {
        LogUtil.debugLog(resource, key);
        return redis.zrank(key, member);
    }

    @ReadOp
    @Override
    public Long zrevrank(@ShardingParam String key, String member) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrank(key, member);
    }

    @ReadOp
    @Override
    public Set<String> zrevrange(@ShardingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrange(key, start, end);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeWithScores(@ShardingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeWithScores(key, start, end);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeWithScores(@ShardingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeWithScores(key, start, end);
    }

    @ReadOp
    @Override
    public Long zcard(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.zcard(key);
    }

    @ReadOp
    @Override
    public Double zscore(@ShardingParam String key, String member) {
        LogUtil.debugLog(resource, key);
        return redis.zscore(key, member);
    }

    @ReadOp
    @Override
    public List<String> sort(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.sort(key);
    }

    @ReadOp
    @Override
    public List<String> sort(@ShardingParam String key, SortingParams sortingParameters) {
        LogUtil.debugLog(resource, key);
        return redis.sort(key, sortingParameters);
    }

    @ReadOp
    @Override
    public Long zcount(@ShardingParam String key, double min, double max) {
        LogUtil.debugLog(resource, key);
        return redis.zcount(key, min, max);
    }

    @ReadOp
    @Override
    public Long zcount(@ShardingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        return redis.zcount(key, min, max);
    }

    @ReadOp
    @Override
    public Set<String> zrangeByScore(@ShardingParam String key, double min, double max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScore(key, min, max);
    }

    @ReadOp
    @Override
    public Set<String> zrangeByScore(@ShardingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScore(key, min, max);
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByScore(@ShardingParam String key, double max, double min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScore(key, max, min);
    }

    @ReadOp
    @Override
    public Set<String> zrangeByScore(@ShardingParam String key, double min, double max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScore(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByScore(@ShardingParam String key, String max, String min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScore(key, max, min);
    }

    @ReadOp
    @Override
    public Set<String> zrangeByScore(@ShardingParam String key, String min, String max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScore(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByScore(@ShardingParam String key, double max, double min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScore(key, max, min, offset, count);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShardingParam String key, double min, double max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScoreWithScores(key, min, max);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShardingParam String key, double max, double min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScoreWithScores(key, max, min);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShardingParam String key, double min, double max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByScore(@ShardingParam String key, String max, String min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScore(key, max, min, offset, count);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShardingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScoreWithScores(key, min, max);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShardingParam String key, String max, String min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScoreWithScores(key, max, min);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShardingParam String key, String min, String max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShardingParam String key, double max, double min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShardingParam String key, String max, String min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @WriteOp
    @Override
    public Long zremrangeByRank(@ShardingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zremrangeByRank(key, start, end);
    }

    @WriteOp
    @Override
    public Long zremrangeByScore(@ShardingParam String key, double start, double end) {
        LogUtil.debugLog(resource, key);
        return redis.zremrangeByScore(key, start, end);
    }

    @WriteOp
    @Override
    public Long zremrangeByScore(@ShardingParam String key, String start, String end) {
        LogUtil.debugLog(resource, key);
        return redis.zremrangeByScore(key, start, end);
    }

    @ReadOp
    @Override
    public Long zlexcount(@ShardingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        return redis.zlexcount(key, min, max);
    }

    @ReadOp
    @Override
    public Set<String> zrangeByLex(@ShardingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByLex(key, min, max);
    }

    @ReadOp
    @Override
    public Set<String> zrangeByLex(@ShardingParam String key, String min, String max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByLex(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByLex(@ShardingParam String key, String max, String min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByLex(key, max, min);
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByLex(@ShardingParam String key, String max, String min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByLex(key, max, min, offset, count);
    }

    @WriteOp
    @Override
    public Long zremrangeByLex(@ShardingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        return redis.zremrangeByLex(key, min, max);
    }

    @WriteOp
    @Override
    public Long linsert(@ShardingParam String key, BinaryClient.LIST_POSITION where, String pivot, String value) {
        LogUtil.debugLog(resource, key);
        return redis.linsert(key, where, pivot, value);
    }

    @WriteOp
    @Override
    public Long lpushx(@ShardingParam String key, String... string) {
        LogUtil.debugLog(resource, key);
        return redis.lpushx(key, string);
    }

    @WriteOp
    @Override
    public Long rpushx(@ShardingParam String key, String... string) {
        LogUtil.debugLog(resource, key);
        return redis.rpushx(key, string);
    }

    @WriteOp
    @Override
    public Long del(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.del(key);
    }

    @ReadOp
    @Override
    public String echo(@ShardingParam String string) {
        LogUtil.debugLog(resource, string);
        return redis.echo(string);
    }

    @ReadOp
    @Override
    public Long bitcount(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.bitcount(key);
    }

    @ReadOp
    @Override
    public Long bitcount(@ShardingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.bitcount(key, start, end);
    }

    @ReadOp
    @Override
    public Long bitpos(@ShardingParam String key, boolean value) {
        LogUtil.debugLog(resource, key);
        return redis.bitpos(key, value);
    }

    @ReadOp
    @Override
    public Long bitpos(@ShardingParam String key, boolean value, BitPosParams params) {
        LogUtil.debugLog(resource, key);
        return redis.bitpos(key, value, params);
    }

    @ReadOp
    @Override
    public Long bitpos(@ShardingParam byte[] key, boolean value) {
        LogUtil.debugLog(resource, key);
        return redis.bitpos(key, value);
    }

    @ReadOp
    @Override
    public Long bitpos(@ShardingParam byte[] key, boolean value, BitPosParams params) {
        LogUtil.debugLog(resource, key);
        return redis.bitpos(key, value, params);
    }

    @ReadOp
    @Override
    public ScanResult<Map.Entry<String, String>> hscan(@ShardingParam String key, String cursor) {
        LogUtil.debugLog(resource, key);
        return redis.hscan(key, cursor);
    }

    @ReadOp
    @Override
    public ScanResult<Map.Entry<String, String>> hscan(@ShardingParam String key, String cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        return redis.hscan(key, cursor, params);
    }

    @ReadOp
    @Override
    public ScanResult<String> sscan(@ShardingParam String key, String cursor) {
        LogUtil.debugLog(resource, key);
        return redis.sscan(key, cursor);
    }

    @ReadOp
    @Override
    public ScanResult<String> sscan(@ShardingParam String key, String cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        return redis.sscan(key, cursor, params);
    }

    @ReadOp
    @Override
    public ScanResult<Tuple> zscan(@ShardingParam String key, String cursor) {
        LogUtil.debugLog(resource, key);
        return redis.zscan(key, cursor);
    }

    @ReadOp
    @Override
    public ScanResult<Tuple> zscan(@ShardingParam String key, String cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        return redis.zscan(key, cursor, params);
    }

    @WriteOp
    @Override
    public Long pfadd(@ShardingParam String key, String... elements) {
        LogUtil.debugLog(resource, key);
        return redis.pfadd(key, elements);
    }

    @ReadOp
    @Override
    public long pfcount(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.pfcount(key);
    }

    @WriteOp
    @Override
    public Long geoadd(@ShardingParam String key, double longitude, double latitude, String member) {
        LogUtil.debugLog(resource, key);
        return redis.geoadd(key, longitude, latitude, member);
    }

    @WriteOp
    @Override
    public Long geoadd(@ShardingParam String key, Map<String, GeoCoordinate> memberCoordinateMap) {
        LogUtil.debugLog(resource, key);
        return redis.geoadd(key, memberCoordinateMap);
    }

    @ReadOp
    @Override
    public Double geodist(@ShardingParam String key, String member1, String member2) {
        LogUtil.debugLog(resource, key);
        return redis.geodist(key, member1, member2);
    }

    @ReadOp
    @Override
    public Double geodist(@ShardingParam String key, String member1, String member2, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        return redis.geodist(key, member1, member2, unit);
    }

    @ReadOp
    @Override
    public List<String> geohash(@ShardingParam String key, String... members) {
        LogUtil.debugLog(resource, key);
        return redis.geohash(key, members);
    }

    @ReadOp
    @Override
    public List<GeoCoordinate> geopos(@ShardingParam String key, String... members) {
        LogUtil.debugLog(resource, key);
        return redis.geopos(key, members);
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadius(@ShardingParam String key, double longitude, double latitude, double radius, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        return redis.georadius(key, longitude, latitude, radius, unit);
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadius(@ShardingParam String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        return redis.georadius(key, longitude, latitude, radius, unit, param);
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadiusByMember(@ShardingParam String key, String member, double radius, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        return redis.georadiusByMember(key, member, radius, unit);
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadiusByMember(@ShardingParam String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        return redis.georadiusByMember(key, member, radius, unit, param);
    }

    @ReadOp
    @Override
    public List<Long> bitfield(@ShardingParam String key, String... arguments) {
        LogUtil.debugLog(resource, key);
        return redis.bitfield(key, arguments);
    }

    @WriteOp
    @Override
    public Long del(@ShardingParam(type = ShardingParam.Type.Collection) byte[]... keys) {
        LogUtil.debugLog(resource, keys);
        return redis.del(keys);
    }

    @ReadOp
    @Override
    public Long exists(@ShardingParam(type = ShardingParam.Type.Collection) byte[]... keys) {
        LogUtil.debugLog(resource, keys);
        return redis.exists(keys);
    }

    @ReadOp
    @Override
    public Map<byte[], byte[]> mget(@ShardingParam(type = ShardingParam.Type.Collection) byte[]... keys) {
        LogUtil.debugLog(resource, keys);
        return redis.mget(keys);
    }

    @WriteOp
    @Override
    public String mset(@ShardingParam(type = ShardingParam.Type.Collection) Map<byte[], byte[]> keysvalues) {
        LogUtil.debugLog(resource, keysvalues);
        return redis.mset(keysvalues);
    }

    @WriteOp
    @Override
    public Long del(@ShardingParam(type = ShardingParam.Type.Collection) String... keys) {
        LogUtil.debugLog(resource, keys);
        return redis.del(keys);
    }

    @ReadOp
    @Override
    public Long exists(@ShardingParam(type = ShardingParam.Type.Collection) String... keys) {
        LogUtil.debugLog(resource, keys);
        return redis.exists(keys);
    }

    @ReadOp
    @Override
    public Map<String, String> mget(@ShardingParam(type = ShardingParam.Type.Collection) String... keys) {
        LogUtil.debugLog(resource, keys);
        return redis.mget(keys);
    }

    @WriteOp
    @Override
    public String set(@ShardingParam byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
        LogUtil.debugLog(resource, key);
        return redis.set(key, value, nxxx, expx, time);
    }

    @ReadOp
    @Override
    public Boolean exists(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.exists(key);
    }

    @WriteOp
    @Override
    public Long persist(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.persist(key);
    }

    @ReadOp
    @Override
    public String type(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.type(key);
    }

    @WriteOp
    @Override
    public Long expire(@ShardingParam byte[] key, int seconds) {
        LogUtil.debugLog(resource, key);
        return redis.expire(key, seconds);
    }

    @WriteOp
    @Override
    public Long pexpire(@ShardingParam byte[] key, long milliseconds) {
        LogUtil.debugLog(resource, key);
        return redis.pexpire(key, milliseconds);
    }

    @WriteOp
    @Override
    public Long expireAt(@ShardingParam byte[] key, long unixTime) {
        LogUtil.debugLog(resource, key);
        return redis.expireAt(key, unixTime);
    }

    @WriteOp
    @Override
    public Long pexpireAt(@ShardingParam byte[] key, long millisecondsTimestamp) {
        LogUtil.debugLog(resource, key);
        return redis.pexpireAt(key, millisecondsTimestamp);
    }

    @ReadOp
    @Override
    public Long ttl(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.ttl(key);
    }

    @ReadOp
    @Override
    public Long pttl(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.pttl(key);
    }

    @WriteOp
    @Override
    public Boolean setbit(@ShardingParam byte[] key, long offset, boolean value) {
        LogUtil.debugLog(resource, key);
        return redis.setbit(key, offset, value);
    }

    @WriteOp
    @Override
    public Boolean setbit(@ShardingParam byte[] key, long offset, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.setbit(key, offset, value);
    }

    @ReadOp
    @Override
    public Boolean getbit(@ShardingParam byte[] key, long offset) {
        LogUtil.debugLog(resource, key);
        return redis.getbit(key, offset);
    }

    @WriteOp
    @Override
    public Long setrange(@ShardingParam byte[] key, long offset, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.setrange(key, offset, value);
    }

    @ReadOp
    @Override
    public byte[] getrange(@ShardingParam byte[] key, long startOffset, long endOffset) {
        LogUtil.debugLog(resource, key);
        return redis.getrange(key, startOffset, endOffset);
    }

    @WriteOp
    @Override
    public byte[] getSet(@ShardingParam byte[] key, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.getSet(key, value);
    }

    @WriteOp
    @Override
    public Long setnx(@ShardingParam byte[] key, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.setnx(key, value);
    }

    @WriteOp
    @Override
    public String setex(@ShardingParam byte[] key, int seconds, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.setex(key, seconds, value);
    }

    @WriteOp
    @Override
    public String psetex(@ShardingParam byte[] key, long milliseconds, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.psetex(key, milliseconds, value);
    }

    @WriteOp
    @Override
    public Long decrBy(@ShardingParam byte[] key, long integer) {
        LogUtil.debugLog(resource, key);
        return redis.decrBy(key, integer);
    }

    @WriteOp
    @Override
    public Long decr(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.decr(key);
    }

    @WriteOp
    @Override
    public Long incrBy(@ShardingParam byte[] key, long integer) {
        LogUtil.debugLog(resource, key);
        return redis.incrBy(key, integer);
    }

    @WriteOp
    @Override
    public Double incrByFloat(@ShardingParam byte[] key, double value) {
        LogUtil.debugLog(resource, key);
        return redis.incrByFloat(key, value);
    }

    @WriteOp
    @Override
    public Long incr(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.incr(key);
    }

    @WriteOp
    @Override
    public Long append(@ShardingParam byte[] key, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.append(key, value);
    }

    @ReadOp
    @Override
    public byte[] substr(@ShardingParam byte[] key, int start, int end) {
        LogUtil.debugLog(resource, key);
        return redis.substr(key, start, end);
    }

    @WriteOp
    @Override
    public Long hset(@ShardingParam byte[] key, byte[] field, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.hset(key, field, value);
    }

    @ReadOp
    @Override
    public byte[] hget(@ShardingParam byte[] key, byte[] field) {
        LogUtil.debugLog(resource, key);
        return redis.hget(key, field);
    }

    @WriteOp
    @Override
    public Long hsetnx(@ShardingParam byte[] key, byte[] field, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.hsetnx(key, field, value);
    }

    @WriteOp
    @Override
    public String hmset(@ShardingParam byte[] key, Map<byte[], byte[]> hash) {
        LogUtil.debugLog(resource, key);
        return redis.hmset(key, hash);
    }

    @ReadOp
    @Override
    public List<byte[]> hmget(@ShardingParam byte[] key, byte[]... fields) {
        LogUtil.debugLog(resource, key);
        return redis.hmget(key, fields);
    }

    @WriteOp
    @Override
    public Long hincrBy(@ShardingParam byte[] key, byte[] field, long value) {
        LogUtil.debugLog(resource, key);
        return redis.hincrBy(key, field, value);
    }

    @WriteOp
    @Override
    public Double hincrByFloat(@ShardingParam byte[] key, byte[] field, double value) {
        LogUtil.debugLog(resource, key);
        return redis.hincrByFloat(key, field, value);
    }

    @ReadOp
    @Override
    public Boolean hexists(@ShardingParam byte[] key, byte[] field) {
        LogUtil.debugLog(resource, key);
        return redis.hexists(key, field);
    }

    @WriteOp
    @Override
    public Long hdel(@ShardingParam byte[] key, byte[]... field) {
        LogUtil.debugLog(resource, key);
        return redis.hdel(key, field);
    }

    @ReadOp
    @Override
    public Long hlen(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.hlen(key);
    }

    @ReadOp
    @Override
    public Set<byte[]> hkeys(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.hkeys(key);
    }

    @ReadOp
    @Override
    public List<byte[]> hvals(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.hvals(key);
    }

    @ReadOp
    @Override
    public Map<byte[], byte[]> hgetAll(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.hgetAll(key);
    }

    @WriteOp
    @Override
    public Long rpush(@ShardingParam byte[] key, byte[]... args) {
        LogUtil.debugLog(resource, key);
        return redis.rpush(key, args);
    }

    @WriteOp
    @Override
    public Long lpush(@ShardingParam byte[] key, byte[]... args) {
        LogUtil.debugLog(resource, key);
        return redis.lpush(key, args);
    }

    @ReadOp
    @Override
    public Long llen(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.llen(key);
    }

    @ReadOp
    @Override
    public List<byte[]> lrange(@ShardingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.lrange(key, start, end);
    }

    @ReadOp
    @Override
    public String ltrim(@ShardingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.ltrim(key, start, end);
    }

    @ReadOp
    @Override
    public byte[] lindex(@ShardingParam byte[] key, long index) {
        LogUtil.debugLog(resource, key);
        return redis.lindex(key, index);
    }

    @WriteOp
    @Override
    public String lset(@ShardingParam byte[] key, long index, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.lset(key, index, value);
    }

    @WriteOp
    @Override
    public Long lrem(@ShardingParam byte[] key, long count, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.lrem(key, count, value);
    }

    @WriteOp
    @Override
    public byte[] lpop(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.lpop(key);
    }

    @WriteOp
    @Override
    public byte[] rpop(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.rpop(key);
    }

    @WriteOp
    @Override
    public Long sadd(@ShardingParam byte[] key, byte[]... member) {
        LogUtil.debugLog(resource, key);
        return redis.sadd(key, member);
    }

    @ReadOp
    @Override
    public Set<byte[]> smembers(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.smembers(key);
    }

    @WriteOp
    @Override
    public Long srem(@ShardingParam byte[] key, byte[]... member) {
        LogUtil.debugLog(resource, key);
        return redis.srem(key, member);
    }

    @WriteOp
    @Override
    public byte[] spop(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.spop(key);
    }

    @WriteOp
    @Override
    public Set<byte[]> spop(@ShardingParam byte[] key, long count) {
        LogUtil.debugLog(resource, key);
        return redis.spop(key, count);
    }

    @ReadOp
    @Override
    public Long scard(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.scard(key);
    }

    @ReadOp
    @Override
    public Boolean sismember(@ShardingParam byte[] key, byte[] member) {
        LogUtil.debugLog(resource, key);
        return redis.sismember(key, member);
    }

    @ReadOp
    @Override
    public byte[] srandmember(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.srandmember(key);
    }

    @ReadOp
    @Override
    public List<byte[]> srandmember(@ShardingParam byte[] key, int count) {
        LogUtil.debugLog(resource, key);
        return redis.srandmember(key, count);
    }

    @ReadOp
    @Override
    public Long strlen(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.strlen(key);
    }

    @WriteOp
    @Override
    public Long zadd(@ShardingParam byte[] key, double score, byte[] member) {
        LogUtil.debugLog(resource, key);
        return redis.zadd(key, score, member);
    }

    @WriteOp
    @Override
    public Long zadd(@ShardingParam byte[] key, double score, byte[] member, ZAddParams params) {
        LogUtil.debugLog(resource, key);
        return redis.zadd(key, score, member, params);
    }

    @WriteOp
    @Override
    public Long zadd(@ShardingParam byte[] key, Map<byte[], Double> scoreMembers) {
        LogUtil.debugLog(resource, key);
        return redis.zadd(key, scoreMembers);
    }

    @WriteOp
    @Override
    public Long zadd(@ShardingParam byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
        LogUtil.debugLog(resource, key);
        return redis.zadd(key, scoreMembers, params);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrange(@ShardingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zrange(key, start, end);
    }

    @WriteOp
    @Override
    public Long zrem(@ShardingParam byte[] key, byte[]... member) {
        LogUtil.debugLog(resource, key);
        return redis.zrem(key, member);
    }

    @WriteOp
    @Override
    public Double zincrby(@ShardingParam byte[] key, double score, byte[] member) {
        LogUtil.debugLog(resource, key);
        return redis.zincrby(key, score, member);
    }

    @WriteOp
    @Override
    public Double zincrby(@ShardingParam byte[] key, double score, byte[] member, ZIncrByParams params) {
        LogUtil.debugLog(resource, key);
        return redis.zincrby(key, score, member, params);
    }

    @ReadOp
    @Override
    public Long zrank(@ShardingParam byte[] key, byte[] member) {
        LogUtil.debugLog(resource, key);
        return redis.zrank(key, member);
    }

    @ReadOp
    @Override
    public Long zrevrank(@ShardingParam byte[] key, byte[] member) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrank(key, member);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrange(@ShardingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrange(key, start, end);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeWithScores(@ShardingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeWithScores(key, start, end);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeWithScores(@ShardingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeWithScores(key, start, end);
    }

    @ReadOp
    @Override
    public Long zcard(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.zcard(key);
    }

    @ReadOp
    @Override
    public Double zscore(@ShardingParam byte[] key, byte[] member) {
        LogUtil.debugLog(resource, key);
        return redis.zscore(key, member);
    }

    @ReadOp
    @Override
    public List<byte[]> sort(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.sort(key);
    }

    @ReadOp
    @Override
    public List<byte[]> sort(@ShardingParam byte[] key, SortingParams sortingParameters) {
        LogUtil.debugLog(resource, key);
        return redis.sort(key, sortingParameters);
    }

    @ReadOp
    @Override
    public Long zcount(@ShardingParam byte[] key, double min, double max) {
        LogUtil.debugLog(resource, key);
        return redis.zcount(key, min, max);
    }

    @ReadOp
    @Override
    public Long zcount(@ShardingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        return redis.zcount(key, min, max);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByScore(@ShardingParam byte[] key, double min, double max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScore(key, min, max);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByScore(@ShardingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScore(key, min, max);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByScore(@ShardingParam byte[] key, double max, double min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScore(key, max, min);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByScore(@ShardingParam byte[] key, double min, double max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScore(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByScore(@ShardingParam byte[] key, byte[] max, byte[] min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScore(key, max, min);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByScore(@ShardingParam byte[] key, byte[] min, byte[] max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScore(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByScore(@ShardingParam byte[] key, double max, double min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScore(key, max, min, offset, count);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShardingParam byte[] key, double min, double max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScoreWithScores(key, min, max);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShardingParam byte[] key, double max, double min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScoreWithScores(key, max, min);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShardingParam byte[] key, double min, double max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByScore(@ShardingParam byte[] key, byte[] max, byte[] min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScore(key, max, min, offset, count);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShardingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScoreWithScores(key, min, max);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShardingParam byte[] key, byte[] max, byte[] min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScoreWithScores(key, max, min);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShardingParam byte[] key, byte[] min, byte[] max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShardingParam byte[] key, double max, double min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShardingParam byte[] key, byte[] max, byte[] min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @WriteOp
    @Override
    public Long zremrangeByRank(@ShardingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.zremrangeByRank(key, start, end);
    }

    @WriteOp
    @Override
    public Long zremrangeByScore(@ShardingParam byte[] key, double start, double end) {
        LogUtil.debugLog(resource, key);
        return redis.zremrangeByScore(key, start, end);
    }

    @WriteOp
    @Override
    public Long zremrangeByScore(@ShardingParam byte[] key, byte[] start, byte[] end) {
        LogUtil.debugLog(resource, key);
        return redis.zremrangeByScore(key, start, end);
    }

    @ReadOp
    @Override
    public Long zlexcount(@ShardingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        return redis.zlexcount(key, min, max);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByLex(@ShardingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByLex(key, min, max);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByLex(@ShardingParam byte[] key, byte[] min, byte[] max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrangeByLex(key, min, max, offset, count);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByLex(@ShardingParam byte[] key, byte[] max, byte[] min) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByLex(key, max, min);
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByLex(@ShardingParam byte[] key, byte[] max, byte[] min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        return redis.zrevrangeByLex(key, max, min, offset, count);
    }

    @WriteOp
    @Override
    public Long zremrangeByLex(@ShardingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        return redis.zremrangeByLex(key, min, max);
    }

    @WriteOp
    @Override
    public Long linsert(@ShardingParam byte[] key, BinaryClient.LIST_POSITION where, byte[] pivot, byte[] value) {
        LogUtil.debugLog(resource, key);
        return redis.linsert(key, where, pivot, value);
    }

    @WriteOp
    @Override
    public Long lpushx(@ShardingParam byte[] key, byte[]... arg) {
        LogUtil.debugLog(resource, key);
        return redis.lpushx(key, arg);
    }

    @WriteOp
    @Override
    public Long rpushx(@ShardingParam byte[] key, byte[]... arg) {
        LogUtil.debugLog(resource, key);
        return redis.rpushx(key, arg);
    }

    @WriteOp
    @Override
    public Long del(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.del(key);
    }


    @ReadOp
    @Override
    public Long bitcount(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.bitcount(key);
    }

    @ReadOp
    @Override
    public Long bitcount(@ShardingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        return redis.bitcount(key, start, end);
    }

    @WriteOp
    @Override
    public Long pfadd(@ShardingParam byte[] key, byte[]... elements) {
        LogUtil.debugLog(resource, key);
        return redis.pfadd(key, elements);
    }

    @ReadOp
    @Override
    public long pfcount(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.pfcount(key);
    }

    @WriteOp
    @Override
    public Long geoadd(@ShardingParam byte[] key, double longitude, double latitude, byte[] member) {
        LogUtil.debugLog(resource, key);
        return redis.geoadd(key, longitude, latitude, member);
    }

    @WriteOp
    @Override
    public Long geoadd(@ShardingParam byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
        LogUtil.debugLog(resource, key);
        return redis.geoadd(key, memberCoordinateMap);
    }

    @ReadOp
    @Override
    public Double geodist(@ShardingParam byte[] key, byte[] member1, byte[] member2) {
        LogUtil.debugLog(resource, key);
        return redis.geodist(key, member1, member2);
    }

    @ReadOp
    @Override
    public Double geodist(@ShardingParam byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        return redis.geodist(key, member1, member2, unit);
    }

    @ReadOp
    @Override
    public List<byte[]> geohash(@ShardingParam byte[] key, byte[]... members) {
        LogUtil.debugLog(resource, key);
        return redis.geohash(key, members);
    }

    @ReadOp
    @Override
    public List<GeoCoordinate> geopos(@ShardingParam byte[] key, byte[]... members) {
        LogUtil.debugLog(resource, key);
        return redis.geopos(key, members);
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadius(@ShardingParam byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        return redis.georadius(key, longitude, latitude, radius, unit);
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadius(@ShardingParam byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        return redis.georadius(key, longitude, latitude, radius, unit, param);
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadiusByMember(@ShardingParam byte[] key, byte[] member, double radius, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        return redis.georadiusByMember(key, member, radius, unit);
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadiusByMember(@ShardingParam byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        return redis.georadiusByMember(key, member, radius, unit, param);
    }

    @ReadOp
    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(@ShardingParam byte[] key, byte[] cursor) {
        LogUtil.debugLog(resource, key);
        return redis.hscan(key, cursor);
    }

    @ReadOp
    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(@ShardingParam byte[] key, byte[] cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        return redis.hscan(key, cursor, params);
    }

    @ReadOp
    @Override
    public ScanResult<byte[]> sscan(@ShardingParam byte[] key, byte[] cursor) {
        LogUtil.debugLog(resource, key);
        return redis.sscan(key, cursor);
    }

    @ReadOp
    @Override
    public ScanResult<byte[]> sscan(@ShardingParam byte[] key, byte[] cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        return redis.sscan(key, cursor, params);
    }

    @ReadOp
    @Override
    public ScanResult<Tuple> zscan(@ShardingParam byte[] key, byte[] cursor) {
        LogUtil.debugLog(resource, key);
        return redis.zscan(key, cursor);
    }

    @ReadOp
    @Override
    public ScanResult<Tuple> zscan(@ShardingParam byte[] key, byte[] cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        return redis.zscan(key, cursor, params);
    }

    @ReadOp
    @Override
    public List<Long> bitfield(@ShardingParam byte[] key, byte[]... arguments) {
        LogUtil.debugLog(resource, key);
        return redis.bitfield(key, arguments);
    }

    @WriteOp
    @Override
    public String set(@ShardingParam String key, String value, String nxxx, String expx, long time) {
        LogUtil.debugLog(resource, key);
        return redis.set(key, value, nxxx, expx, time);
    }

    @WriteOp
    @Override
    public String set(@ShardingParam String key, String value, String nxxx) {
        LogUtil.debugLog(resource, key);
        return redis.set(key, value, nxxx);
    }

    @WriteOp
    @Override
    public String set(@ShardingParam byte[] key, byte[] value, byte[] nxxx) {
        LogUtil.debugLog(resource, key);
        return redis.set(key, value, nxxx);
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
    public byte[] dump(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        return redis.dump(key);
    }

    @ReadOp
    @Override
    public byte[] dump(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        return redis.dump(key);
    }

    @WriteOp
    @Override
    public String restore(@ShardingParam byte[] key, int ttl, byte[] serializedValue) {
        LogUtil.debugLog(resource, key);
        return redis.restore(key, ttl, serializedValue);
    }

    @WriteOp
    @Override
    public String restore(@ShardingParam String key, int ttl, byte[] serializedValue) {
        LogUtil.debugLog(resource, key);
        return redis.restore(key, ttl, serializedValue);
    }
}
