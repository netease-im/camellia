package com.netease.nim.camellia.redis.adaptor;

import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
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
 * Created by caojiajun on 2021/7/26
 */
public class PipelineCamelliaAdaptor extends Pipeline {

    private final ICamelliaRedisPipeline pipeline;

    PipelineCamelliaAdaptor(ICamelliaRedisPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public void sync() {
        pipeline.sync();
    }

    @Override
    public void close() {
        pipeline.sync();
    }

    @Override
    public void clear() {
        pipeline.sync();
    }

    @Override
    public Response<Long> append(String key, String value) {
        return pipeline.append(key, value);
    }

    @Override
    public Response<Long> append(byte[] key, byte[] value) {
        return pipeline.append(key, value);
    }


    @Override
    public Response<Long> decr(String key) {
        return pipeline.decr(key);
    }

    @Override
    public Response<Long> decr(byte[] key) {
        return pipeline.decr(key);
    }

    @Override
    public Response<Long> decrBy(String key, long decrement) {
        return pipeline.decrBy(key, decrement);
    }

    @Override
    public Response<Long> decrBy(byte[] key, long decrement) {
        return pipeline.decrBy(key, decrement);
    }

    @Override
    public Response<Long> del(String key) {
        return pipeline.del(key);
    }

    @Override
    public Response<Long> del(byte[] key) {
        return pipeline.del(key);
    }

    @Override
    public Response<String> echo(String string) {
        return pipeline.echo(string);
    }

    @Override
    public Response<byte[]> echo(byte[] string) {
        return pipeline.echo(string);
    }

    @Override
    public Response<Boolean> exists(String key) {
        return pipeline.exists(key);
    }

    @Override
    public Response<Boolean> exists(byte[] key) {
        return pipeline.exists(key);
    }

    @Override
    public Response<Long> expire(String key, int seconds) {
        return pipeline.expire(key, seconds);
    }

    @Override
    public Response<Long> expire(byte[] key, int seconds) {
        return pipeline.expire(key, seconds);
    }

    @Override
    public Response<Long> expireAt(String key, long unixTime) {
        return pipeline.expireAt(key, unixTime);
    }

    @Override
    public Response<Long> expireAt(byte[] key, long unixTime) {
        return pipeline.expireAt(key, unixTime);
    }

    @Override
    public Response<String> get(String key) {
        return pipeline.get(key);
    }

    @Override
    public Response<byte[]> get(byte[] key) {
        return pipeline.get(key);
    }

    @Override
    public Response<Boolean> getbit(String key, long offset) {
        return pipeline.getbit(key, offset);
    }

    @Override
    public Response<Boolean> getbit(byte[] key, long offset) {
        return pipeline.getbit(key, offset);
    }

    @Override
    public Response<Long> bitpos(String key, boolean value) {
        return pipeline.bitpos(key, value);
    }

    @Override
    public Response<Long> bitpos(String key, boolean value, BitPosParams params) {
        return pipeline.bitpos(key, value, params);
    }

    @Override
    public Response<Long> bitpos(byte[] key, boolean value) {
        return pipeline.bitpos(key, value);
    }

    @Override
    public Response<Long> bitpos(byte[] key, boolean value, BitPosParams params) {
        return pipeline.bitpos(key, value, params);
    }

    @Override
    public Response<String> getrange(String key, long startOffset, long endOffset) {
        return pipeline.getrange(key, startOffset, endOffset);
    }

    @Override
    public Response<String> getSet(String key, String value) {
        return pipeline.getSet(key, value);
    }

    @Override
    public Response<byte[]> getSet(byte[] key, byte[] value) {
        return pipeline.getSet(key, value);
    }

    @Override
    public Response<Long> hdel(String key, String... field) {
        return pipeline.hdel(key, field);
    }

    @Override
    public Response<Long> hdel(byte[] key, byte[]... field) {
        return pipeline.hdel(key, field);
    }

    @Override
    public Response<Boolean> hexists(String key, String field) {
        return pipeline.hexists(key, field);
    }

    @Override
    public Response<Boolean> hexists(byte[] key, byte[] field) {
        return pipeline.hexists(key, field);
    }

    @Override
    public Response<String> hget(String key, String field) {
        return pipeline.hget(key, field);
    }

    @Override
    public Response<byte[]> hget(byte[] key, byte[] field) {
        return pipeline.hget(key, field);
    }

    @Override
    public Response<Map<String, String>> hgetAll(String key) {
        return pipeline.hgetAll(key);
    }

    @Override
    public Response<Map<byte[], byte[]>> hgetAll(byte[] key) {
        return pipeline.hgetAll(key);
    }

    @Override
    public Response<Long> hincrBy(String key, String field, long value) {
        return pipeline.hincrBy(key, field, value);
    }

    @Override
    public Response<Long> hincrBy(byte[] key, byte[] field, long value) {
        return pipeline.hincrBy(key, field, value);
    }

    @Override
    public Response<Set<String>> hkeys(String key) {
        return pipeline.hkeys(key);
    }

    @Override
    public Response<Set<byte[]>> hkeys(byte[] key) {
        return pipeline.hkeys(key);
    }

    @Override
    public Response<Long> hlen(String key) {
        return pipeline.hlen(key);
    }

    @Override
    public Response<Long> hlen(byte[] key) {
        return pipeline.hlen(key);
    }

    @Override
    public Response<List<String>> hmget(String key, String... fields) {
        return pipeline.hmget(key, fields);
    }

    @Override
    public Response<List<byte[]>> hmget(byte[] key, byte[]... fields) {
        return pipeline.hmget(key, fields);
    }

    @Override
    public Response<String> hmset(String key, Map<String, String> hash) {
        return pipeline.hmset(key, hash);
    }

    @Override
    public Response<String> hmset(byte[] key, Map<byte[], byte[]> hash) {
        return pipeline.hmset(key, hash);
    }

    @Override
    public Response<Long> hset(String key, String field, String value) {
        return pipeline.hset(key, field, value);
    }

    @Override
    public Response<Long> hset(byte[] key, byte[] field, byte[] value) {
        return pipeline.hset(key, field, value);
    }

    @Override
    public Response<Long> hsetnx(String key, String field, String value) {
        return pipeline.hsetnx(key, field, value);
    }

    @Override
    public Response<Long> hsetnx(byte[] key, byte[] field, byte[] value) {
        return pipeline.hsetnx(key, field, value);
    }

    @Override
    public Response<List<String>> hvals(String key) {
        return pipeline.hvals(key);
    }

    @Override
    public Response<List<byte[]>> hvals(byte[] key) {
        return pipeline.hvals(key);
    }

    @Override
    public Response<Long> incr(String key) {
        return pipeline.incr(key);
    }

    @Override
    public Response<Long> incr(byte[] key) {
        return pipeline.incr(key);
    }

    @Override
    public Response<Long> incrBy(String key, long increment) {
        return pipeline.incrBy(key, increment);
    }

    @Override
    public Response<Long> incrBy(byte[] key, long increment) {
        return pipeline.incrBy(key, increment);
    }

    @Override
    public Response<String> lindex(String key, long index) {
        return pipeline.lindex(key, index);
    }

    @Override
    public Response<byte[]> lindex(byte[] key, long index) {
        return pipeline.lindex(key, index);
    }

    @Override
    public Response<Long> linsert(String key, ListPosition where, String pivot, String value) {
        return pipeline.linsert(key, where, pivot, value);
    }

    @Override
    public Response<Long> linsert(byte[] key, ListPosition where, byte[] pivot, byte[] value) {
        return pipeline.linsert(key, where, pivot, value);
    }

    @Override
    public Response<Long> llen(String key) {
        return pipeline.llen(key);
    }

    @Override
    public Response<Long> llen(byte[] key) {
        return pipeline.llen(key);
    }

    @Override
    public Response<String> lpop(String key) {
        return pipeline.lpop(key);
    }

    @Override
    public Response<byte[]> lpop(byte[] key) {
        return pipeline.lpop(key);
    }

    @Override
    public Response<Long> lpush(String key, String... string) {
        return pipeline.lpush(key, string);
    }

    @Override
    public Response<Long> lpush(byte[] key, byte[]... string) {
        return pipeline.lpush(key, string);
    }

    @Override
    public Response<Long> lpushx(String key, String... string) {
        return pipeline.lpushx(key, string);
    }

    @Override
    public Response<Long> lpushx(byte[] key, byte[]... bytes) {
        return pipeline.lpushx(key, bytes);
    }

    @Override
    public Response<List<String>> lrange(String key, long start, long stop) {
        return pipeline.lrange(key, start, stop);
    }

    @Override
    public Response<List<byte[]>> lrange(byte[] key, long start, long stop) {
        return pipeline.lrange(key, start, stop);
    }

    @Override
    public Response<Long> lrem(String key, long count, String value) {
        return pipeline.lrem(key, count, value);
    }

    @Override
    public Response<Long> lrem(byte[] key, long count, byte[] value) {
        return pipeline.lrem(key, count, value);
    }

    @Override
    public Response<String> lset(String key, long index, String value) {
        return pipeline.lset(key, index, value);
    }

    @Override
    public Response<String> lset(byte[] key, long index, byte[] value) {
        return pipeline.lset(key, index, value);
    }

    @Override
    public Response<String> ltrim(String key, long start, long stop) {
        return pipeline.ltrim(key, start, stop);
    }

    @Override
    public Response<String> ltrim(byte[] key, long start, long stop) {
        return pipeline.ltrim(key, start, stop);
    }

    @Override
    public Response<Long> persist(String key) {
        return pipeline.persist(key);
    }

    @Override
    public Response<Long> persist(byte[] key) {
        return pipeline.persist(key);
    }

    @Override
    public Response<String> rpop(String key) {
        return pipeline.rpop(key);
    }

    @Override
    public Response<byte[]> rpop(byte[] key) {
        return pipeline.rpop(key);
    }

    @Override
    public Response<Long> rpush(String key, String... string) {
        return pipeline.rpush(key, string);
    }

    @Override
    public Response<Long> rpush(byte[] key, byte[]... string) {
        return pipeline.rpush(key, string);
    }

    @Override
    public Response<Long> rpushx(String key, String... string) {
        return pipeline.rpushx(key, string);
    }

    @Override
    public Response<Long> rpushx(byte[] key, byte[]... string) {
        return pipeline.rpushx(key, string);
    }

    @Override
    public Response<Long> sadd(String key, String... member) {
        return pipeline.sadd(key, member);
    }

    @Override
    public Response<Long> sadd(byte[] key, byte[]... member) {
        return pipeline.sadd(key, member);
    }

    @Override
    public Response<Long> scard(String key) {
        return pipeline.scard(key);
    }

    @Override
    public Response<Long> scard(byte[] key) {
        return pipeline.scard(key);
    }

    @Override
    public Response<String> set(String key, String value) {
        return pipeline.set(key, value);
    }

    @Override
    public Response<String> set(byte[] key, byte[] value) {
        return pipeline.set(key, value);
    }

    @Override
    public Response<Boolean> setbit(String key, long offset, boolean value) {
        return pipeline.setbit(key, offset, value);
    }

    @Override
    public Response<Boolean> setbit(byte[] key, long offset, byte[] value) {
        return pipeline.setbit(key, offset, value);
    }

    @Override
    public Response<String> setex(String key, int seconds, String value) {
        return pipeline.setex(key, seconds, value);
    }

    @Override
    public Response<String> setex(byte[] key, int seconds, byte[] value) {
        return pipeline.setex(key, seconds, value);
    }

    @Override
    public Response<Long> setnx(String key, String value) {
        return pipeline.setnx(key, value);
    }

    @Override
    public Response<Long> setnx(byte[] key, byte[] value) {
        return pipeline.setnx(key, value);
    }

    @Override
    public Response<Long> setrange(String key, long offset, String value) {
        return pipeline.setrange(key, offset, value);
    }

    @Override
    public Response<Long> setrange(byte[] key, long offset, byte[] value) {
        return pipeline.setrange(key, offset, value);
    }

    @Override
    public Response<Boolean> sismember(String key, String member) {
        return pipeline.sismember(key, member);
    }

    @Override
    public Response<Boolean> sismember(byte[] key, byte[] member) {
        return pipeline.sismember(key, member);
    }

    @Override
    public Response<Set<String>> smembers(String key) {
        return pipeline.smembers(key);
    }

    @Override
    public Response<Set<byte[]>> smembers(byte[] key) {
        return pipeline.smembers(key);
    }

    @Override
    public Response<List<String>> sort(String key) {
        return pipeline.sort(key);
    }

    @Override
    public Response<List<byte[]>> sort(byte[] key) {
        return pipeline.sort(key);
    }

    @Override
    public Response<List<String>> sort(String key, SortingParams sortingParameters) {
        return pipeline.sort(key, sortingParameters);
    }

    @Override
    public Response<List<byte[]>> sort(byte[] key, SortingParams sortingParameters) {
        return pipeline.sort(key, sortingParameters);
    }

    @Override
    public Response<String> spop(String key) {
        return pipeline.spop(key);
    }

    @Override
    public Response<Set<String>> spop(String key, long count) {
        return pipeline.spop(key, count);
    }

    @Override
    public Response<byte[]> spop(byte[] key) {
        return pipeline.spop(key);
    }

    @Override
    public Response<Set<byte[]>> spop(byte[] key, long count) {
        return pipeline.spop(key, count);
    }

    @Override
    public Response<String> srandmember(String key) {
        return pipeline.srandmember(key);
    }

    @Override
    public Response<List<String>> srandmember(String key, int count) {
        return pipeline.srandmember(key, count);
    }

    @Override
    public Response<byte[]> srandmember(byte[] key) {
        return pipeline.srandmember(key);
    }

    @Override
    public Response<List<byte[]>> srandmember(byte[] key, int count) {
        return pipeline.srandmember(key, count);
    }

    @Override
    public Response<Long> srem(String key, String... member) {
        return pipeline.srem(key, member);
    }

    @Override
    public Response<Long> srem(byte[] key, byte[]... member) {
        return pipeline.srem(key, member);
    }

    @Override
    public Response<Long> strlen(String key) {
        return pipeline.strlen(key);
    }

    @Override
    public Response<Long> strlen(byte[] key) {
        return pipeline.strlen(key);
    }

    @Override
    public Response<String> substr(String key, int start, int end) {
        return pipeline.substr(key, start, end);
    }

    @Override
    public Response<String> substr(byte[] key, int start, int end) {
        return pipeline.substr(key, start, end);
    }

    @Override
    public Response<Long> ttl(String key) {
        return pipeline.ttl(key);
    }

    @Override
    public Response<Long> ttl(byte[] key) {
        return pipeline.ttl(key);
    }

    @Override
    public Response<String> type(String key) {
        return pipeline.type(key);
    }

    @Override
    public Response<String> type(byte[] key) {
        return pipeline.type(key);
    }

    @Override
    public Response<Long> zadd(String key, double score, String member) {
        return pipeline.zadd(key, score, member);
    }

    @Override
    public Response<Long> zadd(String key, double score, String member, ZAddParams params) {
        return pipeline.zadd(key, score, member, params);
    }

    @Override
    public Response<Long> zadd(String key, Map<String, Double> scoreMembers) {
        return pipeline.zadd(key, scoreMembers);
    }

    @Override
    public Response<Long> zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
        return pipeline.zadd(key, scoreMembers, params);
    }

    @Override
    public Response<Long> zadd(byte[] key, double score, byte[] member) {
        return pipeline.zadd(key, score, member);
    }

    @Override
    public Response<Long> zadd(byte[] key, double score, byte[] member, ZAddParams params) {
        return pipeline.zadd(key, score, member, params);
    }

    @Override
    public Response<Long> zadd(byte[] key, Map<byte[], Double> scoreMembers) {
        return pipeline.zadd(key, scoreMembers);
    }

    @Override
    public Response<Long> zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
        return pipeline.zadd(key, scoreMembers, params);
    }

    @Override
    public Response<Long> zcard(String key) {
        return pipeline.zcard(key);
    }

    @Override
    public Response<Long> zcard(byte[] key) {
        return pipeline.zcard(key);
    }

    @Override
    public Response<Long> zcount(String key, double min, double max) {
        return pipeline.zcount(key, min, max);
    }

    @Override
    public Response<Long> zcount(String key, String min, String max) {
        return pipeline.zcount(key, min, max);
    }

    @Override
    public Response<Long> zcount(byte[] key, double min, double max) {
        return pipeline.zcount(key, min, max);
    }

    @Override
    public Response<Long> zcount(byte[] key, byte[] min, byte[] max) {
        return pipeline.zcount(key, min, max);
    }

    @Override
    public Response<Double> zincrby(String key, double increment, String member) {
        return pipeline.zincrby(key, increment, member);
    }

    @Override
    public Response<Double> zincrby(String key, double increment, String member, ZIncrByParams params) {
        return pipeline.zincrby(key, increment, member, params);
    }

    @Override
    public Response<Double> zincrby(byte[] key, double increment, byte[] member) {
        return pipeline.zincrby(key, increment, member);
    }

    @Override
    public Response<Double> zincrby(byte[] key, double increment, byte[] member, ZIncrByParams params) {
        return pipeline.zincrby(key, increment, member, params);
    }

    @Override
    public Response<Set<String>> zrange(String key, long start, long stop) {
        return pipeline.zrange(key, start, stop);
    }

    @Override
    public Response<Set<byte[]>> zrange(byte[] key, long start, long stop) {
        return pipeline.zrange(key, start, stop);
    }

    @Override
    public Response<Set<String>> zrangeByScore(String key, double min, double max) {
        return pipeline.zrangeByScore(key, min, max);
    }

    @Override
    public Response<Set<byte[]>> zrangeByScore(byte[] key, double min, double max) {
        return pipeline.zrangeByScore(key, min, max);
    }

    @Override
    public Response<Set<String>> zrangeByScore(String key, String min, String max) {
        return pipeline.zrangeByScore(key, min, max);
    }

    @Override
    public Response<Set<byte[]>> zrangeByScore(byte[] key, byte[] min, byte[] max) {
        return pipeline.zrangeByScore(key, min, max);
    }

    @Override
    public Response<Set<String>> zrangeByScore(String key, double min, double max, int offset, int count) {
        return pipeline.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Response<Set<String>> zrangeByScore(String key, String min, String max, int offset, int count) {
        return pipeline.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Response<Set<byte[]>> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
        return pipeline.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Response<Set<byte[]>> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return pipeline.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max) {
        return pipeline.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max) {
        return pipeline.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, double min, double max) {
        return pipeline.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
        return pipeline.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        return pipeline.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
        return pipeline.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
        return pipeline.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return pipeline.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Response<Set<String>> zrevrangeByScore(String key, double max, double min) {
        return pipeline.zrevrangeByScore(key, max, min);
    }

    @Override
    public Response<Set<byte[]>> zrevrangeByScore(byte[] key, double max, double min) {
        return pipeline.zrevrangeByScore(key, max, min);
    }

    @Override
    public Response<Set<String>> zrevrangeByScore(String key, String max, String min) {
        return pipeline.zrevrangeByScore(key, max, min);
    }

    @Override
    public Response<Set<byte[]>> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
        return pipeline.zrevrangeByScore(key, max, min);
    }

    @Override
    public Response<Set<String>> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        return pipeline.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Response<Set<String>> zrevrangeByScore(String key, String max, String min, int offset, int count) {
        return pipeline.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Response<Set<byte[]>> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
        return pipeline.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Response<Set<byte[]>> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return pipeline.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min) {
        return pipeline.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min) {
        return pipeline.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
        return pipeline.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
        return pipeline.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        return pipeline.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
        return pipeline.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
        return pipeline.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return pipeline.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Response<Set<Tuple>> zrangeWithScores(String key, long start, long stop) {
        return pipeline.zrangeWithScores(key, start, stop);
    }

    @Override
    public Response<Set<Tuple>> zrangeWithScores(byte[] key, long start, long stop) {
        return pipeline.zrangeWithScores(key, start, stop);
    }

    @Override
    public Response<Long> zrank(String key, String member) {
        return pipeline.zrank(key, member);
    }

    @Override
    public Response<Long> zrank(byte[] key, byte[] member) {
        return pipeline.zrank(key, member);
    }

    @Override
    public Response<Long> zrem(String key, String... members) {
        return pipeline.zrem(key, members);
    }

    @Override
    public Response<Long> zrem(byte[] key, byte[]... members) {
        return pipeline.zrem(key, members);
    }

    @Override
    public Response<Long> zremrangeByRank(String key, long start, long stop) {
        return pipeline.zremrangeByRank(key, start, stop);
    }

    @Override
    public Response<Long> zremrangeByRank(byte[] key, long start, long stop) {
        return pipeline.zremrangeByRank(key, start, stop);
    }

    @Override
    public Response<Long> zremrangeByScore(String key, double min, double max) {
        return pipeline.zremrangeByScore(key, min, max);
    }

    @Override
    public Response<Long> zremrangeByScore(String key, String min, String max) {
        return pipeline.zremrangeByScore(key, min, max);
    }

    @Override
    public Response<Long> zremrangeByScore(byte[] key, double min, double max) {
        return pipeline.zremrangeByScore(key, min, max);
    }

    @Override
    public Response<Long> zremrangeByScore(byte[] key, byte[] min, byte[] max) {
        return pipeline.zremrangeByScore(key, min, max);
    }

    @Override
    public Response<Set<String>> zrevrange(String key, long start, long stop) {
        return pipeline.zrevrange(key, start, stop);
    }

    @Override
    public Response<Set<byte[]>> zrevrange(byte[] key, long start, long stop) {
        return pipeline.zrevrange(key, start, stop);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeWithScores(String key, long start, long stop) {
        return pipeline.zrevrangeWithScores(key, start, stop);
    }

    @Override
    public Response<Set<Tuple>> zrevrangeWithScores(byte[] key, long start, long stop) {
        return pipeline.zrevrangeWithScores(key, start, stop);
    }

    @Override
    public Response<Long> zrevrank(String key, String member) {
        return pipeline.zrevrank(key, member);
    }

    @Override
    public Response<Long> zrevrank(byte[] key, byte[] member) {
        return pipeline.zrevrank(key, member);
    }

    @Override
    public Response<Double> zscore(String key, String member) {
        return pipeline.zscore(key, member);
    }

    @Override
    public Response<Double> zscore(byte[] key, byte[] member) {
        return pipeline.zscore(key, member);
    }

    @Override
    public Response<Long> zlexcount(byte[] key, byte[] min, byte[] max) {
        return pipeline.zlexcount(key, min, max);
    }

    @Override
    public Response<Long> zlexcount(String key, String min, String max) {
        return pipeline.zlexcount(key, min, max);
    }

    @Override
    public Response<Set<byte[]>> zrangeByLex(byte[] key, byte[] min, byte[] max) {
        return pipeline.zrangeByLex(key, min, max);
    }

    @Override
    public Response<Set<String>> zrangeByLex(String key, String min, String max) {
        return pipeline.zrangeByLex(key, min, max);
    }

    @Override
    public Response<Set<byte[]>> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return pipeline.zrangeByLex(key, min, max, offset, count);
    }

    @Override
    public Response<Set<String>> zrangeByLex(String key, String min, String max, int offset, int count) {
        return pipeline.zrangeByLex(key, min, max, offset, count);
    }

    @Override
    public Response<Set<byte[]>> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
        return pipeline.zrevrangeByLex(key, max, min);
    }

    @Override
    public Response<Set<String>> zrevrangeByLex(String key, String max, String min) {
        return pipeline.zrevrangeByLex(key, max, min);
    }

    @Override
    public Response<Set<byte[]>> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return pipeline.zrevrangeByLex(key, max, min, offset, count);
    }

    @Override
    public Response<Set<String>> zrevrangeByLex(String key, String max, String min, int offset, int count) {
        return pipeline.zrevrangeByLex(key, max, min, offset, count);
    }

    @Override
    public Response<Long> zremrangeByLex(byte[] key, byte[] min, byte[] max) {
        return pipeline.zremrangeByLex(key, min, max);
    }

    @Override
    public Response<Long> zremrangeByLex(String key, String min, String max) {
        return pipeline.zremrangeByLex(key, min, max);
    }

    @Override
    public Response<Long> bitcount(String key) {
        return pipeline.bitcount(key);
    }

    @Override
    public Response<Long> bitcount(String key, long start, long end) {
        return pipeline.bitcount(key, start, end);
    }

    @Override
    public Response<Long> bitcount(byte[] key) {
        return pipeline.bitcount(key);
    }

    @Override
    public Response<Long> bitcount(byte[] key, long start, long end) {
        return pipeline.bitcount(key, start, end);
    }

    @Override
    public Response<byte[]> dump(String key) {
        return pipeline.dump(key);
    }

    @Override
    public Response<byte[]> dump(byte[] key) {
        return pipeline.dump(key);
    }

//    @Override
//    public Response<Long> pexpire(String key, int milliseconds) {
//        return pipeline.pexpire(key, milliseconds);
//    }
//
//    @Override
//    public Response<Long> pexpire(byte[] key, int milliseconds) {
//        return pipeline.pexpire(key, milliseconds);
//    }

    @Override
    public Response<Long> pexpire(String key, long milliseconds) {
        return pipeline.pexpire(key, milliseconds);
    }

    @Override
    public Response<Long> pexpire(byte[] key, long milliseconds) {
        return pipeline.pexpire(key, milliseconds);
    }

    @Override
    public Response<Long> pexpireAt(String key, long millisecondsTimestamp) {
        return pipeline.pexpireAt(key, millisecondsTimestamp);
    }

    @Override
    public Response<Long> pexpireAt(byte[] key, long millisecondsTimestamp) {
        return pipeline.pexpireAt(key, millisecondsTimestamp);
    }

    @Override
    public Response<Long> pttl(String key) {
        return pipeline.pttl(key);
    }

    @Override
    public Response<Long> pttl(byte[] key) {
        return pipeline.pttl(key);
    }

    @Override
    public Response<String> restore(String key, int ttl, byte[] serializedValue) {
        return pipeline.restore(key, ttl, serializedValue);
    }

    @Override
    public Response<String> restore(byte[] key, int ttl, byte[] serializedValue) {
        return pipeline.restore(key, ttl, serializedValue);
    }

    @Override
    public Response<Double> incrByFloat(String key, double increment) {
        return pipeline.incrByFloat(key, increment);
    }

    @Override
    public Response<Double> incrByFloat(byte[] key, double increment) {
        return pipeline.incrByFloat(key, increment);
    }

//    @Override
//    public Response<String> psetex(String key, int milliseconds, String value) {
//        return pipeline.psetex(key, milliseconds, value);
//    }
//
//    @Override
//    public Response<String> psetex(byte[] key, int milliseconds, byte[] value) {
//        return pipeline.psetex(key, milliseconds, value);
//    }

    @Override
    public Response<String> psetex(String key, long milliseconds, String value) {
        return pipeline.psetex(key, milliseconds, value);
    }

    @Override
    public Response<String> psetex(byte[] key, long milliseconds, byte[] value) {
        return pipeline.psetex(key, milliseconds, value);
    }

    @Override
    public Response<String> set(String key, String value, SetParams params) {
        return pipeline.set(key, value, params);
    }

    @Override
    public Response<String> set(byte[] key, byte[] value, SetParams params) {
        return pipeline.set(key, value, params);
    }

//    @Override
//    public Response<String> set(String key, String value, String nxxx) {
//        return pipeline.set(key, value, nxxx);
//    }
//
//    @Override
//    public Response<String> set(byte[] key, byte[] value, byte[] nxxx) {
//        return pipeline.set(key, value, nxxx);
//    }
//
//    @Override
//    public Response<String> set(String key, String value, String nxxx, String expx, int time) {
//        return pipeline.set(key, value, nxxx, expx, time);
//    }
//
//    @Override
//    public Response<String> set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, int time) {
//        return pipeline.set(key, value, nxxx, expx, time);
//    }

    @Override
    public Response<Double> hincrByFloat(String key, String field, double increment) {
        return pipeline.hincrByFloat(key, field, increment);
    }

    @Override
    public Response<Double> hincrByFloat(byte[] key, byte[] field, double increment) {
        return pipeline.hincrByFloat(key, field, increment);
    }


    @Override
    public Response<Long> pfadd(byte[] key, byte[]... elements) {
        return pipeline.pfadd(key, elements);
    }

    @Override
    public Response<Long> pfcount(byte[] key) {
        return pipeline.pfcount(key);
    }

    @Override
    public Response<Long> pfadd(String key, String... elements) {
        return pipeline.pfadd(key, elements);
    }

    @Override
    public Response<Long> pfcount(String key) {
        return pipeline.pfcount(key);
    }

    @Override
    public Response<Long> geoadd(byte[] key, double longitude, double latitude, byte[] member) {
        return pipeline.geoadd(key, longitude, latitude, member);
    }

    @Override
    public Response<Long> geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
        return pipeline.geoadd(key, memberCoordinateMap);
    }

    @Override
    public Response<Long> geoadd(String key, double longitude, double latitude, String member) {
        return pipeline.geoadd(key, longitude, latitude, member);
    }

    @Override
    public Response<Long> geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
        return pipeline.geoadd(key, memberCoordinateMap);
    }

    @Override
    public Response<Double> geodist(byte[] key, byte[] member1, byte[] member2) {
        return pipeline.geodist(key, member1, member2);
    }

    @Override
    public Response<Double> geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
        return pipeline.geodist(key, member1, member2, unit);
    }

    @Override
    public Response<Double> geodist(String key, String member1, String member2) {
        return pipeline.geodist(key, member1, member2);
    }

    @Override
    public Response<Double> geodist(String key, String member1, String member2, GeoUnit unit) {
        return pipeline.geodist(key, member1, member2, unit);
    }

    @Override
    public Response<List<byte[]>> geohash(byte[] key, byte[]... members) {
        return pipeline.geohash(key, members);
    }

    @Override
    public Response<List<String>> geohash(String key, String... members) {
        return pipeline.geohash(key, members);
    }

    @Override
    public Response<List<GeoCoordinate>> geopos(byte[] key, byte[]... members) {
        return pipeline.geopos(key, members);
    }

    @Override
    public Response<List<GeoCoordinate>> geopos(String key, String... members) {
        return pipeline.geopos(key, members);
    }

    @Override
    public Response<List<GeoRadiusResponse>> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
        return pipeline.georadius(key, longitude, latitude, radius, unit);
    }

    @Override
    public Response<List<GeoRadiusResponse>> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        return pipeline.georadius(key, longitude, latitude, radius, unit, param);
    }

    @Override
    public Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
        return pipeline.georadius(key, longitude, latitude, radius, unit);
    }

    @Override
    public Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        return pipeline.georadius(key, longitude, latitude, radius, unit, param);
    }

    @Override
    public Response<List<GeoRadiusResponse>> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit) {
        return pipeline.georadiusByMember(key, member, radius, unit);
    }

    @Override
    public Response<List<GeoRadiusResponse>> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
        return pipeline.georadiusByMember(key, member, radius, unit, param);
    }

    @Override
    public Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
        return pipeline.georadiusByMember(key, member, radius, unit);
    }

    @Override
    public Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
        return pipeline.georadiusByMember(key, member, radius, unit, param);
    }

    @Override
    public Response<List<Long>> bitfield(String key, String... elements) {
        return pipeline.bitfield(key, elements);
    }

    @Override
    public Response<List<Long>> bitfield(byte[] key, byte[]... elements) {
        return pipeline.bitfield(key, elements);
    }

    @Override
    protected <T> Response<T> getResponse(Builder<T> builder) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public void setClient(Client client) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    protected Client getClient(byte[] key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    protected Client getClient(String key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public boolean isInMulti() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<Object> syncAndReturnAll() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> discard() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<Object>> exec() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> multi() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    protected void clean() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    protected Response<?> generateResponse(Object data) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    protected boolean hasPipelinedResponse() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    protected int getPipelinedResponseLength() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<String>> brpop(String... args) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<String>> brpop(int timeout, String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<String>> blpop(String... args) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<String>> blpop(int timeout, String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Map<String, String>> blpopMap(int timeout, String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<byte[]>> brpop(byte[]... args) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<String>> brpop(int timeout, byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Map<String, String>> brpopMap(int timeout, String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<byte[]>> blpop(byte[]... args) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<String>> blpop(int timeout, byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> del(String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> del(byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> exists(String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> exists(byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Set<String>> keys(String pattern) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Set<byte[]>> keys(byte[] pattern) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<String>> mget(String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<byte[]>> mget(byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> mset(String... keysvalues) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> mset(byte[]... keysvalues) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> msetnx(String... keysvalues) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> msetnx(byte[]... keysvalues) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> rename(String oldkey, String newkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> rename(byte[] oldkey, byte[] newkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> renamenx(String oldkey, String newkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> renamenx(byte[] oldkey, byte[] newkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> rpoplpush(String srckey, String dstkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<byte[]> rpoplpush(byte[] srckey, byte[] dstkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Set<String>> sdiff(String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Set<byte[]>> sdiff(byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> sdiffstore(String dstkey, String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> sdiffstore(byte[] dstkey, byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Set<String>> sinter(String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Set<byte[]>> sinter(byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> sinterstore(String dstkey, String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> sinterstore(byte[] dstkey, byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> smove(String srckey, String dstkey, String member) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> smove(byte[] srckey, byte[] dstkey, byte[] member) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> sort(String key, SortingParams sortingParameters, String dstkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> sort(byte[] key, SortingParams sortingParameters, byte[] dstkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> sort(String key, String dstkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> sort(byte[] key, byte[] dstkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Set<String>> sunion(String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Set<byte[]>> sunion(byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> sunionstore(String dstkey, String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> sunionstore(byte[] dstkey, byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> watch(String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> watch(byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> zinterstore(String dstkey, String... sets) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> zinterstore(byte[] dstkey, byte[]... sets) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> zinterstore(String dstkey, ZParams params, String... sets) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> zinterstore(byte[] dstkey, ZParams params, byte[]... sets) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> zunionstore(String dstkey, String... sets) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> zunionstore(byte[] dstkey, byte[]... sets) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> zunionstore(String dstkey, ZParams params, String... sets) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> zunionstore(byte[] dstkey, ZParams params, byte[]... sets) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> bgrewriteaof() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> bgsave() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<String>> configGet(String pattern) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> configSet(String parameter, String value) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> brpoplpush(String source, String destination, int timeout) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<byte[]> brpoplpush(byte[] source, byte[] destination, int timeout) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> configResetStat() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> save() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> lastsave() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> publish(String channel, String message) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> publish(byte[] channel, byte[] message) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> randomKey() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<byte[]> randomKeyBinary() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> flushDB() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> flushAll() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> info() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> info(String section) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<String>> time() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> dbSize() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> shutdown() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> ping() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> select(int index) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> bitop(BitOP op, byte[] destKey, byte[]... srcKeys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> bitop(BitOP op, String destKey, String... srcKeys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> clusterNodes() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> clusterMeet(String ip, int port) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> clusterAddSlots(int... slots) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> clusterDelSlots(int... slots) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> clusterInfo() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<String>> clusterGetKeysInSlot(int slot, int count) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> clusterSetSlotNode(int slot, String nodeId) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> clusterSetSlotMigrating(int slot, String nodeId) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> clusterSetSlotImporting(int slot, String nodeId) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Object> eval(byte[] script) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Object> eval(byte[] script, byte[] keyCount, byte[]... params) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Object> eval(byte[] script, List<byte[]> keys, List<byte[]> args) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Object> eval(byte[] script, int keyCount, byte[]... params) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Object> evalsha(byte[] sha1) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Object> evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Object> evalsha(byte[] sha1, int keyCount, byte[]... params) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> pfmerge(byte[] destkey, byte[]... sourcekeys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> pfmerge(String destkey, String... sourcekeys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> pfcount(String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> pfcount(byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<byte[]> getrange(byte[] key, long startOffset, long endOffset) {
        //这里是因为jedis自己的bug，回包应该是Response<byte[]>
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<String>> blpop(String key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<String>> brpop(String key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<byte[]>> blpop(byte[] key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<List<byte[]>> brpop(byte[] key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> objectRefcount(byte[] key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> objectEncoding(String key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<byte[]> objectEncoding(byte[] key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> objectIdletime(String key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> objectIdletime(byte[] key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> move(String key, int dbIndex) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Long> move(byte[] key, int dbIndex) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<String> migrate(String host, int port, String key, int destinationDb, int timeout) {
        throw new UnsupportedOperationException("not support");
    }

//    @Override
//    public Response<String> migrate(byte[] host, int port, byte[] key, int destinationDb, int timeout) {
//        throw new UnsupportedOperationException("not support");
//    }



    @Override
    public Response<Long> objectRefcount(String key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Object> eval(String script) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Object> eval(String script, List<String> keys, List<String> args) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Object> eval(String script, int numKeys, String... args) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Object> evalsha(String script) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Object> evalsha(String sha1, List<String> keys, List<String> args) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Response<Object> evalsha(String sha1, int numKeys, String... args) {
        throw new UnsupportedOperationException("not support");
    }

}
