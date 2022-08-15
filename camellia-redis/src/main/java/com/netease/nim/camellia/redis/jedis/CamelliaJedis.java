package com.netease.nim.camellia.redis.jedis;

import com.netease.nim.camellia.redis.ICamelliaRedis;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.proxy.RedisProxyResource;
import com.netease.nim.camellia.redis.resource.CamelliaRedisProxyResource;
import com.netease.nim.camellia.redis.resource.RedisResource;
import com.netease.nim.camellia.redis.resource.RedisSentinelResource;
import com.netease.nim.camellia.redis.resource.RedisSentinelSlavesResource;
import com.netease.nim.camellia.redis.util.CloseUtil;
import redis.clients.jedis.*;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;
import redis.clients.util.Pool;

import java.util.*;

/**
 * 封装了Jedis的接口
 * Created by caojiajun on 2019/7/19.
 */
public class CamelliaJedis implements ICamelliaRedis {

    private final Pool<Jedis> jedisPool;

    public CamelliaJedis(RedisResource resource, CamelliaRedisEnv env) {
        this.jedisPool = env.getJedisPoolFactory().getJedisPool(resource);
    }

    public CamelliaJedis(RedisSentinelResource resource, CamelliaRedisEnv env) {
        this.jedisPool = env.getJedisPoolFactory().getJedisSentinelPool(resource);
    }

    public CamelliaJedis(RedisProxyResource resource, CamelliaRedisEnv env) {
        this.jedisPool = resource.getJedisPool();
    }

    public CamelliaJedis(CamelliaRedisProxyResource resource, CamelliaRedisEnv env) {
        this.jedisPool = env.getJedisPoolFactory().getCamelliaJedisPool(resource);
    }

    public CamelliaJedis(RedisSentinelSlavesResource resource, CamelliaRedisEnv env) {
        this.jedisPool = env.getJedisPoolFactory().getJedisSentinelSlavesPool(resource);
    }

    @Override
    public List<Jedis> getJedisList() {
        return Collections.singletonList(jedisPool.getResource());
    }

    @Override
    public String set(String key, String value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.set(key, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String set(String key, String value, String nxxx, String expx, long time) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.set(key, value, nxxx, expx, time);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String set(String key, String value, String nxxx) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.set(key, value, nxxx);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String set(byte[] key, byte[] value, byte[] nxxx) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.set(key, value, nxxx);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String get(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.get(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Boolean exists(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.exists(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long persist(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.persist(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String type(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.type(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long expire(String key, int seconds) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.expire(key, seconds);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long pexpire(String key, long milliseconds) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.pexpire(key, milliseconds);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long expireAt(String key, long unixTime) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.expireAt(key, unixTime);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long pexpireAt(String key, long millisecondsTimestamp) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.pexpireAt(key, millisecondsTimestamp);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long ttl(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.ttl(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long pttl(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.pttl(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Boolean setbit(String key, long offset, boolean value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.setbit(key, offset, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Boolean setbit(String key, long offset, String value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.setbit(key, offset, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Boolean getbit(String key, long offset) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.getbit(key, offset);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long setrange(String key, long offset, String value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.setrange(key, offset, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String getrange(String key, long startOffset, long endOffset) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.getrange(key, startOffset, endOffset);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String getSet(String key, String value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.getSet(key, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long setnx(String key, String value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.setnx(key, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String setex(String key, int seconds, String value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.setex(key, seconds, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String psetex(String key, long milliseconds, String value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.psetex(key, milliseconds, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long decrBy(String key, long integer) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.decrBy(key, integer);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long decr(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.decr(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long incrBy(String key, long integer) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.incrBy(key, integer);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Double incrByFloat(String key, double value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.incrByFloat(key, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long incr(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.incr(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long append(String key, String value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.append(key, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String substr(String key, int start, int end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.substr(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long hset(String key, String field, String value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hset(key, field, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String hget(String key, String field) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hget(key, field);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long hsetnx(String key, String field, String value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hsetnx(key, field, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String hmset(String key, Map<String, String> hash) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hmset(key, hash);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<String> hmget(String key, String... fields) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hmget(key, fields);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long hincrBy(String key, String field, long value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hincrBy(key, field, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Double hincrByFloat(String key, String field, double value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hincrByFloat(key, field, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Boolean hexists(String key, String field) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hexists(key, field);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long hdel(String key, String... field) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hdel(key, field);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long hlen(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hlen(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<String> hkeys(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hkeys(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<String> hvals(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hvals(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hgetAll(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long rpush(String key, String... string) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.rpush(key, string);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long lpush(String key, String... string) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.lpush(key, string);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long llen(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.llen(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<String> lrange(String key, long start, long end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.lrange(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String ltrim(String key, long start, long end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.ltrim(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String lindex(String key, long index) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.lindex(key, index);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String lset(String key, long index, String value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.lset(key, index, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long lrem(String key, long count, String value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.lrem(key, count, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String lpop(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.lpop(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String rpop(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.rpop(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long sadd(String key, String... member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.sadd(key, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<String> smembers(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.smembers(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long srem(String key, String... member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.srem(key, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String spop(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.spop(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<String> spop(String key, long count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.spop(key, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long scard(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.scard(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Boolean sismember(String key, String member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.sismember(key, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String srandmember(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.srandmember(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<String> srandmember(String key, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.srandmember(key, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long strlen(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.strlen(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zadd(String key, double score, String member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zadd(key, score, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zadd(String key, double score, String member, ZAddParams params) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zadd(key, score, member, params);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zadd(String key, Map<String, Double> scoreMembers) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zadd(key, scoreMembers);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zadd(key, scoreMembers, params);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<String> zrange(String key, long start, long end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrange(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zrem(String key, String... member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrem(key, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Double zincrby(String key, double score, String member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zincrby(key, score, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Double zincrby(String key, double score, String member, ZIncrByParams params) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zincrby(key, score, member, params);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zrank(String key, String member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrank(key, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zrevrank(String key, String member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrank(key, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<String> zrevrange(String key, long start, long end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrange(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrangeWithScores(String key, long start, long end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeWithScores(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeWithScores(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zcard(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zcard(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Double zscore(String key, String member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zscore(key, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<String> sort(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.sort(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<String> sort(String key, SortingParams sortingParameters) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.sort(key, sortingParameters);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zcount(String key, double min, double max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zcount(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zcount(String key, String min, String max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zcount(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByScore(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<String> zrangeByScore(String key, String min, String max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByScore(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByScore(key, max, min);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByScore(key, min, max, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<String> zrevrangeByScore(String key, String max, String min) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByScore(key, max, min);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByScore(key, min, max, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByScore(key, max, min, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByScoreWithScores(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByScoreWithScores(key, max, min);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByScoreWithScores(key, min, max, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByScore(key, max, min, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByScoreWithScores(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByScoreWithScores(key, max, min);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByScoreWithScores(key, min, max, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByScoreWithScores(key, max, min, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByScoreWithScores(key, max, min, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zremrangeByRank(String key, long start, long end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zremrangeByRank(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zremrangeByScore(String key, double start, double end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zremrangeByScore(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zremrangeByScore(String key, String start, String end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zremrangeByScore(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zlexcount(String key, String min, String max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zlexcount(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByLex(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByLex(key, min, max, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByLex(key, max, min);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByLex(key, max, min, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zremrangeByLex(String key, String min, String max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zremrangeByLex(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long linsert(String key, BinaryClient.LIST_POSITION where, String pivot, String value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.linsert(key, where, pivot, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long lpushx(String key, String... string) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.lpushx(key, string);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long rpushx(String key, String... string) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.rpushx(key, string);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long del(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.del(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String echo(String string) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.echo(string);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long bitcount(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.bitcount(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long bitcount(String key, long start, long end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.bitcount(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long bitpos(String key, boolean value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.bitpos(key, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long bitpos(byte[] key, boolean value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.bitpos(key, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long bitpos(byte[] key, boolean value, BitPosParams params) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.bitpos(key, value, params);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long bitpos(String key, boolean value, BitPosParams params) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.bitpos(key, value, params);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hscan(key, cursor);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hscan(key, cursor, params);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.sscan(key, cursor);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.sscan(key, cursor, params);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zscan(key, cursor);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zscan(key, cursor, params);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long pfadd(String key, String... elements) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.pfadd(key, elements);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public long pfcount(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.pfcount(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long geoadd(String key, double longitude, double latitude, String member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.geoadd(key, longitude, latitude, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.geoadd(key, memberCoordinateMap);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Double geodist(String key, String member1, String member2) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.geodist(key, member1, member2);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Double geodist(String key, String member1, String member2, GeoUnit unit) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.geodist(key, member1, member2, unit);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<String> geohash(String key, String... members) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.geohash(key, members);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<GeoCoordinate> geopos(String key, String... members) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.geopos(key, members);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.georadius(key, longitude, latitude, radius, unit);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.georadius(key, longitude, latitude, radius, unit, param);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.georadiusByMember(key, member, radius, unit);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.georadiusByMember(key, member, radius, unit, param);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<Long> bitfield(String key, String... arguments) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.bitfield(key, arguments);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long del(byte[]... keys) {
        if (keys == null) return 0L;
        if (keys.length == 0) return 0L;
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.del(keys);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long exists(byte[]... keys) {
        if (keys == null) return 0L;
        if (keys.length == 0) return 0L;
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.exists(keys);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Map<byte[], byte[]> mget(byte[]... keys) {
        if (keys == null) return Collections.emptyMap();
        if (keys.length == 0) return Collections.emptyMap();
        Jedis jedis = jedisPool.getResource();
        try {
            Map<byte[], byte[]> ret = new HashMap<>();
            List<byte[]> mget = jedis.mget(keys);
            for (int i=0; i<keys.length; i++) {
                byte[] key = keys[i];
                byte[] value = mget.get(i);
                ret.put(key, value);
            }
            return ret;
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String mset(Map<byte[], byte[]> keysvalues) {
        if (keysvalues == null) return null;
        if (keysvalues.isEmpty()) return null;
        Jedis jedis = jedisPool.getResource();
        try {
            List<byte[]> args = new ArrayList<>();
            for (Map.Entry<byte[], byte[]> entry : keysvalues.entrySet()) {
                args.add(entry.getKey());
                args.add(entry.getValue());
            }
            byte[][] bytes = args.toArray(new byte[0][0]);
            return jedis.mset(bytes);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long del(String... keys) {
        if (keys == null) return 0L;
        if (keys.length == 0) return 0L;
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.del(keys);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long exists(String... keys) {
        if (keys == null) return 0L;
        if (keys.length == 0) return 0L;
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.exists(keys);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Map<String, String> mget(String... keys) {
        if (keys == null) return Collections.emptyMap();
        if (keys.length == 0) return Collections.emptyMap();
        Jedis jedis = jedisPool.getResource();
        try {
            Map<String, String> ret = new HashMap<>();
            List<String> list = jedis.mget(keys);
            for (int i=0; i<keys.length; i++) {
                String key = keys[i];
                String value = list.get(i);
                ret.put(key, value);
            }
            return ret;
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Jedis getJedis(byte[] key) {
        return jedisPool.getResource();
    }

    @Override
    public String set(byte[] key, byte[] value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.set(key, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public byte[] get(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.get(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.set(key, value, nxxx, expx, time);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Boolean exists(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.exists(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long persist(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.persist(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String type(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.type(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long expire(byte[] key, int seconds) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.expire(key, seconds);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long pexpire(byte[] key, long milliseconds) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.pexpire(key, milliseconds);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long expireAt(byte[] key, long unixTime) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.expireAt(key, unixTime);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long pexpireAt(byte[] key, long millisecondsTimestamp) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.pexpireAt(key, millisecondsTimestamp);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long ttl(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.ttl(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long pttl(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.pttl(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Boolean setbit(byte[] key, long offset, boolean value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.setbit(key, offset, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Boolean setbit(byte[] key, long offset, byte[] value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.setbit(key, offset, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Boolean getbit(byte[] key, long offset) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.getbit(key, offset);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long setrange(byte[] key, long offset, byte[] value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.setrange(key, offset, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public byte[] getrange(byte[] key, long startOffset, long endOffset) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.getrange(key, startOffset, endOffset);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public byte[] getSet(byte[] key, byte[] value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.getSet(key, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long setnx(byte[] key, byte[] value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.setnx(key, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String setex(byte[] key, int seconds, byte[] value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.setex(key, seconds, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String psetex(byte[] key, long milliseconds, byte[] value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.psetex(key, milliseconds, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long decrBy(byte[] key, long integer) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.decrBy(key, integer);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long decr(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.decr(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long incrBy(byte[] key, long integer) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.incrBy(key, integer);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Double incrByFloat(byte[] key, double value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.incrByFloat(key, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long incr(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.incr(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long append(byte[] key, byte[] value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.append(key, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public byte[] substr(byte[] key, int start, int end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.substr(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long hset(byte[] key, byte[] field, byte[] value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hset(key, field, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public byte[] hget(byte[] key, byte[] field) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hget(key, field);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long hsetnx(byte[] key, byte[] field, byte[] value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hsetnx(key, field, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String hmset(byte[] key, Map<byte[], byte[]> hash) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hmset(key, hash);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<byte[]> hmget(byte[] key, byte[]... fields) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hmget(key, fields);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long hincrBy(byte[] key, byte[] field, long value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hincrBy(key, field, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Double hincrByFloat(byte[] key, byte[] field, double value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hincrByFloat(key, field, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Boolean hexists(byte[] key, byte[] field) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hexists(key, field);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long hdel(byte[] key, byte[]... field) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hdel(key, field);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long hlen(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hlen(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<byte[]> hkeys(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hkeys(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<byte[]> hvals(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hvals(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Map<byte[], byte[]> hgetAll(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hgetAll(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long rpush(byte[] key, byte[]... args) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.rpush(key, args);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long lpush(byte[] key, byte[]... args) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.lpush(key, args);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long llen(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.llen(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<byte[]> lrange(byte[] key, long start, long end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.lrange(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String ltrim(byte[] key, long start, long end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.ltrim(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public byte[] lindex(byte[] key, long index) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.lindex(key, index);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String lset(byte[] key, long index, byte[] value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.lset(key, index, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long lrem(byte[] key, long count, byte[] value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.lrem(key, count, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public byte[] lpop(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.lpop(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public byte[] rpop(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.rpop(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long sadd(byte[] key, byte[]... member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.sadd(key, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<byte[]> smembers(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.smembers(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long srem(byte[] key, byte[]... member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.srem(key, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public byte[] spop(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.spop(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<byte[]> spop(byte[] key, long count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.spop(key, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long scard(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.scard(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Boolean sismember(byte[] key, byte[] member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.sismember(key, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public byte[] srandmember(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.srandmember(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<byte[]> srandmember(byte[] key, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.srandmember(key, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long strlen(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.strlen(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zadd(byte[] key, double score, byte[] member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zadd(key, score, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zadd(byte[] key, double score, byte[] member, ZAddParams params) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zadd(key, score, member, params);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zadd(byte[] key, Map<byte[], Double> scoreMembers) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zadd(key, scoreMembers);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zadd(key, scoreMembers, params);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<byte[]> zrange(byte[] key, long start, long end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrange(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zrem(byte[] key, byte[]... member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrem(key, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Double zincrby(byte[] key, double score, byte[] member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zincrby(key, score, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Double zincrby(byte[] key, double score, byte[] member, ZIncrByParams params) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zincrby(key, score, member, params);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zrank(byte[] key, byte[] member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrank(key, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zrevrank(byte[] key, byte[] member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrank(key, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<byte[]> zrevrange(byte[] key, long start, long end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrange(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrangeWithScores(byte[] key, long start, long end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeWithScores(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(byte[] key, long start, long end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeWithScores(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zcard(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zcard(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Double zscore(byte[] key, byte[] member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zscore(key, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<byte[]> sort(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.sort(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<byte[]> sort(byte[] key, SortingParams sortingParameters) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.sort(key, sortingParameters);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zcount(byte[] key, double min, double max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zcount(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zcount(byte[] key, byte[] min, byte[] max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zcount(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByScore(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByScore(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByScore(key, max, min);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByScore(key, min, max, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByScore(key, max, min);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByScore(key, min, max, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByScore(key, max, min, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByScoreWithScores(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByScoreWithScores(key, max, min);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByScoreWithScores(key, min, max, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByScore(key, max, min, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByScoreWithScores(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByScoreWithScores(key, max, min);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByScoreWithScores(key, min, max, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByScoreWithScores(key, max, min, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByScoreWithScores(key, max, min, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zremrangeByRank(byte[] key, long start, long end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zremrangeByRank(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zremrangeByScore(byte[] key, double start, double end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zremrangeByScore(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zremrangeByScore(byte[] key, byte[] start, byte[] end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zremrangeByScore(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zlexcount(byte[] key, byte[] min, byte[] max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zlexcount(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByLex(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrangeByLex(key, min, max, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByLex(key, max, min);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zrevrangeByLex(key, max, min, offset, count);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long zremrangeByLex(byte[] key, byte[] min, byte[] max) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zremrangeByLex(key, min, max);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long linsert(byte[] key, BinaryClient.LIST_POSITION where, byte[] pivot, byte[] value) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.linsert(key, where, pivot, value);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long lpushx(byte[] key, byte[]... arg) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.lpushx(key, arg);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long rpushx(byte[] key, byte[]... arg) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.rpushx(key, arg);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long del(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.del(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long bitcount(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.bitcount(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long bitcount(byte[] key, long start, long end) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.bitcount(key, start, end);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long pfadd(byte[] key, byte[]... elements) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.pfadd(key, elements);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public long pfcount(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.pfcount(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long geoadd(byte[] key, double longitude, double latitude, byte[] member) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.geoadd(key, longitude, latitude, member);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Long geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.geoadd(key, memberCoordinateMap);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Double geodist(byte[] key, byte[] member1, byte[] member2) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.geodist(key, member1, member2);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Double geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.geodist(key, member1, member2, unit);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<byte[]> geohash(byte[] key, byte[]... members) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.geohash(key, members);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<GeoCoordinate> geopos(byte[] key, byte[]... members) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.geopos(key, members);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.georadius(key, longitude, latitude, radius, unit);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.georadius(key, longitude, latitude, radius, unit, param);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.georadiusByMember(key, member, radius, unit);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.georadiusByMember(key, member, radius, unit, param);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hscan(key, cursor);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor, ScanParams params) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.hscan(key, cursor, params);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public ScanResult<byte[]> sscan(byte[] key, byte[] cursor) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.sscan(key, cursor);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams params) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.sscan(key, cursor, params);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public ScanResult<Tuple> zscan(byte[] key, byte[] cursor) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zscan(key, cursor);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams params) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.zscan(key, cursor, params);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public List<Long> bitfield(byte[] key, byte[]... arguments) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.bitfield(key, arguments);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Object eval(byte[] script, int keyCount, byte[]... params) {
        Jedis jedis = jedisPool.getResource();
        try {
            Client client = jedis.getClient();
            client.eval(script, keyCount, params);
            return client.getOne();
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public Object evalsha(byte[] sha1, int keyCount, byte[]... params) {
        Jedis jedis = jedisPool.getResource();
        try {
            Client client = jedis.getClient();
            client.evalsha(sha1, keyCount, params);
            return client.getOne();
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public byte[] dump(String key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.dump(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public byte[] dump(byte[] key) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.dump(key);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String restore(byte[] key, int ttl, byte[] serializedValue) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.restore(key, ttl, serializedValue);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public String restore(String key, int ttl, byte[] serializedValue) {
        Jedis jedis = jedisPool.getResource();
        try {
            return jedis.restore(key, ttl, serializedValue);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }
}
