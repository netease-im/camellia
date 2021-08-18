package com.netease.nim.camellia.redis.pipeline;

import redis.clients.jedis.*;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2019/7/22.
 */
public interface ICamelliaRedisPipeline extends Closeable {

    void sync();

    void close();

    Response<Long> append(byte[] key, byte[] value);

    Response<Long> decr(byte[] key);

    Response<Long> decrBy(byte[] key, long integer);

    Response<Long> del(byte[] key);

    Response<byte[]> echo(byte[] string);

    Response<Boolean> exists(byte[] key);

    Response<Long> expire(byte[] key, int seconds);

    Response<Long> pexpire(byte[] key, long milliseconds);

    Response<Long> expireAt(byte[] key, long unixTime);

    Response<Long> pexpireAt(byte[] key, long millisecondsTimestamp);

    Response<byte[]> get(byte[] key);

    Response<Boolean> getbit(byte[] key, long offset);

    Response<byte[]> getSet(byte[] key, byte[] value);

    Response<byte[]> getrange(byte[] key, long startOffset, long endOffset);

    Response<Long> hdel(byte[] key, byte[]... field);

    Response<Boolean> hexists(byte[] key, byte[] field);

    Response<byte[]> hget(byte[] key, byte[] field);

    Response<Map<byte[], byte[]>> hgetAll(byte[] key);

    Response<Long> hincrBy(byte[] key, byte[] field, long value);

    Response<Double> hincrByFloat(byte[] key, byte[] field, double value);

    Response<Double> hincrByFloat(String key, String field, double increment);

    Response<Set<byte[]>> hkeys(byte[] key);

    Response<Long> hlen(byte[] key);

    Response<List<byte[]>> hmget(byte[] key, byte[]... fields);

    Response<String> hmset(byte[] key, Map<byte[], byte[]> hash);

    Response<Long> hset(byte[] key, byte[] field, byte[] value);

    Response<Long> hsetnx(byte[] key, byte[] field, byte[] value);

    Response<List<byte[]>> hvals(byte[] key);

    Response<Long> incr(byte[] key);

    Response<Long> incrBy(byte[] key, long integer);

    Response<Double> incrByFloat(byte[] key, double integer);

    Response<Double> incrByFloat(String key, double increment);

    Response<byte[]> lindex(byte[] key, long index);

    Response<Long> linsert(byte[] key, ListPosition where, byte[] pivot, byte[] value);

    Response<Long> llen(byte[] key);

    Response<byte[]> lpop(byte[] key);

    Response<Long> lpush(byte[] key, byte[]... string);

    Response<Long> lpushx(byte[] key, byte[]... bytes);

    Response<List<byte[]>> lrange(byte[] key, long start, long end);

    Response<Long> lrem(byte[] key, long count, byte[] value);

    Response<String> lset(byte[] key, long index, byte[] value);

    Response<String> ltrim(byte[] key, long start, long end);

    Response<Long> persist(byte[] key);

    Response<byte[]> rpop(byte[] key);

    Response<Long> rpush(byte[] key, byte[]... string);

    Response<Long> rpushx(byte[] key, byte[]... string);

    Response<Long> sadd(byte[] key, byte[]... member);

    Response<Long> scard(byte[] key);

    Response<String> set(byte[] key, byte[] value);

    Response<String> set(String key, String value, SetParams params);

    Response<String> set(byte[] key, byte[] value, SetParams params);

//    Response<String> set(String key, String value, String nxxx);
//
//    Response<String> set(byte[] key, byte[] value, byte[] nxxx);
//
//    Response<String> set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time);
//
//    Response<String> set(String key, String value, String nxxx, String expx, int time);

    Response<Boolean> setbit(byte[] key, long offset, byte[] value);

    Response<Long> setrange(byte[] key, long offset, byte[] value);

    Response<String> setex(byte[] key, int seconds, byte[] value);

    Response<String> psetex(byte[] key, long milliseconds, byte[] value);

    Response<String> psetex(String key, long milliseconds, String value);

    Response<String> psetex(String key, int milliseconds, String value);

    Response<Long> setnx(byte[] key, byte[] value);

    Response<Set<byte[]>> smembers(byte[] key);

    Response<Boolean> sismember(byte[] key, byte[] member);

    Response<List<byte[]>> sort(byte[] key);

    Response<List<byte[]>> sort(byte[] key, SortingParams sortingParameters);

    Response<byte[]> spop(byte[] key);

    Response<Set<byte[]>> spop(byte[] key, long count);

    Response<byte[]> srandmember(byte[] key);

    Response<List<byte[]>> srandmember(byte[] key, int count);

    Response<List<String>> srandmember(String key, int count);

    Response<Long> srem(byte[] key, byte[]... member);

    Response<Long> strlen(byte[] key);

    Response<String> substr(byte[] key, int start, int end);

    Response<Long> ttl(byte[] key);

    Response<Long> pttl(byte[] key);

    Response<Long> pttl(String key);

    Response<String> type(byte[] key);

    Response<Long> zadd(byte[] key, double score, byte[] member);

    Response<Long> zadd(byte[] key, double score, byte[] member, ZAddParams params);

    Response<Long> zcard(byte[] key);

    Response<Long> zcount(byte[] key, double min, double max);

    Response<Double> zincrby(byte[] key, double score, byte[] member);

    Response<Double> zincrby(byte[] key, double score, byte[] member, ZIncrByParams params);

    Response<Set<byte[]>> zrange(byte[] key, long start, long end);

    Response<Set<byte[]>> zrangeByScore(byte[] key, double min, double max);

    Response<Set<String>> zrangeByScore(String key, String min, String max, int offset, int count);

    Response<Set<byte[]>> zrangeByScore(byte[] key, byte[] min, byte[] max);

    Response<Set<byte[]>> zrangeByScore(byte[] key, double min, double max, int offset, int count);

    Response<Set<byte[]>> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count);

    Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, double min, double max);

    Response<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max, int offset, int count);

    Response<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max);

    Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max);

    Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, double min, double max, int offset,
                                                 int count);

    Response<Set<Tuple>> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset,
                                                 int count);

    Response<Set<byte[]>> zrevrangeByScore(byte[] key, double max, double min);

    Response<Set<byte[]>> zrevrangeByScore(byte[] key, byte[] max, byte[] min);

    Response<Set<String>> zrevrangeByScore(String key, String max, String min, int offset, int count);

    Response<Set<byte[]>> zrevrangeByScore(byte[] key, double max, double min, int offset, int count);

    Response<Set<byte[]>> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count);

    Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, double max, double min);

    Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count);

    Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min);

    Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min);

    Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset,
                                                    int count);

    Response<Set<Tuple>> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset,
                                                    int count);

    Response<Set<Tuple>> zrangeWithScores(byte[] key, long start, long end);

    Response<Long> zrank(byte[] key, byte[] member);

    Response<Long> zrem(byte[] key, byte[]... member);

    Response<Long> zremrangeByRank(byte[] key, long start, long end);

    Response<Long> zremrangeByScore(byte[] key, double start, double end);

    Response<Long> zremrangeByScore(String key, String min, String max);

    Response<Long> zremrangeByScore(byte[] key, byte[] start, byte[] end);

    Response<Set<byte[]>> zrevrange(byte[] key, long start, long end);

    Response<Set<Tuple>> zrevrangeWithScores(byte[] key, long start, long end);

    Response<Long> zrevrank(byte[] key, byte[] member);

    Response<Double> zscore(byte[] key, byte[] member);

    Response<Long> zlexcount(final byte[] key, final byte[] min, final byte[] max);

    Response<Set<byte[]>> zrangeByLex(final byte[] key, final byte[] min, final byte[] max);

    Response<Set<byte[]>> zrangeByLex(final byte[] key, final byte[] min, final byte[] max,
                                      int offset, int count);

    Response<Set<byte[]>> zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min);

    Response<Set<byte[]>> zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min,
                                         int offset, int count);

    Response<Long> zremrangeByLex(final byte[] key, final byte[] min, final byte[] max);

    Response<Long> bitcount(byte[] key);

    Response<Long> bitcount(byte[] key, long start, long end);

    Response<Long> pfadd(final byte[] key, final byte[]... elements);

    Response<Long> pfcount(final byte[] key);

    Response<Long> geoadd(byte[] key, double longitude, double latitude, byte[] member);

    Response<Long> geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap);

    Response<Double> geodist(byte[] key, byte[] member1, byte[] member2);

    Response<Double> geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit);

    Response<List<byte[]>> geohash(byte[] key, byte[]... members);

    Response<List<GeoCoordinate>> geopos(byte[] key, byte[]... members);

    Response<List<GeoRadiusResponse>> georadius(byte[] key, double longitude, double latitude,
                                                double radius, GeoUnit unit);

    Response<List<GeoRadiusResponse>> georadius(byte[] key, double longitude, double latitude,
                                                double radius, GeoUnit unit, GeoRadiusParam param);

    Response<List<GeoRadiusResponse>> georadiusByMember(byte[] key, byte[] member, double radius,
                                                        GeoUnit unit);

    Response<List<GeoRadiusResponse>> georadiusByMember(byte[] key, byte[] member, double radius,
                                                        GeoUnit unit, GeoRadiusParam param);

    Response<List<Long>> bitfield(byte[] key, byte[]... elements);

    Response<Long> bitpos(byte[] key, boolean value, BitPosParams params);

    Response<Long> bitpos(String key, boolean value);

    Response<Long> bitpos(String key, boolean value, BitPosParams params);

    Response<Long> bitpos(byte[] key, boolean value);

    Response<Long> append(String key, String value);

    Response<Long> decr(String key);

    Response<Long> decrBy(String key, long integer);

    Response<Long> del(String key);

    Response<String> echo(String string);

    Response<Boolean> exists(String key);

    Response<Long> expire(String key, int seconds);

    Response<Long> pexpire(String key, long milliseconds);

    Response<Long> expireAt(String key, long unixTime);

    Response<Long> pexpireAt(String key, long millisecondsTimestamp);

    Response<String> get(String key);

    Response<Boolean> getbit(String key, long offset);

    Response<String> getrange(String key, long startOffset, long endOffset);

    Response<String> getSet(String key, String value);

    Response<Long> hdel(String key, String... field);

    Response<Boolean> hexists(String key, String field);

    Response<String> hget(String key, String field);

    Response<Map<String, String>> hgetAll(String key);

    Response<Long> hincrBy(String key, String field, long value);

    Response<Set<String>> hkeys(String key);

    Response<Long> hlen(String key);

    Response<List<String>> hmget(String key, String... fields);

    Response<String> hmset(String key, Map<String, String> hash);

    Response<Long> hset(String key, String field, String value);

    Response<Long> hsetnx(String key, String field, String value);

    Response<List<String>> hvals(String key);

    Response<Long> incr(String key);

    Response<Long> incrBy(String key, long integer);

    Response<String> lindex(String key, long index);

    Response<Long> linsert(String key, ListPosition where, String pivot, String value);

    Response<Long> llen(String key);

    Response<String> lpop(String key);

    Response<Long> lpush(String key, String... string);

    Response<Long> lpushx(String key, String... string);

    Response<List<String>> lrange(String key, long start, long end);

    Response<Long> lrem(String key, long count, String value);

    Response<String> lset(String key, long index, String value);

    Response<String> ltrim(String key, long start, long end);

    Response<Long> persist(String key);

    Response<String> rpop(String key);

    Response<Long> rpush(String key, String... string);

    Response<Long> rpushx(String key, String... string);

    Response<Long> sadd(String key, String... member);

    Response<Long> scard(String key);

    Response<Boolean> sismember(String key, String member);

    Response<String> set(String key, String value);

    Response<Boolean> setbit(String key, long offset, boolean value);

    Response<String> setex(String key, int seconds, String value);

    Response<Long> setnx(String key, String value);

    Response<Long> setrange(String key, long offset, String value);

    Response<Set<String>> smembers(String key);

    Response<List<String>> sort(String key);

    Response<List<String>> sort(String key, SortingParams sortingParameters);

    Response<String> spop(String key);

    Response<Set<String>> spop(String key, long count);

    Response<String> srandmember(String key);

    Response<Long> srem(String key, String... member);

    Response<Long> strlen(String key);

    Response<String> substr(String key, int start, int end);

    Response<Long> ttl(String key);

    Response<String> type(String key);

    Response<Long> zadd(String key, double score, String member);

    Response<Long> zadd(String key, double score, String member, ZAddParams params);

    Response<Long> zadd(String key, Map<String, Double> scoreMembers);

    Response<Long> zadd(String key, Map<String, Double> scoreMembers, ZAddParams params);

    Response<Long> zcard(String key);

    Response<Long> zadd(byte[] key, Map<byte[], Double> scoreMembers);

    Response<Long> zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params);

    Response<Long> zcount(String key, double min, double max);

    Response<Long> zcount(byte[] key, byte[] min, byte[] max);

    Response<Long> zcount(String key, String min, String max);

    Response<Double> zincrby(String key, double score, String member);

    Response<Double> zincrby(String key, double score, String member, ZIncrByParams params);

    Response<Set<String>> zrange(String key, long start, long end);

    Response<Set<String>> zrangeByScore(String key, double min, double max);

    Response<Set<String>> zrangeByScore(String key, String min, String max);

    Response<Set<String>> zrangeByScore(String key, double min, double max, int offset, int count);

    Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max);

    Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max, int offset,
                                                 int count);

    Response<Set<String>> zrevrangeByScore(String key, double max, double min);

    Response<Set<String>> zrevrangeByScore(String key, String max, String min);

    Response<Set<String>> zrevrangeByScore(String key, double max, double min, int offset, int count);

    Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min);

    Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min, int offset,
                                                    int count);

    Response<Set<Tuple>> zrangeWithScores(String key, long start, long end);

    Response<Long> zrank(String key, String member);

    Response<Long> zrem(String key, String... member);

    Response<Long> zremrangeByRank(String key, long start, long end);

    Response<Long> zremrangeByScore(String key, double start, double end);

    Response<Set<String>> zrevrange(String key, long start, long end);

    Response<Set<Tuple>> zrevrangeWithScores(String key, long start, long end);

    Response<Long> zrevrank(String key, String member);

    Response<Double> zscore(String key, String member);

    Response<Long> zlexcount(final String key, final String min, final String max);

    Response<Set<String>> zrangeByLex(final String key, final String min, final String max);

    Response<Set<String>> zrangeByLex(final String key, final String min, final String max,
                                      final int offset, final int count);

    Response<Set<String>> zrevrangeByLex(final String key, final String max, final String min);

    Response<Set<String>> zrevrangeByLex(final String key, final String max, final String min,
                                         final int offset, final int count);

    Response<Long> zremrangeByLex(final String key, final String start, final String end);

    Response<Long> bitcount(String key);

    Response<Long> bitcount(String key, long start, long end);

    Response<List<Long>> bitfield(String key, String... arguments);

    Response<Long> pfadd(final String key, final String... elements);

    Response<Long> pfcount(final String key);

    Response<Long> geoadd(String key, double longitude, double latitude, String member);

    Response<Long> geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap);

    Response<Double> geodist(String key, String member1, String member2);

    Response<Double> geodist(String key, String member1, String member2, GeoUnit unit);

    Response<List<String>> geohash(String key, String... members);

    Response<List<GeoCoordinate>> geopos(String key, String... members);

    Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude,
                                                double radius, GeoUnit unit);

    Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude,
                                                double radius, GeoUnit unit, GeoRadiusParam param);

    Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius,
                                                        GeoUnit unit);

    Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius,
                                                        GeoUnit unit, GeoRadiusParam param);


    Response<byte[]> dump(String key);

    Response<byte[]> dump(byte[] key);


    Response<String> restore(byte[] key, int ttl, byte[] serializedValue);

    Response<String> restore(String key, int ttl, byte[] serializedValue);
}
