package com.netease.nim.camellia.redis;

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
public interface ICamelliaRedis {

    String set(byte[] key, byte[] value);

    byte[] get(byte[] key);

    String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time);

    Boolean exists(byte[] key);

    Long persist(byte[] key);

    String type(byte[] key);

    Long expire(byte[] key, int seconds);

    Long pexpire(byte[] key, long milliseconds);

    Long expireAt(byte[] key, long unixTime);

    Long pexpireAt(byte[] key, long millisecondsTimestamp);

    Long ttl(byte[] key);

    Boolean setbit(byte[] key, long offset, boolean value);

    Boolean setbit(byte[] key, long offset, byte[] value);

    Boolean getbit(byte[] key, long offset);

    Long setrange(byte[] key, long offset, byte[] value);

    byte[] getrange(byte[] key, long startOffset, long endOffset);

    byte[] getSet(byte[] key, byte[] value);

    Long setnx(byte[] key, byte[] value);

    String setex(byte[] key, int seconds, byte[] value);

    String psetex(byte[] key, long milliseconds, byte[] value);

    Long decrBy(byte[] key, long integer);

    Long decr(byte[] key);

    Long incrBy(byte[] key, long integer);

    Double incrByFloat(byte[] key, double value);

    Long incr(byte[] key);

    Long append(byte[] key, byte[] value);

    byte[] substr(byte[] key, int start, int end);

    Long hset(byte[] key, byte[] field, byte[] value);

    byte[] hget(byte[] key, byte[] field);

    Long hsetnx(byte[] key, byte[] field, byte[] value);

    String hmset(byte[] key, Map<byte[], byte[]> hash);

    List<byte[]> hmget(byte[] key, byte[]... fields);

    Long hincrBy(byte[] key, byte[] field, long value);

    Double hincrByFloat(byte[] key, byte[] field, double value);

    Boolean hexists(byte[] key, byte[] field);

    Long hdel(byte[] key, byte[]... field);

    Long hlen(byte[] key);

    Set<byte[]> hkeys(byte[] key);

    List<byte[]> hvals(byte[] key);

    Map<byte[], byte[]> hgetAll(byte[] key);

    Long rpush(byte[] key, byte[]... args);

    Long lpush(byte[] key, byte[]... args);

    Long llen(byte[] key);

    List<byte[]> lrange(byte[] key, long start, long end);

    String ltrim(byte[] key, long start, long end);

    byte[] lindex(byte[] key, long index);

    String lset(byte[] key, long index, byte[] value);

    Long lrem(byte[] key, long count, byte[] value);

    byte[] lpop(byte[] key);

    byte[] rpop(byte[] key);

    Long sadd(byte[] key, byte[]... member);

    Set<byte[]> smembers(byte[] key);

    Long srem(byte[] key, byte[]... member);

    byte[] spop(byte[] key);

    Set<byte[]> spop(byte[] key, long count);

    Long scard(byte[] key);

    Boolean sismember(byte[] key, byte[] member);

    byte[] srandmember(byte[] key);

    List<byte[]> srandmember(byte[] key, int count);

    Long strlen(byte[] key);

    Long zadd(byte[] key, double score, byte[] member);

    Long zadd(byte[] key, double score, byte[] member, ZAddParams params);

    Long zadd(byte[] key, Map<byte[], Double> scoreMembers);

    Long zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params);

    Set<byte[]> zrange(byte[] key, long start, long end);

    Long zrem(byte[] key, byte[]... member);

    Double zincrby(byte[] key, double score, byte[] member);

    Double zincrby(byte[] key, double score, byte[] member, ZIncrByParams params);

    Long zrank(byte[] key, byte[] member);

    Long zrevrank(byte[] key, byte[] member);

    Set<byte[]> zrevrange(byte[] key, long start, long end);

    Set<Tuple> zrangeWithScores(byte[] key, long start, long end);

    Set<Tuple> zrevrangeWithScores(byte[] key, long start, long end);

    Long zcard(byte[] key);

    Double zscore(byte[] key, byte[] member);

    List<byte[]> sort(byte[] key);

    List<byte[]> sort(byte[] key, SortingParams sortingParameters);

    Long zcount(byte[] key, double min, double max);

    Long zcount(byte[] key, byte[] min, byte[] max);

    Set<byte[]> zrangeByScore(byte[] key, double min, double max);

    Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max);

    Set<byte[]> zrevrangeByScore(byte[] key, double max, double min);

    Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count);

    Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min);

    Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count);

    Set<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count);

    Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max);

    Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min);

    Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count);

    Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count);

    Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max);

    Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min);

    Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count);

    Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count);

    Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count);

    Long zremrangeByRank(byte[] key, long start, long end);

    Long zremrangeByScore(byte[] key, double start, double end);

    Long zremrangeByScore(byte[] key, byte[] start, byte[] end);

    Long zlexcount(byte[] key, byte[] min, byte[] max);

    Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max);

    Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count);

    Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min);

    Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count);

    Long zremrangeByLex(byte[] key, byte[] min, byte[] max);

    Long linsert(byte[] key, BinaryClient.LIST_POSITION where, byte[] pivot, byte[] value);

    Long lpushx(byte[] key, byte[]... arg);

    Long rpushx(byte[] key, byte[]... arg);

    Long del(byte[] key);

    Long bitcount(byte[] key);

    Long bitcount(byte[] key, long start, long end);

    Long pfadd(byte[] key, byte[]... elements);

    long pfcount(byte[] key);

    Long geoadd(byte[] key, double longitude, double latitude, byte[] member);

    Long geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap);

    Double geodist(byte[] key, byte[] member1, byte[] member2);

    Double geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit);

    List<byte[]> geohash(byte[] key, byte[]... members);

    List<GeoCoordinate> geopos(byte[] key, byte[]... members);

    List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit);

    List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param);

    List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit);

    List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param);

    ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor);

    ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor, ScanParams params);

    ScanResult<byte[]> sscan(byte[] key, byte[] cursor);

    ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams params);

    ScanResult<Tuple> zscan(byte[] key, byte[] cursor);

    ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams params);

    List<Long> bitfield(byte[] key, byte[]... arguments);

    String set(String key, String value);

    String set(String key, String value, String nxxx, String expx, long time);

    String set(String key, String value, String nxxx);

    String get(String key);

    Boolean exists(String key);

    Long persist(String key);

    String type(String key);

    Long expire(String key, int seconds);

    Long pexpire(String key, long milliseconds);

    Long expireAt(String key, long unixTime);

    Long pexpireAt(String key, long millisecondsTimestamp);

    Long ttl(String key);

    Long pttl(String key);

    Boolean setbit(String key, long offset, boolean value);

    Boolean setbit(String key, long offset, String value);

    Boolean getbit(String key, long offset);

    Long setrange(String key, long offset, String value);

    String getrange(String key, long startOffset, long endOffset);

    String getSet(String key, String value);

    Long setnx(String key, String value);

    String setex(String key, int seconds, String value);

    String psetex(String key, long milliseconds, String value);

    Long decrBy(String key, long integer);

    Long decr(String key);

    Long incrBy(String key, long integer);

    Double incrByFloat(String key, double value);

    Long incr(String key);

    Long append(String key, String value);

    String substr(String key, int start, int end);

    Long hset(String key, String field, String value);

    String hget(String key, String field);

    Long hsetnx(String key, String field, String value);

    String hmset(String key, Map<String, String> hash);

    List<String> hmget(String key, String... fields);

    Long hincrBy(String key, String field, long value);

    Double hincrByFloat(String key, String field, double value);

    Boolean hexists(String key, String field);

    Long hdel(String key, String... field);

    Long hlen(String key);

    Set<String> hkeys(String key);

    List<String> hvals(String key);

    Map<String, String> hgetAll(String key);

    Long rpush(String key, String... string);

    Long lpush(String key, String... string);

    Long llen(String key);

    List<String> lrange(String key, long start, long end);

    String ltrim(String key, long start, long end);

    String lindex(String key, long index);

    String lset(String key, long index, String value);

    Long lrem(String key, long count, String value);

    String lpop(String key);

    String rpop(String key);

    Long sadd(String key, String... member);

    Set<String> smembers(String key);

    Long srem(String key, String... member);

    String spop(String key);

    Set<String> spop(String key, long count);

    Long scard(String key);

    Boolean sismember(String key, String member);

    String srandmember(String key);

    List<String> srandmember(String key, int count);

    Long strlen(String key);

    Long zadd(String key, double score, String member);

    Long zadd(String key, double score, String member, ZAddParams params);

    Long zadd(String key, Map<String, Double> scoreMembers);

    Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params);

    Set<String> zrange(String key, long start, long end);

    Long zrem(String key, String... member);

    Double zincrby(String key, double score, String member);

    Double zincrby(String key, double score, String member, ZIncrByParams params);

    Long zrank(String key, String member);

    Long zrevrank(String key, String member);

    Set<String> zrevrange(String key, long start, long end);

    Set<Tuple> zrangeWithScores(String key, long start, long end);

    Set<Tuple> zrevrangeWithScores(String key, long start, long end);

    Long zcard(String key);

    Double zscore(String key, String member);

    List<String> sort(String key);

    List<String> sort(String key, SortingParams sortingParameters);

    Long zcount(String key, double min, double max);

    Long zcount(String key, String min, String max);

    Set<String> zrangeByScore(String key, double min, double max);

    Set<String> zrangeByScore(String key, String min, String max);

    Set<String> zrevrangeByScore(String key, double max, double min);

    Set<String> zrangeByScore(String key, double min, double max, int offset, int count);

    Set<String> zrevrangeByScore(String key, String max, String min);

    Set<String> zrangeByScore(String key, String min, String max, int offset, int count);

    Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count);

    Set<Tuple> zrangeByScoreWithScores(String key, double min, double max);

    Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min);

    Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count);

    Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count);

    Set<Tuple> zrangeByScoreWithScores(String key, String min, String max);

    Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min);

    Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count);

    Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count);

    Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count);

    Long zremrangeByRank(String key, long start, long end);

    Long zremrangeByScore(String key, double start, double end);

    Long zremrangeByScore(String key, String start, String end);

    Long zlexcount(String key, String min, String max);

    Set<String> zrangeByLex(String key, String min, String max);

    Set<String> zrangeByLex(String key, String min, String max, int offset, int count);

    Set<String> zrevrangeByLex(String key, String max, String min);

    Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count);

    Long zremrangeByLex(String key, String min, String max);

    Long linsert(String key, BinaryClient.LIST_POSITION where, String pivot, String value);

    Long lpushx(String key, String... string);

    Long rpushx(String key, String... string);

    Long del(String key);

    String echo(String string);

    Long bitcount(String key);

    Long bitcount(String key, long start, long end);

    Long bitpos(String key, boolean value);

    Long bitpos(String key, boolean value, BitPosParams params);

    ScanResult<Map.Entry<String, String>> hscan(String key, String cursor);

    ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params);

    ScanResult<String> sscan(String key, String cursor);

    ScanResult<String> sscan(String key, String cursor, ScanParams params);

    ScanResult<Tuple> zscan(String key, String cursor);

    ScanResult<Tuple> zscan(String key, String cursor, ScanParams params);

    Long pfadd(String key, String... elements);

    long pfcount(String key);

    Long geoadd(String key, double longitude, double latitude, String member);

    Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap);

    Double geodist(String key, String member1, String member2);

    Double geodist(String key, String member1, String member2, GeoUnit unit);

    List<String> geohash(String key, String... members);

    List<GeoCoordinate> geopos(String key, String... members);

    List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit);

    List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param);

    List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit);

    List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param);

    List<Long> bitfield(String key, String... arguments);

    Long del(byte[]... keys);

    Long exists(byte[]... keys);

    Map<byte[], byte[]> mget(byte[]... keys);

    String mset(Map<byte[], byte[]> keysvalues);

    Long del(String... keys);

    Long exists(String... keys);

    Map<String, String> mget(String... keys);
}
