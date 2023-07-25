package com.netease.nim.camellia.redis.pipeline;

import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import redis.clients.jedis.*;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 调用完读写操作之后，需要先调用sync方法获取结果
 * 以上操作可以多次
 * 最后，需要调用close方法释放资源
 * Created by caojiajun on 2019/7/22.
 */
public class CamelliaRedisPipeline implements ICamelliaRedisPipeline {

    private final AtomicBoolean close = new AtomicBoolean(false);

    private final ICamelliaRedisPipeline pipeline;
    private final ResponseQueable queable;
    private final RedisClientPool redisClientPool;
    private final PipelinePool pipelinePool;

    public CamelliaRedisPipeline(ICamelliaRedisPipeline pipeline,
                                 ResponseQueable queable,
                                 RedisClientPool redisClientPool,
                                 PipelinePool pipelinePool) {
        this.pipeline = pipeline;
        this.queable = queable;
        this.redisClientPool = redisClientPool;
        this.pipelinePool = pipelinePool;
    }

    @Override
    public void sync() {
        try {
            queable.sync(redisClientPool);
        } finally {
            queable.clear();
            redisClientPool.clear();
        }
    }

    @Override
    public void close() {
        try {
            sync();
        } finally {
            if (close.compareAndSet(false, true)) {
                //放回pool，可以复用pipeline对象
                pipelinePool.set(this, () -> close.compareAndSet(true, false));
            }
        }
    }

    private void check() {
        if (close.get()) {
            throw new CamelliaRedisException("pipeline has closed");
        }
    }

    @Override
    public Response<String> set(byte[] key, byte[] value) {
        check();
        return pipeline.set(key, value);
    }

    @Override
    public Response<String> set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
        check();
        return pipeline.set(key, value, nxxx, expx, time);
    }

    @Override
    public Response<byte[]> get(byte[] key) {
        check();
        return pipeline.get(key);
    }

    @Override
    public Response<String> get(String key) {
        check();
        return pipeline.get(key);
    }

    @Override
    public Response<String> set(String key, String value) {
        check();
        return pipeline.set(key, value);
    }

    @Override
    public Response<Long> append(byte[] key, byte[] value) {
        check();
        return pipeline.append(key, value);
    }

    @Override
    public Response<Long> decr(byte[] key) {
        check();
        return pipeline.decr(key);
    }

    @Override
    public Response<Long> decrBy(byte[] key, long integer) {
        check();
        return pipeline.decrBy(key, integer);
    }

    @Override
    public Response<Long> del(byte[] key) {
        check();
        return pipeline.del(key);
    }

    @Override
    public Response<byte[]> echo(byte[] string) {
        check();
        return pipeline.echo(string);
    }

    @Override
    public Response<Boolean> exists(byte[] key) {
        check();
        return pipeline.exists(key);
    }

    @Override
    public Response<Long> expire(byte[] key, int seconds) {
        check();
        return pipeline.expire(key, seconds);
    }

    @Override
    public Response<Long> pexpire(byte[] key, long milliseconds) {
        check();
        return pipeline.pexpire(key, milliseconds);
    }

    @Override
    public Response<Long> expireAt(byte[] key, long unixTime) {
        check();
        return pipeline.expireAt(key, unixTime);
    }

    @Override
    public Response<Long> pexpireAt(byte[] key, long millisecondsTimestamp) {
        check();
        return pipeline.pexpireAt(key, millisecondsTimestamp);
    }

    @Override
    public Response<Boolean> getbit(byte[] key, long offset) {
        check();
        return pipeline.getbit(key, offset);
    }

    @Override
    public Response<byte[]> getSet(byte[] key, byte[] value) {
        check();
        return pipeline.getSet(key, value);
    }

    @Override
    public Response<byte[]> getrange(byte[] key, long startOffset, long endOffset) {
        check();
        return pipeline.getrange(key, startOffset, endOffset);
    }

    @Override
    public Response<Long> hdel(byte[] key, byte[]... field) {
        check();
        return pipeline.hdel(key, field);
    }

    @Override
    public Response<Boolean> hexists(byte[] key, byte[] field) {
        check();
        return pipeline.hexists(key, field);
    }

    @Override
    public Response<byte[]> hget(byte[] key, byte[] field) {
        check();
        return pipeline.hget(key, field);
    }

    @Override
    public Response<Map<byte[], byte[]>> hgetAll(byte[] key) {
        check();
        return pipeline.hgetAll(key);
    }

    @Override
    public Response<Long> hincrBy(byte[] key, byte[] field, long value) {
        check();
        return pipeline.hincrBy(key, field, value);
    }

    @Override
    public Response<Double> hincrByFloat(byte[] key, byte[] field, double value) {
        check();
        return pipeline.hincrByFloat(key, field, value);
    }

    @Override
    public Response<Set<byte[]>> hkeys(byte[] key) {
        check();
        return pipeline.hkeys(key);
    }

    @Override
    public Response<Long> hlen(byte[] key) {
        check();
        return pipeline.hlen(key);
    }

    @Override
    public Response<List<byte[]>> hmget(byte[] key, byte[]... fields) {
        check();
        return pipeline.hmget(key, fields);
    }

    @Override
    public Response<String> hmset(byte[] key, Map<byte[], byte[]> hash) {
        check();
        return pipeline.hmset(key, hash);
    }

    @Override
    public Response<Long> hset(byte[] key, byte[] field, byte[] value) {
        check();
        return pipeline.hset(key, field, value);
    }

    @Override
    public Response<Long> hsetnx(byte[] key, byte[] field, byte[] value) {
        check();
        return pipeline.hsetnx(key, field, value);
    }

    @Override
    public Response<List<byte[]>> hvals(byte[] key) {
        check();
        return pipeline.hvals(key);
    }

    @Override
    public Response<Long> incr(byte[] key) {
        check();
        return pipeline.incr(key);
    }

    @Override
    public Response<Long> incrBy(byte[] key, long integer) {
        check();
        return pipeline.incrBy(key, integer);
    }

    @Override
    public Response<Double> incrByFloat(byte[] key, double integer) {
        check();
        return pipeline.incrByFloat(key, integer);
    }

    @Override
    public Response<byte[]> lindex(byte[] key, long index) {
        check();
        return pipeline.lindex(key, index);
    }

    @Override
    public Response<Long> llen(byte[] key) {
        check();
        return pipeline.llen(key);
    }

    @Override
    public Response<byte[]> lpop(byte[] key) {
        check();
        return pipeline.lpop(key);
    }

    @Override
    public Response<Long> linsert(String key, ListPosition where, String pivot, String value) {
        check();
        return pipeline.linsert(key, where, pivot, value);
    }

    @Override
    public Response<Long> linsert(byte[] key, ListPosition where, byte[] pivot, byte[] value) {
        check();
        return pipeline.linsert(key, where, pivot, value);
    }

    @Override
    public Response<Long> lpush(byte[] key, byte[]... string) {
        check();
        return pipeline.lpush(key, string);
    }

    @Override
    public Response<Long> lpushx(byte[] key, byte[]... bytes) {
        check();
        return pipeline.lpushx(key, bytes);
    }

    @Override
    public Response<List<byte[]>> lrange(byte[] key, long start, long end) {
        check();
        return pipeline.lrange(key, start, end);
    }

    @Override
    public Response<Long> lrem(byte[] key, long count, byte[] value) {
        check();
        return pipeline.lrem(key, count, value);
    }

    @Override
    public Response<String> lset(byte[] key, long index, byte[] value) {
        check();
        return pipeline.lset(key, index, value);
    }

    @Override
    public Response<String> ltrim(byte[] key, long start, long end) {
        check();
        return pipeline.ltrim(key, start, end);
    }

    @Override
    public Response<Long> persist(byte[] key) {
        check();
        return pipeline.persist(key);
    }

    @Override
    public Response<byte[]> rpop(byte[] key) {
        check();
        return pipeline.rpop(key);
    }

    @Override
    public Response<Long> rpush(byte[] key, byte[]... string) {
        check();
        return pipeline.rpush(key, string);
    }

    @Override
    public Response<Long> rpushx(byte[] key, byte[]... string) {
        check();
        return pipeline.rpushx(key, string);
    }

    @Override
    public Response<Long> sadd(byte[] key, byte[]... member) {
        check();
        return pipeline.sadd(key, member);
    }

    @Override
    public Response<Long> scard(byte[] key) {
        check();
        return pipeline.scard(key);
    }

    @Override
    public Response<Boolean> setbit(byte[] key, long offset, byte[] value) {
        check();
        return pipeline.setbit(key, offset, value);
    }

    @Override
    public Response<Long> setrange(byte[] key, long offset, byte[] value) {
        check();
        return pipeline.setrange(key, offset, value);
    }

    @Override
    public Response<String> setex(byte[] key, int seconds, byte[] value) {
        check();
        return pipeline.setex(key, seconds, value);
    }

    @Override
    public Response<String> psetex(byte[] key, long milliseconds, byte[] value) {
        check();
        return pipeline.psetex(key, milliseconds, value);
    }

    @Override
    public Response<Long> setnx(byte[] key, byte[] value) {
        check();
        return pipeline.setnx(key, value);
    }

    @Override
    public Response<Set<byte[]>> smembers(byte[] key) {
        check();
        return pipeline.smembers(key);
    }

    @Override
    public Response<Boolean> sismember(byte[] key, byte[] member) {
        check();
        return pipeline.sismember(key, member);
    }

    @Override
    public Response<List<byte[]>> sort(byte[] key) {
        check();
        return pipeline.sort(key);
    }

    @Override
    public Response<List<byte[]>> sort(byte[] key, SortingParams sortingParameters) {
        check();
        return pipeline.sort(key, sortingParameters);
    }

    @Override
    public Response<byte[]> spop(byte[] key) {
        check();
        return pipeline.spop(key);
    }

    @Override
    public Response<Set<byte[]>> spop(byte[] key, long count) {
        check();
        return pipeline.spop(key, count);
    }

    @Override
    public Response<byte[]> srandmember(byte[] key) {
        check();
        return pipeline.srandmember(key);
    }

    @Override
    public Response<List<byte[]>> srandmember(byte[] key, int count) {
        check();
        return pipeline.srandmember(key, count);
    }

    @Override
    public Response<Long> srem(byte[] key, byte[]... member) {
        check();
        return pipeline.srem(key, member);
    }

    @Override
    public Response<Long> strlen(byte[] key) {
        check();
        return pipeline.strlen(key);
    }

    @Override
    public Response<String> substr(byte[] key, int start, int end) {
        check();
        return pipeline.substr(key, start, end);
    }

    @Override
    public Response<Long> ttl(byte[] key) {
        check();
        return pipeline.ttl(key);
    }

    @Override
    public Response<Long> pttl(byte[] key) {
        check();
        return pipeline.pttl(key);
    }

    @Override
    public Response<String> type(byte[] key) {
        check();
        return pipeline.type(key);
    }

    @Override
    public Response<Long> zadd(byte[] key, double score, byte[] member) {
        check();
        return pipeline.zadd(key, score, member);
    }

    @Override
    public Response<Long> zadd(byte[] key, double score, byte[] member, ZAddParams params) {
        check();
        return pipeline.zadd(key, score, member, params);
    }

    @Override
    public Response<Long> zadd(byte[] key, Map<byte[], Double> scoreMembers) {
        check();
        return pipeline.zadd(key, scoreMembers);
    }

    @Override
    public Response<Long> zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
        check();
        return pipeline.zadd(key, scoreMembers, params);
    }

    @Override
    public Response<Long> zcard(byte[] key) {
        check();
        return pipeline.zcard(key);
    }

    @Override
    public Response<Long> zcount(byte[] key, double min, double max) {
        check();
        return pipeline.zcount(key, min, max);
    }

    @Override
    public Response<Double> zincrby(byte[] key, double score, byte[] member) {
        check();
        return pipeline.zincrby(key, score, member);
    }

    @Override
    public Response<Double> zincrby(byte[] key, double score, byte[] member, ZIncrByParams params) {
        check();
        return pipeline.zincrby(key, score, member, params);
    }

    @Override
    public Response<Set<byte[]>> zrange(byte[] key, long start, long end) {
        check();
        return pipeline.zrange(key, start, end);
    }

    @Override
    public Response<Set<byte[]>> zrangeByScore(byte[] key, double min, double max) {
        check();
        return pipeline.zrangeByScore(key, min, max);
    }

    @Override
    public Response<Set<byte[]>> zrangeByScore(byte[] key, byte[] min, byte[] max) {
        check();
        return pipeline.zrangeByScore(key, min, max);
    }

    @Override
    public Response<Set<byte[]>> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
        check();
        return pipeline.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Response<Set<byte[]>> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
        check();
        return pipeline.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, double min, double max) {
        check();
        return pipeline.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
        check();
        return pipeline.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
        check();
        return pipeline.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
        check();
        return pipeline.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Response<Set<byte[]>> zrevrangeByScore(byte[] key, double max, double min) {
        check();
        return pipeline.zrevrangeByScore(key, max, min);
    }

    @Override
    public Response<Set<byte[]>> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
        check();
        return pipeline.zrevrangeByScore(key, max, min);
    }

    @Override
    public Response<Set<byte[]>> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
        check();
        return pipeline.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Response<Set<byte[]>> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
        check();
        return pipeline.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
        check();
        return pipeline.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
        check();
        return pipeline.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
        check();
        return pipeline.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
        check();
        return pipeline.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrangeWithScores(byte[] key, long start, long end) {
        check();
        return pipeline.zrangeWithScores(key, start, end);
    }

    @Override
    public Response<Long> zrank(byte[] key, byte[] member) {
        check();
        return pipeline.zrank(key, member);
    }

    @Override
    public Response<Long> zrem(byte[] key, byte[]... member) {
        check();
        return pipeline.zrem(key, member);
    }

    @Override
    public Response<Long> zremrangeByRank(byte[] key, long start, long end) {
        check();
        return pipeline.zremrangeByRank(key, start, end);
    }

    @Override
    public Response<Long> zremrangeByScore(byte[] key, double start, double end) {
        check();
        return pipeline.zremrangeByScore(key, start, end);
    }

    @Override
    public Response<Long> zremrangeByScore(byte[] key, byte[] start, byte[] end) {
        check();
        return pipeline.zremrangeByScore(key, start, end);
    }

    @Override
    public Response<Set<byte[]>> zrevrange(byte[] key, long start, long end) {
        check();
        return pipeline.zrevrange(key, start, end);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeWithScores(byte[] key, long start, long end) {
        check();
        return pipeline.zrevrangeWithScores(key, start, end);
    }

    @Override
    public Response<Long> zrevrank(byte[] key, byte[] member) {
        check();
        return pipeline.zrevrank(key, member);
    }

    @Override
    public Response<Double> zscore(byte[] key, byte[] member) {
        check();
        return pipeline.zscore(key, member);
    }

    @Override
    public Response<List<Double>> zmscore(String key, String... members) {
        check();
        return pipeline.zmscore(key, members);
    }

    @Override
    public Response<List<Double>> zmscore(byte[] key, byte[]... members) {
        check();
        return pipeline.zmscore(key, members);
    }

    @Override
    public Response<Long> zlexcount(byte[] key, byte[] min, byte[] max) {
        check();
        return pipeline.zlexcount(key, min, max);
    }

    @Override
    public Response<Set<byte[]>> zrangeByLex(byte[] key, byte[] min, byte[] max) {
        check();
        return pipeline.zrangeByLex(key, min, max);
    }

    @Override
    public Response<Set<byte[]>> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
        check();
        return pipeline.zrangeByLex(key, min, max, offset, count);
    }

    @Override
    public Response<Set<byte[]>> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
        check();
        return pipeline.zrevrangeByLex(key, max, min);
    }

    @Override
    public Response<Set<byte[]>> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
        check();
        return pipeline.zrevrangeByLex(key, max, min, offset, count);
    }

    @Override
    public Response<Long> zremrangeByLex(byte[] key, byte[] min, byte[] max) {
        check();
        return pipeline.zremrangeByLex(key, min, max);
    }

    @Override
    public Response<Long> bitcount(byte[] key) {
        check();
        return pipeline.bitcount(key);
    }

    @Override
    public Response<Long> bitcount(byte[] key, long start, long end) {
        check();
        return pipeline.bitcount(key, start, end);
    }

    @Override
    public Response<Long> pfadd(byte[] key, byte[]... elements) {
        check();
        return pipeline.pfadd(key, elements);
    }

    @Override
    public Response<Long> pfcount(byte[] key) {
        check();
        return pipeline.pfcount(key);
    }

    @Override
    public Response<Long> geoadd(byte[] key, double longitude, double latitude, byte[] member) {
        check();
        return pipeline.geoadd(key, longitude, latitude, member);
    }

    @Override
    public Response<Long> geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
        check();
        return pipeline.geoadd(key, memberCoordinateMap);
    }

    @Override
    public Response<Double> geodist(byte[] key, byte[] member1, byte[] member2) {
        check();
        return pipeline.geodist(key, member1, member2);
    }

    @Override
    public Response<Double> geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
        check();
        return pipeline.geodist(key, member1, member2, unit);
    }

    @Override
    public Response<List<byte[]>> geohash(byte[] key, byte[]... members) {
        check();
        return pipeline.geohash(key, members);
    }

    @Override
    public Response<List<GeoCoordinate>> geopos(byte[] key, byte[]... members) {
        check();
        return pipeline.geopos(key, members);
    }

    @Override
    public Response<List<GeoRadiusResponse>> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
        check();
        return pipeline.georadius(key, longitude, latitude, radius, unit);
    }

    @Override
    public Response<List<GeoRadiusResponse>> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        check();
        return pipeline.georadius(key, longitude, latitude, radius, unit, param);
    }

    @Override
    public Response<List<GeoRadiusResponse>> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit) {
        check();
        return pipeline.georadiusByMember(key, member, radius, unit);
    }

    @Override
    public Response<List<GeoRadiusResponse>> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
        check();
        return pipeline.georadiusByMember(key, member, radius, unit, param);
    }

    @Override
    public Response<List<Long>> bitfield(byte[] key, byte[]... elements) {
        check();
        return pipeline.bitfield(key, elements);
    }

    @Override
    public Response<Long> bitpos(byte[] key, boolean value, BitPosParams params) {
        check();
        return pipeline.bitpos(key, value, params);
    }

    @Override
    public Response<Long> append(String key, String value) {
        check();
        return pipeline.append(key, value);
    }

    @Override
    public Response<Long> decr(String key) {
        check();
        return pipeline.decr(key);
    }

    @Override
    public Response<Long> decrBy(String key, long integer) {
        check();
        return pipeline.decrBy(key, integer);
    }

    @Override
    public Response<Long> del(String key) {
        check();
        return pipeline.del(key);
    }

    @Override
    public Response<String> echo(String string) {
        check();
        return pipeline.echo(string);
    }

    @Override
    public Response<Boolean> exists(String key) {
        check();
        return pipeline.exists(key);
    }

    @Override
    public Response<Long> expire(String key, int seconds) {
        check();
        return pipeline.expire(key, seconds);
    }

    @Override
    public Response<Long> pexpire(String key, long milliseconds) {
        check();
        return pipeline.pexpire(key, milliseconds);
    }

    @Override
    public Response<Long> expireAt(String key, long unixTime) {
        check();
        return pipeline.expireAt(key, unixTime);
    }

    @Override
    public Response<Long> pexpireAt(String key, long millisecondsTimestamp) {
        check();
        return pipeline.pexpireAt(key, millisecondsTimestamp);
    }

    @Override
    public Response<Boolean> getbit(String key, long offset) {
        check();
        return pipeline.getbit(key, offset);
    }

    @Override
    public Response<String> getrange(String key, long startOffset, long endOffset) {
        check();
        return pipeline.getrange(key, startOffset, endOffset);
    }

    @Override
    public Response<String> getSet(String key, String value) {
        check();
        return pipeline.getSet(key, value);
    }

    @Override
    public Response<Long> hdel(String key, String... field) {
        check();
        return pipeline.hdel(key, field);
    }

    @Override
    public Response<Boolean> hexists(String key, String field) {
        check();
        return pipeline.hexists(key, field);
    }

    @Override
    public Response<String> hget(String key, String field) {
        check();
        return pipeline.hget(key, field);
    }

    @Override
    public Response<Map<String, String>> hgetAll(String key) {
        check();
        return pipeline.hgetAll(key);
    }

    @Override
    public Response<Long> hincrBy(String key, String field, long value) {
        check();
        return pipeline.hincrBy(key, field, value);
    }

    @Override
    public Response<Set<String>> hkeys(String key) {
        check();
        return pipeline.hkeys(key);
    }

    @Override
    public Response<Long> hlen(String key) {
        check();
        return pipeline.hlen(key);
    }

    @Override
    public Response<List<String>> hmget(String key, String... fields) {
        check();
        return pipeline.hmget(key, fields);
    }

    @Override
    public Response<String> hmset(String key, Map<String, String> hash) {
        check();
        return pipeline.hmset(key, hash);
    }

    @Override
    public Response<Long> hset(String key, String field, String value) {
        check();
        return pipeline.hset(key, field, value);
    }

    @Override
    public Response<Long> hsetnx(String key, String field, String value) {
        check();
        return pipeline.hsetnx(key, field, value);
    }

    @Override
    public Response<List<String>> hvals(String key) {
        check();
        return pipeline.hvals(key);
    }

    @Override
    public Response<Long> incr(String key) {
        check();
        return pipeline.incr(key);
    }

    @Override
    public Response<Long> incrBy(String key, long integer) {
        check();
        return pipeline.incrBy(key, integer);
    }

    @Override
    public Response<String> lindex(String key, long index) {
        check();
        return pipeline.lindex(key, index);
    }

    @Override
    public Response<Long> llen(String key) {
        check();
        return pipeline.llen(key);
    }

    @Override
    public Response<String> lpop(String key) {
        check();
        return pipeline.lpop(key);
    }

    @Override
    public Response<Long> lpush(String key, String... string) {
        check();
        return pipeline.lpush(key, string);
    }

    @Override
    public Response<Long> lpushx(String key, String... string) {
        check();
        return pipeline.lpushx(key, string);
    }

    @Override
    public Response<List<String>> lrange(String key, long start, long end) {
        check();
        return pipeline.lrange(key, start, end);
    }

    @Override
    public Response<Long> lrem(String key, long count, String value) {
        check();
        return pipeline.lrem(key, count, value);
    }

    @Override
    public Response<String> lset(String key, long index, String value) {
        check();
        return pipeline.lset(key, index, value);
    }

    @Override
    public Response<String> ltrim(String key, long start, long end) {
        check();
        return pipeline.ltrim(key, start, end);
    }

    @Override
    public Response<Long> persist(String key) {
        check();
        return pipeline.persist(key);
    }

    @Override
    public Response<String> rpop(String key) {
        check();
        return pipeline.rpop(key);
    }

    @Override
    public Response<Long> rpush(String key, String... string) {
        check();
        return pipeline.rpush(key, string);
    }

    @Override
    public Response<Long> rpushx(String key, String... string) {
        check();
        return pipeline.rpushx(key, string);
    }

    @Override
    public Response<Long> sadd(String key, String... member) {
        check();
        return pipeline.sadd(key, member);
    }

    @Override
    public Response<Long> scard(String key) {
        check();
        return pipeline.scard(key);
    }

    @Override
    public Response<Boolean> sismember(String key, String member) {
        check();
        return pipeline.sismember(key, member);
    }

    @Override
    public Response<Boolean> setbit(String key, long offset, boolean value) {
        check();
        return pipeline.setbit(key, offset, value);
    }

    @Override
    public Response<String> setex(String key, int seconds, String value) {
        check();
        return pipeline.setex(key, seconds, value);
    }

    @Override
    public Response<Long> setnx(String key, String value) {
        check();
        return pipeline.setnx(key, value);
    }

    @Override
    public Response<Long> setrange(String key, long offset, String value) {
        check();
        return pipeline.setrange(key, offset, value);
    }

    @Override
    public Response<Set<String>> smembers(String key) {
        check();
        return pipeline.smembers(key);
    }

    @Override
    public Response<List<String>> sort(String key) {
        check();
        return pipeline.sort(key);
    }

    @Override
    public Response<List<String>> sort(String key, SortingParams sortingParameters) {
        check();
        return pipeline.sort(key, sortingParameters);
    }

    @Override
    public Response<String> spop(String key) {
        check();
        return pipeline.spop(key);
    }

    @Override
    public Response<Set<String>> spop(String key, long count) {
        check();
        return pipeline.spop(key, count);
    }

    @Override
    public Response<String> srandmember(String key) {
        check();
        return pipeline.srandmember(key);
    }

    @Override
    public Response<Long> srem(String key, String... member) {
        check();
        return pipeline.srem(key, member);
    }

    @Override
    public Response<Long> strlen(String key) {
        check();
        return pipeline.strlen(key);
    }

    @Override
    public Response<String> substr(String key, int start, int end) {
        check();
        return pipeline.substr(key, start, end);
    }

    @Override
    public Response<Long> ttl(String key) {
        check();
        return pipeline.ttl(key);
    }

    @Override
    public Response<String> type(String key) {
        check();
        return pipeline.type(key);
    }

    @Override
    public Response<Long> zadd(String key, double score, String member) {
        check();
        return pipeline.zadd(key, score, member);
    }

    @Override
    public Response<Long> zadd(String key, double score, String member, ZAddParams params) {
        check();
        return pipeline.zadd(key, score, member, params);
    }

    @Override
    public Response<Long> zadd(String key, Map<String, Double> scoreMembers) {
        check();
        return pipeline.zadd(key, scoreMembers);
    }

    @Override
    public Response<Long> zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
        check();
        return pipeline.zadd(key, scoreMembers, params);
    }

    @Override
    public Response<Long> zcard(String key) {
        check();
        return pipeline.zcard(key);
    }

    @Override
    public Response<Long> zcount(String key, double min, double max) {
        check();
        return pipeline.zcount(key, min, max);
    }

    @Override
    public Response<Double> zincrby(String key, double score, String member) {
        check();
        return pipeline.zincrby(key, score, member);
    }

    @Override
    public Response<Double> zincrby(String key, double score, String member, ZIncrByParams params) {
        check();
        return pipeline.zincrby(key, score, member, params);
    }

    @Override
    public Response<Set<String>> zrange(String key, long start, long end) {
        check();
        return pipeline.zrange(key, start, end);
    }

    @Override
    public Response<Set<String>> zrangeByScore(String key, double min, double max) {
        check();
        return pipeline.zrangeByScore(key, min, max);
    }

    @Override
    public Response<Set<String>> zrangeByScore(String key, String min, String max) {
        check();
        return pipeline.zrangeByScore(key, min, max);
    }

    @Override
    public Response<Set<String>> zrangeByScore(String key, double min, double max, int offset, int count) {
        check();
        return pipeline.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max) {
        check();
        return pipeline.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        check();
        return pipeline.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Response<Set<String>> zrevrangeByScore(String key, double max, double min) {
        check();
        return pipeline.zrevrangeByScore(key, max, min);
    }

    @Override
    public Response<Set<String>> zrevrangeByScore(String key, String max, String min) {
        check();
        return pipeline.zrevrangeByScore(key, max, min);
    }

    @Override
    public Response<Set<String>> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        check();
        return pipeline.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min) {
        check();
        return pipeline.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        check();
        return pipeline.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrangeWithScores(String key, long start, long end) {
        check();
        return pipeline.zrangeWithScores(key, start, end);
    }

    @Override
    public Response<Long> zrank(String key, String member) {
        check();
        return pipeline.zrank(key, member);
    }

    @Override
    public Response<Long> zrem(String key, String... member) {
        check();
        return pipeline.zrem(key, member);
    }

    @Override
    public Response<Long> zremrangeByRank(String key, long start, long end) {
        check();
        return pipeline.zremrangeByRank(key, start, end);
    }

    @Override
    public Response<Long> zremrangeByScore(String key, double start, double end) {
        check();
        return pipeline.zremrangeByScore(key, start, end);
    }

    @Override
    public Response<Set<String>> zrevrange(String key, long start, long end) {
        check();
        return pipeline.zrevrange(key, start, end);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeWithScores(String key, long start, long end) {
        check();
        return pipeline.zrevrangeWithScores(key, start, end);
    }

    @Override
    public Response<Long> zrevrank(String key, String member) {
        check();
        return pipeline.zrevrank(key, member);
    }

    @Override
    public Response<Double> zscore(String key, String member) {
        check();
        return pipeline.zscore(key, member);
    }

    @Override
    public Response<Long> zlexcount(String key, String min, String max) {
        check();
        return pipeline.zlexcount(key, min, max);
    }

    @Override
    public Response<Set<String>> zrangeByLex(String key, String min, String max) {
        check();
        return pipeline.zrangeByLex(key, min, max);
    }

    @Override
    public Response<Set<String>> zrangeByLex(String key, String min, String max, int offset, int count) {
        check();
        return pipeline.zrangeByLex(key, min, max, offset, count);
    }

    @Override
    public Response<Set<String>> zrevrangeByLex(String key, String max, String min) {
        check();
        return pipeline.zrevrangeByLex(key, max, min);
    }

    @Override
    public Response<Set<String>> zrevrangeByLex(String key, String max, String min, int offset, int count) {
        check();
        return pipeline.zrevrangeByLex(key, max, min, offset, count);
    }

    @Override
    public Response<Long> zremrangeByLex(String key, String start, String end) {
        check();
        return pipeline.zremrangeByLex(key, start, end);
    }

    @Override
    public Response<Long> bitcount(String key) {
        check();
        return pipeline.bitcount(key);
    }

    @Override
    public Response<Long> bitcount(String key, long start, long end) {
        check();
        return pipeline.bitcount(key, start, end);
    }

    @Override
    public Response<Long> pfadd(String key, String... elements) {
        check();
        return pipeline.pfadd(key, elements);
    }

    @Override
    public Response<Long> pfcount(String key) {
        check();
        return pipeline.pfcount(key);
    }

    @Override
    public Response<List<Long>> bitfield(String key, String... arguments) {
        check();
        return pipeline.bitfield(key, arguments);
    }

    @Override
    public Response<Long> geoadd(String key, double longitude, double latitude, String member) {
        check();
        return pipeline.geoadd(key, longitude, latitude, member);
    }

    @Override
    public Response<Long> geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
        check();
        return pipeline.geoadd(key, memberCoordinateMap);
    }

    @Override
    public Response<Double> geodist(String key, String member1, String member2) {
        check();
        return pipeline.geodist(key, member1, member2);
    }

    @Override
    public Response<Double> geodist(String key, String member1, String member2, GeoUnit unit) {
        check();
        return pipeline.geodist(key, member1, member2, unit);
    }

    @Override
    public Response<List<String>> geohash(String key, String... members) {
        check();
        return pipeline.geohash(key, members);
    }

    @Override
    public Response<List<GeoCoordinate>> geopos(String key, String... members) {
        check();
        return pipeline.geopos(key, members);
    }

    @Override
    public Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
        check();
        return pipeline.georadius(key, longitude, latitude, radius, unit);
    }

    @Override
    public Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        check();
        return pipeline.georadius(key, longitude, latitude, radius, unit, param);
    }

    @Override
    public Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
        check();
        return pipeline.georadiusByMember(key, member, radius, unit);
    }

    @Override
    public Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
        check();
        return pipeline.georadiusByMember(key, member, radius, unit, param);
    }

    @Override
    public Response<byte[]> dump(String key) {
        check();
        return pipeline.dump(key);
    }

    @Override
    public Response<byte[]> dump(byte[] key) {
        check();
        return pipeline.dump(key);
    }

    @Override
    public Response<String> restore(byte[] key, int ttl, byte[] serializedValue) {
        check();
        return pipeline.restore(key, ttl, serializedValue);
    }

    @Override
    public Response<String> restore(String key, int ttl, byte[] serializedValue) {
        check();
        return pipeline.restore(key, ttl, serializedValue);
    }


    @Override
    public Response<Double> hincrByFloat(String key, String field, double increment) {
        check();
        return pipeline.hincrByFloat(key, field, increment);
    }

    @Override
    public Response<Double> incrByFloat(String key, double increment) {
        check();
        return pipeline.incrByFloat(key, increment);
    }

    @Override
    public Response<String> set(String key, String value, String nxxx) {
        check();
        return pipeline.set(key, value, nxxx);
    }

    @Override
    public Response<String> set(byte[] key, byte[] value, byte[] nxxx) {
        check();
        return pipeline.set(key, value, nxxx);
    }

    @Override
    public Response<String> set(String key, String value, String nxxx, String expx, int time) {
        check();
        return pipeline.set(key, value, nxxx, expx, time);
    }

    @Override
    public Response<String> psetex(String key, long milliseconds, String value) {
        check();
        return pipeline.psetex(key, milliseconds, value);
    }

    @Override
    public Response<String> psetex(String key, int milliseconds, String value) {
        check();
        return pipeline.psetex(key, milliseconds, value);
    }

    @Override
    public Response<List<String>> srandmember(String key, int count) {
        check();
        return pipeline.srandmember(key, count);
    }

    @Override
    public Response<Long> pttl(String key) {
        check();
        return pipeline.pttl(key);
    }

    @Override
    public Response<Set<String>> zrangeByScore(String key, String min, String max, int offset, int count) {
        check();
        return pipeline.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
        check();
        return pipeline.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max) {
        check();
        return pipeline.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Response<Set<String>> zrevrangeByScore(String key, String max, String min, int offset, int count) {
        check();
        return pipeline.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
        check();
        return pipeline.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min) {
        check();
        return pipeline.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Response<Long> zremrangeByScore(String key, String min, String max) {
        check();
        return pipeline.zremrangeByScore(key, min, max);
    }

    @Override
    public Response<Long> bitpos(String key, boolean value) {
        check();
        return pipeline.bitpos(key, value);
    }

    @Override
    public Response<Long> bitpos(String key, boolean value, BitPosParams params) {
        check();
        return pipeline.bitpos(key, value, params);
    }

    @Override
    public Response<Long> bitpos(byte[] key, boolean value) {
        check();
        return pipeline.bitpos(key, value);
    }

    @Override
    public Response<Long> zcount(byte[] key, byte[] min, byte[] max) {
        check();
        return pipeline.zcount(key, min, max);
    }

    @Override
    public Response<Long> zcount(String key, String min, String max) {
        check();
        return pipeline.zcount(key, min, max);
    }
}
