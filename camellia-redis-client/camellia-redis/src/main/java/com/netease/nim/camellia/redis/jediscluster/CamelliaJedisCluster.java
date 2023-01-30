package com.netease.nim.camellia.redis.jediscluster;


import com.netease.nim.camellia.redis.ICamelliaRedis;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.base.resource.RedisClusterResource;
import com.netease.nim.camellia.redis.util.CloseUtil;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisClusterException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;
import redis.clients.util.SafeEncoder;

import java.util.*;
import java.util.concurrent.*;

import static redis.clients.jedis.Protocol.toByteArray;

/**
 * 封装了JedisCluster的接口
 * Created by caojiajun on 2019/7/22.
 */
public class CamelliaJedisCluster implements ICamelliaRedis {

    private final JedisClusterWrapper jedisCluster;
    private final CamelliaRedisEnv env;

    public CamelliaJedisCluster(RedisClusterResource resource, CamelliaRedisEnv env) {
        this.jedisCluster = env.getJedisClusterFactory().getJedisCluster(resource);
        this.env = env;
    }

    public List<Jedis> getJedisList() {
        List<JedisPool> jedisPoolList = jedisCluster.getJedisPoolList();
        List<Jedis> jedisList = new ArrayList<>();
        if (jedisPoolList == null || jedisPoolList.isEmpty()) {
            return jedisList;
        }
        for (JedisPool jedisPool : jedisPoolList) {
            Jedis jedis = jedisPool.getResource();
            jedisList.add(jedis);
        }
        return jedisList;
    }

    @Override
    public Jedis getJedis(byte[] key) {
        return jedisCluster.getJedisPool(key).getResource();
    }

    @Override
    public String set(byte[] key, byte[] value) {
        return jedisCluster.set(key, value);
    }

    @Override
    public byte[] get(byte[] key) {
        return jedisCluster.get(key);
    }

    @Override
    public String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
        return jedisCluster.set(key, value, nxxx, expx, time);
    }

    @Override
    public String set(String key, String value) {
        return jedisCluster.set(key, value);
    }

    @Override
    public String set(String key, String value, String nxxx, String expx, long time) {
        return jedisCluster.set(key, value, nxxx, expx, time);
    }

    @Override
    public String set(String key, String value, String nxxx) {
        return jedisCluster.set(key, value, nxxx);
    }

    @Override
    public String set(final byte[] key, final byte[] value, final byte[] nxxx) {
        return new JedisClusterCommand<String>(jedisCluster.getConnectionHandler(), jedisCluster.getMaxAttempts()) {
            @Override
            public String execute(Jedis connection) {
                return connection.set(key, value, nxxx);
            }
        }.runBinary(key);
    }

    @Override
    public String get(String key) {
        return jedisCluster.get(key);
    }

    @Override
    public Boolean exists(String key) {
        return jedisCluster.exists(key);
    }

    @Override
    public Long persist(String key) {
        return jedisCluster.persist(key);
    }

    @Override
    public String type(String key) {
        return jedisCluster.type(key);
    }

    @Override
    public Long expire(String key, int seconds) {
        return jedisCluster.expire(key, seconds);
    }

    @Override
    public Long pexpire(String key, long milliseconds) {
        return jedisCluster.pexpire(key, milliseconds);
    }

    @Override
    public Long expireAt(String key, long unixTime) {
        return jedisCluster.expireAt(key, unixTime);
    }

    @Override
    public Long pexpireAt(String key, long millisecondsTimestamp) {
        return jedisCluster.pexpireAt(key, millisecondsTimestamp);
    }

    @Override
    public Long ttl(String key) {
        return jedisCluster.ttl(key);
    }

    @Override
    public Long pttl(String key) {
        return jedisCluster.pttl(key);
    }

    @Override
    public Boolean setbit(String key, long offset, boolean value) {
        return jedisCluster.setbit(key, offset, value);
    }

    @Override
    public Boolean setbit(String key, long offset, String value) {
        return jedisCluster.setbit(key, offset, value);
    }

    @Override
    public Boolean getbit(String key, long offset) {
        return jedisCluster.getbit(key, offset);
    }

    @Override
    public Long setrange(String key, long offset, String value) {
        return jedisCluster.setrange(key, offset, value);
    }

    @Override
    public String getrange(String key, long startOffset, long endOffset) {
        return jedisCluster.getrange(key, startOffset, endOffset);
    }

    @Override
    public String getSet(String key, String value) {
        return jedisCluster.getSet(key, value);
    }

    @Override
    public Long setnx(String key, String value) {
        return jedisCluster.setnx(key, value);
    }

    @Override
    public String setex(String key, int seconds, String value) {
        return jedisCluster.setex(key, seconds, value);
    }

    @Override
    public String psetex(String key, long milliseconds, String value) {
        return jedisCluster.psetex(key, milliseconds, value);
    }

    @Override
    public Long decrBy(String key, long integer) {
        return jedisCluster.decrBy(key, integer);
    }

    @Override
    public Long decr(String key) {
        return jedisCluster.decr(key);
    }

    @Override
    public Long incrBy(String key, long integer) {
        return jedisCluster.incrBy(key, integer);
    }

    @Override
    public Double incrByFloat(String key, double value) {
        return jedisCluster.incrByFloat(key, value);
    }

    @Override
    public Long incr(String key) {
        return jedisCluster.incr(key);
    }

    @Override
    public Long append(String key, String value) {
        return jedisCluster.append(key, value);
    }

    @Override
    public String substr(String key, int start, int end) {
        return jedisCluster.substr(key, start, end);
    }

    @Override
    public Long hset(String key, String field, String value) {
        return jedisCluster.hset(key, field, value);
    }

    @Override
    public String hget(String key, String field) {
        return jedisCluster.hget(key, field);
    }

    @Override
    public Long hsetnx(String key, String field, String value) {
        return jedisCluster.hsetnx(key, field, value);
    }

    @Override
    public String hmset(String key, Map<String, String> hash) {
        return jedisCluster.hmset(key, hash);
    }

    @Override
    public List<String> hmget(String key, String... fields) {
        return jedisCluster.hmget(key, fields);
    }

    @Override
    public Long hincrBy(String key, String field, long value) {
        return jedisCluster.hincrBy(key, field, value);
    }

    @Override
    public Double hincrByFloat(String key, String field, double value) {
        return jedisCluster.hincrByFloat(key, field, value);
    }

    @Override
    public Boolean hexists(String key, String field) {
        return jedisCluster.hexists(key, field);
    }

    @Override
    public Long hdel(String key, String... field) {
        return jedisCluster.hdel(key, field);
    }

    @Override
    public Long hlen(String key) {
        return jedisCluster.hlen(key);
    }

    @Override
    public Set<String> hkeys(String key) {
        return jedisCluster.hkeys(key);
    }

    @Override
    public List<String> hvals(String key) {
        return jedisCluster.hvals(key);
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        return jedisCluster.hgetAll(key);
    }

    @Override
    public Long rpush(String key, String... string) {
        return jedisCluster.rpush(key, string);
    }

    @Override
    public Long lpush(String key, String... string) {
        return jedisCluster.lpush(key, string);
    }

    @Override
    public Long llen(String key) {
        return jedisCluster.llen(key);
    }

    @Override
    public List<String> lrange(String key, long start, long end) {
        return jedisCluster.lrange(key, start, end);
    }

    @Override
    public String ltrim(String key, long start, long end) {
        return jedisCluster.ltrim(key, start, end);
    }

    @Override
    public String lindex(String key, long index) {
        return jedisCluster.lindex(key, index);
    }

    @Override
    public String lset(String key, long index, String value) {
        return jedisCluster.lset(key, index, value);
    }

    @Override
    public Long lrem(String key, long count, String value) {
        return jedisCluster.lrem(key, count, value);
    }

    @Override
    public String lpop(String key) {
        return jedisCluster.lpop(key);
    }

    @Override
    public String rpop(String key) {
        return jedisCluster.rpop(key);
    }

    @Override
    public Long sadd(String key, String... member) {
        return jedisCluster.sadd(key, member);
    }

    @Override
    public Set<String> smembers(String key) {
        return jedisCluster.smembers(key);
    }

    @Override
    public Long srem(String key, String... member) {
        return jedisCluster.srem(key, member);
    }

    @Override
    public String spop(String key) {
        return jedisCluster.spop(key);
    }

    @Override
    public Set<String> spop(String key, long count) {
        return jedisCluster.spop(key, count);
    }

    @Override
    public Long scard(String key) {
        return jedisCluster.scard(key);
    }

    @Override
    public Boolean sismember(String key, String member) {
        return jedisCluster.sismember(key, member);
    }

    @Override
    public String srandmember(String key) {
        return jedisCluster.srandmember(key);
    }

    @Override
    public List<String> srandmember(String key, int count) {
        return jedisCluster.srandmember(key, count);
    }

    @Override
    public Long strlen(String key) {
        return jedisCluster.strlen(key);
    }

    @Override
    public Long zadd(String key, double score, String member) {
        return jedisCluster.zadd(key, score, member);
    }

    @Override
    public Long zadd(String key, double score, String member, ZAddParams params) {
        return jedisCluster.zadd(key, score, member, params);
    }

    @Override
    public Long zadd(String key, Map<String, Double> scoreMembers) {
        return jedisCluster.zadd(key, scoreMembers);
    }

    @Override
    public Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
        return jedisCluster.zadd(key, scoreMembers, params);
    }

    @Override
    public Set<String> zrange(String key, long start, long end) {
        return jedisCluster.zrange(key, start, end);
    }

    @Override
    public Long zrem(String key, String... member) {
        return jedisCluster.zrem(key, member);
    }

    @Override
    public Double zincrby(String key, double score, String member) {
        return jedisCluster.zincrby(key, score, member);
    }

    @Override
    public Double zincrby(String key, double score, String member, ZIncrByParams params) {
        return jedisCluster.zincrby(key, score, member, params);
    }

    @Override
    public Long zrank(String key, String member) {
        return jedisCluster.zrank(key, member);
    }

    @Override
    public Long zrevrank(String key, String member) {
        return jedisCluster.zrevrank(key, member);
    }

    @Override
    public Set<String> zrevrange(String key, long start, long end) {
        return jedisCluster.zrevrange(key, start, end);
    }

    @Override
    public Set<Tuple> zrangeWithScores(String key, long start, long end) {
        return jedisCluster.zrangeWithScores(key, start, end);
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
        return jedisCluster.zrevrangeWithScores(key, start, end);
    }

    @Override
    public Long zcard(String key) {
        return jedisCluster.zcard(key);
    }

    @Override
    public Double zscore(String key, String member) {
        return jedisCluster.zscore(key, member);
    }

    @Override
    public List<String> sort(String key) {
        return jedisCluster.sort(key);
    }

    @Override
    public List<String> sort(String key, SortingParams sortingParameters) {
        return jedisCluster.sort(key, sortingParameters);
    }

    @Override
    public Long zcount(String key, double min, double max) {
        return jedisCluster.zcount(key, min, max);
    }

    @Override
    public Long zcount(String key, String min, String max) {
        return jedisCluster.zcount(key, min, max);
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max) {
        return jedisCluster.zrangeByScore(key, min, max);
    }

    @Override
    public Set<String> zrangeByScore(String key, String min, String max) {
        return jedisCluster.zrangeByScore(key, min, max);
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min) {
        return jedisCluster.zrevrangeByScore(key, max, min);
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        return jedisCluster.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<String> zrevrangeByScore(String key, String max, String min) {
        return jedisCluster.zrevrangeByScore(key, max, min);
    }

    @Override
    public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
        return jedisCluster.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        return jedisCluster.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
        return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
        return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
        return jedisCluster.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
        return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
        return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
        return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Long zremrangeByRank(String key, long start, long end) {
        return jedisCluster.zremrangeByRank(key, start, end);
    }

    @Override
    public Long zremrangeByScore(String key, double start, double end) {
        return jedisCluster.zremrangeByScore(key, start, end);
    }

    @Override
    public Long zremrangeByScore(String key, String start, String end) {
        return jedisCluster.zremrangeByScore(key, start, end);
    }

    @Override
    public Long zlexcount(String key, String min, String max) {
        return jedisCluster.zlexcount(key, min, max);
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max) {
        return jedisCluster.zrangeByLex(key, min, max);
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max, int offset, int count) {
        return jedisCluster.zrangeByLex(key, min, max, offset, count);
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min) {
        return jedisCluster.zrevrangeByLex(key, max, min);
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
        return jedisCluster.zrevrangeByLex(key, max, min, offset, count);
    }

    @Override
    public Long zremrangeByLex(String key, String min, String max) {
        return jedisCluster.zremrangeByLex(key, min, max);
    }

    @Override
    public Long linsert(String key, BinaryClient.LIST_POSITION where, String pivot, String value) {
        return jedisCluster.linsert(key, where, pivot, value);
    }

    @Override
    public Long lpushx(String key, String... string) {
        return jedisCluster.lpushx(key, string);
    }

    @Override
    public Long rpushx(String key, String... string) {
        return jedisCluster.rpushx(key, string);
    }

    @Override
    public Long del(String key) {
        return jedisCluster.del(key);
    }

    @Override
    public String echo(String string) {
        return jedisCluster.echo(string);
    }

    @Override
    public Long bitcount(String key) {
        return jedisCluster.bitcount(key);
    }

    @Override
    public Long bitcount(String key, long start, long end) {
        return jedisCluster.bitcount(key, start, end);
    }

    @Override
    public Long bitpos(String key, boolean value) {
        return jedisCluster.bitpos(key, value);
    }

    @Override
    public Long bitpos(final byte[] key, final boolean value) {
        return new JedisClusterCommand<Long>(jedisCluster.getConnectionHandler(), jedisCluster.getMaxAttempts()) {
            @Override
            public Long execute(Jedis connection) {
                return connection.bitpos(key, value);
            }
        }.runBinary(key);
    }

    @Override
    public Long bitpos(final byte[] key, final boolean value, final BitPosParams params) {
        return new JedisClusterCommand<Long>(jedisCluster.getConnectionHandler(), jedisCluster.getMaxAttempts()) {
            @Override
            public Long execute(Jedis connection) {
                return connection.bitpos(key, value, params);
            }
        }.runBinary(key);
    }

    @Override
    public Long bitpos(String key, boolean value, BitPosParams params) {
        return jedisCluster.bitpos(key, value, params);
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor) {
        return jedisCluster.hscan(key, cursor);
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
        return jedisCluster.hscan(key, cursor, params);
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor) {
        return jedisCluster.sscan(key, cursor);
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
        return jedisCluster.sscan(key, cursor, params);
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor) {
        return jedisCluster.zscan(key, cursor);
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
        return jedisCluster.zscan(key, cursor, params);
    }

    @Override
    public Long pfadd(String key, String... elements) {
        return jedisCluster.pfadd(key, elements);
    }

    @Override
    public long pfcount(String key) {
        return jedisCluster.pfcount(key);
    }

    @Override
    public Long geoadd(String key, double longitude, double latitude, String member) {
        return jedisCluster.geoadd(key, longitude, latitude, member);
    }

    @Override
    public Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
        return jedisCluster.geoadd(key, memberCoordinateMap);
    }

    @Override
    public Double geodist(String key, String member1, String member2) {
        return jedisCluster.geodist(key, member1, member2);
    }

    @Override
    public Double geodist(String key, String member1, String member2, GeoUnit unit) {
        return jedisCluster.geodist(key, member1, member2, unit);
    }

    @Override
    public List<String> geohash(String key, String... members) {
        return jedisCluster.geohash(key, members);
    }

    @Override
    public List<GeoCoordinate> geopos(String key, String... members) {
        return jedisCluster.geopos(key, members);
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
        return jedisCluster.georadius(key, longitude, latitude, radius, unit);
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        return jedisCluster.georadius(key, longitude, latitude, radius, unit, param);
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
        return jedisCluster.georadiusByMember(key, member, radius, unit);
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
        return jedisCluster.georadiusByMember(key, member, radius, unit, param);
    }

    @Override
    public List<Long> bitfield(String key, String... arguments) {
        return jedisCluster.bitfield(key, arguments);
    }

    @Override
    public Long del(byte[]... keys) {
        if (keys == null) return 0L;
        if (keys.length == 0) return 0L;
        Map<JedisPool, List<byte[]>> map = toMap(keys);
        List<Future<Long>> futureList = new ArrayList<>();
        final ConcurrentHashMap<byte[], Exception> failMap = new ConcurrentHashMap<>();
        for (Map.Entry<JedisPool, List<byte[]>> entry : map.entrySet()) {
            final JedisPool jedisPool = entry.getKey();
            final List<byte[]> list = entry.getValue();
            if (list == null || list.isEmpty()) continue;
            Future<Long> future = env.getConcurrentExec().submit(() -> {
                Jedis jedis = null;
                try {
                    jedis = jedisPool.getResource();
                    Pipeline pipelined = jedis.pipelined();
                    List<Response<Long>> responseList = new ArrayList<>();
                    for (byte[] key : list) {
                        Response<Long> response = pipelined.del(key);
                        responseList.add(response);
                    }
                    pipelined.sync();
                    Long ret = 0L;
                    for (int i=0; i<responseList.size(); i++) {
                        Response<Long> response = responseList.get(i);
                        try {
                            ret += response.get();
                        } catch (Exception e) {
                            byte[] key = list.get(i);
                            handlerException(key, e, failMap);
                        }
                    }
                    return ret;
                } catch (Exception e) {
                    for (byte[] key : list) {
                        handlerException(key, e, failMap);
                    }
                    return 0L;
                } finally {
                    CloseUtil.closeQuietly(jedis);
                }
            });
            futureList.add(future);
        }
        Long ret = longFutureListGet(futureList);
        if (!failMap.isEmpty()) {
            List<Future<Long>> failFutureList = new ArrayList<>();
            for (final Map.Entry<byte[], Exception> entry : failMap.entrySet()) {
                Future<Long> future = env.getConcurrentExec().submit(() -> {
                    byte[] key = entry.getKey();
                    return jedisCluster.del(key);
                });
                failFutureList.add(future);
            }
            ret += longFutureListGet(failFutureList);
        }
        return ret;
    }

    @Override
    public Long exists(byte[]... keys) {
        if (keys == null) return 0L;
        if (keys.length == 0) return 0L;
        Map<JedisPool, List<byte[]>> map = toMap(keys);
        List<Future<Long>> futureList = new ArrayList<>();
        final ConcurrentHashMap<byte[], Exception> failMap = new ConcurrentHashMap<>();
        for (Map.Entry<JedisPool, List<byte[]>> entry : map.entrySet()) {
            final JedisPool jedisPool = entry.getKey();
            final List<byte[]> list = entry.getValue();
            if (list == null || list.isEmpty()) continue;
            Future<Long> future = env.getConcurrentExec().submit(() -> {
                Jedis jedis = null;
                try {
                    jedis = jedisPool.getResource();
                    Pipeline pipelined = jedis.pipelined();
                    List<Response<Boolean>> responseList = new ArrayList<>();
                    for (byte[] key : list) {
                        Response<Boolean> response = pipelined.exists(key);
                        responseList.add(response);
                    }
                    pipelined.sync();
                    long ret = 0L;
                    for (int i=0; i<responseList.size(); i++) {
                        Response<Boolean> response = responseList.get(i);
                        try {
                            ret += response.get() ? 1L : 0L;
                        } catch (Exception e) {
                            byte[] key = list.get(i);
                            handlerException(key, e, failMap);
                        }
                    }
                    return ret;
                } catch (Exception e) {
                    for (byte[] key : list) {
                        handlerException(key, e, failMap);
                    }
                    return 0L;
                } finally {
                    CloseUtil.closeQuietly(jedis);
                }
            });
            futureList.add(future);
        }
        Long ret = longFutureListGet(futureList);
        if (!failMap.isEmpty()) {
            List<Future<Long>> failFutureList = new ArrayList<>();
            for (final Map.Entry<byte[], Exception> entry : failMap.entrySet()) {
                Future<Long> future = env.getConcurrentExec().submit(() -> {
                    Boolean exists = jedisCluster.exists(entry.getKey());
                    return exists ? 1L : 0L;
                });
                failFutureList.add(future);
            }
            ret += longFutureListGet(failFutureList);
        }
        return ret;
    }

    private static final String OK = "OK";

    private void handlerException(byte[] key, Exception e, ConcurrentHashMap<byte[], Exception> failMap) throws CamelliaRedisException {
        if (e == null) return;
        if (e instanceof JedisMovedDataException) {
            jedisCluster.renewSlotCache();
            failMap.put(key, e);
            return;
        } else if (e instanceof JedisAskDataException) {
            failMap.put(key, e);
            return;
        } else if (e instanceof JedisConnectionException) {
            jedisCluster.renewSlotCache();
            failMap.put(key, e);
            return;
        } else if (e instanceof JedisClusterException) {
            jedisCluster.renewSlotCache();
            failMap.put(key, e);
            return;
        }
        throw new CamelliaRedisException(e);
    }

    @Override
    public Map<byte[], byte[]> mget(byte[]... keys) {
        if (keys == null) return Collections.emptyMap();
        if (keys.length == 0) return Collections.emptyMap();
        Map<JedisPool, List<byte[]>> map = toMap(keys);
        List<Future<?>> futureList = new ArrayList<>();
        final ConcurrentHashMap<byte[], Exception> failMap = new ConcurrentHashMap<>();
        final Map<byte[], byte[]> retMap = new HashMap<>();
        for (Map.Entry<JedisPool, List<byte[]>> entry : map.entrySet()) {
            final JedisPool jedisPool = entry.getKey();
            final List<byte[]> list = entry.getValue();
            if (list == null || list.isEmpty()) continue;
            Future<?> future = env.getConcurrentExec().submit(() -> {
                Jedis jedis = null;
                try {
                    jedis = jedisPool.getResource();
                    Pipeline pipelined = jedis.pipelined();
                    List<Response<byte[]>> responseList = new ArrayList<>();
                    for (byte[] key : list) {
                        Response<byte[]> response = pipelined.get(key);
                        responseList.add(response);
                    }
                    pipelined.sync();
                    for (int i=0; i<list.size(); i++) {
                        byte[] key = list.get(i);
                        Response<byte[]> response = responseList.get(i);
                        try {
                            synchronized (retMap) {
                                retMap.put(key, response.get());
                            }
                        } catch (Exception e) {
                            handlerException(key, e, failMap);
                        }
                    }
                } catch (Exception e) {
                    for (byte[] key : list) {
                        handlerException(key, e, failMap);
                    }
                } finally {
                    CloseUtil.closeQuietly(jedis);
                }
            });
            futureList.add(future);
        }
        try {
            for (Future<?> future : futureList) {
                future.get();
            }
        } catch (Exception e) {
            handlerFutureException(e);
        }
        if (!failMap.isEmpty()) {
            List<Future<?>> failFutureList = new ArrayList<>();
            for (final Map.Entry<byte[], Exception> entry : failMap.entrySet()) {
                Future<?> future = env.getConcurrentExec().submit(() -> {
                    byte[] key = entry.getKey();
                    byte[] value = jedisCluster.get(key);
                    synchronized (retMap) {
                        retMap.put(key, value);
                    }
                });
                failFutureList.add(future);
            }
            try {
                for (Future<?> future : failFutureList) {
                    future.get();
                }
            } catch (Exception e) {
                handlerFutureException(e);
            }
        }
        return retMap;
    }

    @Override
    public String mset(Map<byte[], byte[]> keysvalues) {
        if (keysvalues == null) return null;
        if (keysvalues.isEmpty()) return null;
        Map<JedisPool, List<Map.Entry<byte[], byte[]>>> map = new HashMap<>();
        for (Map.Entry<byte[], byte[]> entry : keysvalues.entrySet()) {
            byte[] key = entry.getKey();
            byte[] value = entry.getValue();
            if (key == null || value == null) continue;
            JedisPool jedisPool = jedisCluster.getJedisPool(key);
            addToMap(map, jedisPool, entry);
        }
        final ConcurrentHashMap<byte[], byte[]> failMap = new ConcurrentHashMap<>();
        List<Future<?>> futureList = new ArrayList<>();
        for (Map.Entry<JedisPool, List<Map.Entry<byte[], byte[]>>> entry : map.entrySet()) {
            final JedisPool jedisPool = entry.getKey();
            final List<Map.Entry<byte[], byte[]>> list = entry.getValue();
            if (list == null || list.isEmpty()) continue;
            Future<?> future = env.getConcurrentExec().submit(() -> {
                Jedis jedis = null;
                try {
                    jedis = jedisPool.getResource();
                    Pipeline pipelined = jedis.pipelined();
                    List<Response<String>> responseList = new ArrayList<>();
                    for (Map.Entry<byte[], byte[]> subEntry : list) {
                        Response<String> response = pipelined.set(subEntry.getKey(), subEntry.getValue());
                        responseList.add(response);
                    }
                    pipelined.sync();
                    for (int i=0; i<responseList.size(); i++) {
                        Response<String> response = responseList.get(i);
                        try {
                            String ret = response.get();
                            if (!ret.equalsIgnoreCase(OK)) {
                                throw new CamelliaRedisException(ret);
                            }
                        } catch (Exception e) {
                            Map.Entry<byte[], byte[]> subEntry = list.get(i);
                            msetHandlerException(e, subEntry.getKey(), subEntry.getValue(), failMap);
                        }
                    }
                } catch (Exception e) {
                    for (Map.Entry<byte[], byte[]> subEntry : list) {
                        msetHandlerException(e, subEntry.getKey(), subEntry.getValue(), failMap);
                    }
                } finally {
                    CloseUtil.closeQuietly(jedis);
                }
            });
            futureList.add(future);
        }
        try {
            for (Future<?> future : futureList) {
                future.get();
            }
        } catch (Exception e) {
            handlerFutureException(e);
        }
        if (!failMap.isEmpty()) {
            List<Future<?>> failFutureList = new ArrayList<>();
            for (final Map.Entry<byte[], byte[]> entry : failMap.entrySet()) {
                Future<?> future = env.getConcurrentExec().submit(() -> {
                    String set = jedisCluster.set(entry.getKey(), entry.getValue());
                    if (!set.equalsIgnoreCase(OK)) {
                        throw new CamelliaRedisException(set);
                    }
                });
                failFutureList.add(future);
            }
            try {
                for (Future<?> future : failFutureList) {
                    future.get();
                }
            } catch (Exception e) {
                handlerFutureException(e);
            }
        }
        return OK;
    }

    private void handlerFutureException(Exception e) {
        if (e instanceof ExecutionException) {
            Throwable cause = e.getCause();
            if (cause != null) {
                if (cause instanceof CamelliaRedisException) {
                    throw (CamelliaRedisException) cause;
                } else {
                    throw new CamelliaRedisException(cause);
                }
            } else {
                throw new CamelliaRedisException(e);
            }
        } else {
            throw new CamelliaRedisException(e);
        }
    }

    private void msetHandlerException(Exception e, byte[] key, byte[] value, ConcurrentHashMap<byte[], byte[]> failMap) throws CamelliaRedisException {
        if (e instanceof JedisConnectionException) {
            jedisCluster.renewSlotCache();
            failMap.put(key, value);
            return;
        } else if (e instanceof JedisMovedDataException) {
            jedisCluster.renewSlotCache();
            failMap.put(key, value);
            return;
        } else if (e instanceof JedisAskDataException) {
            failMap.put(key, value);
            return;
        } else if (e instanceof JedisClusterException) {
            jedisCluster.renewSlotCache();
            failMap.put(key, value);
            return;
        }
        throw new CamelliaRedisException(e);
    }

    @Override
    public Long del(String... keys) {
        if (keys == null) return 0L;
        if (keys.length == 0) return 0L;
        List<byte[]> keysRaw = new ArrayList<>();
        for (String key : keys) {
            keysRaw.add(SafeEncoder.encode(key));
        }
        return del(keysRaw.toArray(new byte[0][0]));
    }

    private Long longFutureListGet(List<Future<Long>> futureList) {
        if (futureList == null) return null;
        if (futureList.isEmpty()) return 0L;
        Long ret = 0L;
        try {
            for (Future<Long> future : futureList) {
                ret += future.get();
            }
            return ret;
        } catch (Exception e) {
            handlerFutureException(e);
            return ret;
        }
    }

    @SafeVarargs
    private final <T> Map<JedisPool, List<T>> toMap(T... keys) {
        Map<JedisPool, List<T>> map = new HashMap<>();
        for (T key : keys) {
            JedisPool jedisPool;
            if (key instanceof byte[]) {
                jedisPool = jedisCluster.getJedisPool((byte[]) key);
            } else if (key instanceof String) {
                jedisPool = jedisCluster.getJedisPool((String) key);
            } else {
                throw new IllegalArgumentException("key not byte[] or String");
            }
            addToMap(map, jedisPool, key);
        }
        return map;
    }

    private <K, V> void addToMap(Map<K, List<V>> map, K k, V v) {
        List<V> list = map.get(k);
        if (list == null) {
            list = new ArrayList<>();
            list.add(v);
            map.put(k, list);
        } else {
            list.add(v);
        }
    }

    @Override
    public Long exists(String... keys) {
        if (keys == null) return 0L;
        if (keys.length == 0) return 0L;
        List<byte[]> keysRaw = new ArrayList<>();
        for (String key : keys) {
            keysRaw.add(SafeEncoder.encode(key));
        }
        return exists(keysRaw.toArray(new byte[0][0]));
    }

    @Override
    public Map<String, String> mget(String... keys) {
        if (keys == null) return Collections.emptyMap();
        if (keys.length == 0) return Collections.emptyMap();
        byte[][] bytes = SafeEncoder.encodeMany(keys);
        Map<byte[], byte[]> map = mget(bytes);
        if (map == null) return Collections.emptyMap();
        Map<String, String> retMap = new HashMap<>();
        for (Map.Entry<byte[], byte[]> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                retMap.put(SafeEncoder.encode(entry.getKey()), null);
            } else {
                retMap.put(SafeEncoder.encode(entry.getKey()), SafeEncoder.encode(entry.getValue()));
            }
        }
        return retMap;
    }

    @Override
    public Boolean exists(byte[] key) {
        return jedisCluster.exists(key);
    }

    @Override
    public Long persist(byte[] key) {
        return jedisCluster.persist(key);
    }

    @Override
    public String type(byte[] key) {
        return jedisCluster.type(key);
    }

    @Override
    public Long expire(byte[] key, int seconds) {
        return jedisCluster.expire(key, seconds);
    }

    @Override
    public Long pexpire(byte[] key, long milliseconds) {
        return jedisCluster.pexpire(key, milliseconds);
    }

    @Override
    public Long expireAt(byte[] key, long unixTime) {
        return jedisCluster.expireAt(key, unixTime);
    }

    @Override
    public Long pexpireAt(byte[] key, long millisecondsTimestamp) {
        return jedisCluster.pexpireAt(key, millisecondsTimestamp);
    }

    @Override
    public Long ttl(byte[] key) {
        return jedisCluster.ttl(key);
    }

    @Override
    public Long pttl(byte[] key) {
        return jedisCluster.pttl(key);
    }

    @Override
    public Boolean setbit(byte[] key, long offset, boolean value) {
        return jedisCluster.setbit(key, offset, value);
    }

    @Override
    public Boolean setbit(byte[] key, long offset, byte[] value) {
        return jedisCluster.setbit(key, offset, value);
    }

    @Override
    public Boolean getbit(byte[] key, long offset) {
        return jedisCluster.getbit(key, offset);
    }

    @Override
    public Long setrange(byte[] key, long offset, byte[] value) {
        return jedisCluster.setrange(key, offset, value);
    }

    @Override
    public byte[] getrange(byte[] key, long startOffset, long endOffset) {
        return jedisCluster.getrange(key, startOffset, endOffset);
    }

    @Override
    public byte[] getSet(byte[] key, byte[] value) {
        return jedisCluster.getSet(key, value);
    }

    @Override
    public Long setnx(byte[] key, byte[] value) {
        return jedisCluster.setnx(key, value);
    }

    @Override
    public String setex(byte[] key, int seconds, byte[] value) {
        return jedisCluster.setex(key, seconds, value);
    }

    @Override
    public String psetex(final byte[] key, final long milliseconds, final byte[] value) {
        return new JedisClusterCommand<String>(jedisCluster.getConnectionHandler(), jedisCluster.getMaxAttempts()) {
            @Override
            public String execute(Jedis connection) {
                return connection.psetex(key, milliseconds, value);
            }
        }.runBinary(key);
    }

    @Override
    public Long decrBy(byte[] key, long integer) {
        return jedisCluster.decrBy(key, integer);
    }

    @Override
    public Long decr(byte[] key) {
        return jedisCluster.decr(key);
    }

    @Override
    public Long incrBy(byte[] key, long integer) {
        return jedisCluster.incrBy(key, integer);
    }

    @Override
    public Double incrByFloat(byte[] key, double value) {
        return jedisCluster.incrByFloat(key, value);
    }

    @Override
    public Long incr(byte[] key) {
        return jedisCluster.incr(key);
    }

    @Override
    public Long append(byte[] key, byte[] value) {
        return jedisCluster.append(key, value);
    }

    @Override
    public byte[] substr(byte[] key, int start, int end) {
        return jedisCluster.substr(key, start, end);
    }

    @Override
    public Long hset(byte[] key, byte[] field, byte[] value) {
        return jedisCluster.hset(key, field, value);
    }

    @Override
    public byte[] hget(byte[] key, byte[] field) {
        return jedisCluster.hget(key, field);
    }

    @Override
    public Long hsetnx(byte[] key, byte[] field, byte[] value) {
        return jedisCluster.hsetnx(key, field, value);
    }

    @Override
    public String hmset(byte[] key, Map<byte[], byte[]> hash) {
        return jedisCluster.hmset(key, hash);
    }

    @Override
    public List<byte[]> hmget(byte[] key, byte[]... fields) {
        return jedisCluster.hmget(key, fields);
    }

    @Override
    public Long hincrBy(byte[] key, byte[] field, long value) {
        return jedisCluster.hincrBy(key, field, value);
    }

    @Override
    public Double hincrByFloat(byte[] key, byte[] field, double value) {
        return jedisCluster.hincrByFloat(key, field, value);
    }

    @Override
    public Boolean hexists(byte[] key, byte[] field) {
        return jedisCluster.hexists(key, field);
    }

    @Override
    public Long hdel(byte[] key, byte[]... field) {
        return jedisCluster.hdel(key, field);
    }

    @Override
    public Long hlen(byte[] key) {
        return jedisCluster.hlen(key);
    }

    @Override
    public Set<byte[]> hkeys(byte[] key) {
        return jedisCluster.hkeys(key);
    }

    @Override
    public List<byte[]> hvals(byte[] key) {
        Collection<byte[]> list = jedisCluster.hvals(key);
        if (list instanceof List) {
            return (List<byte[]>) list;
        }
        return new ArrayList<>(list);
    }

    @Override
    public Map<byte[], byte[]> hgetAll(byte[] key) {
        return jedisCluster.hgetAll(key);
    }

    @Override
    public Long rpush(byte[] key, byte[]... args) {
        return jedisCluster.rpush(key, args);
    }

    @Override
    public Long lpush(byte[] key, byte[]... args) {
        return jedisCluster.lpush(key, args);
    }

    @Override
    public Long llen(byte[] key) {
        return jedisCluster.llen(key);
    }

    @Override
    public List<byte[]> lrange(byte[] key, long start, long end) {
        return jedisCluster.lrange(key, start, end);
    }

    @Override
    public String ltrim(byte[] key, long start, long end) {
        return jedisCluster.ltrim(key, start, end);
    }

    @Override
    public byte[] lindex(byte[] key, long index) {
        return jedisCluster.lindex(key, index);
    }

    @Override
    public String lset(byte[] key, long index, byte[] value) {
        return jedisCluster.lset(key, index, value);
    }

    @Override
    public Long lrem(byte[] key, long count, byte[] value) {
        return jedisCluster.lrem(key, count, value);
    }

    @Override
    public byte[] lpop(byte[] key) {
        return jedisCluster.lpop(key);
    }

    @Override
    public byte[] rpop(byte[] key) {
        return jedisCluster.rpop(key);
    }

    @Override
    public Long sadd(byte[] key, byte[]... member) {
        return jedisCluster.sadd(key, member);
    }

    @Override
    public Set<byte[]> smembers(byte[] key) {
        return jedisCluster.smembers(key);
    }

    @Override
    public Long srem(byte[] key, byte[]... member) {
        return jedisCluster.srem(key, member);
    }

    @Override
    public byte[] spop(byte[] key) {
        return jedisCluster.spop(key);
    }

    @Override
    public Set<byte[]> spop(byte[] key, long count) {
        return jedisCluster.spop(key, count);
    }

    @Override
    public Long scard(byte[] key) {
        return jedisCluster.scard(key);
    }

    @Override
    public Boolean sismember(byte[] key, byte[] member) {
        return jedisCluster.sismember(key, member);
    }

    @Override
    public byte[] srandmember(byte[] key) {
        return jedisCluster.srandmember(key);
    }

    @Override
    public List<byte[]> srandmember(byte[] key, int count) {
        return jedisCluster.srandmember(key, count);
    }

    @Override
    public Long strlen(byte[] key) {
        return jedisCluster.strlen(key);
    }

    @Override
    public Long zadd(byte[] key, double score, byte[] member) {
        return jedisCluster.zadd(key, score, member);
    }

    @Override
    public Long zadd(byte[] key, double score, byte[] member, ZAddParams params) {
        return jedisCluster.zadd(key, score, member, params);
    }

    @Override
    public Long zadd(byte[] key, Map<byte[], Double> scoreMembers) {
        return jedisCluster.zadd(key, scoreMembers);
    }

    @Override
    public Long zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
        return jedisCluster.zadd(key, scoreMembers, params);
    }

    @Override
    public Set<byte[]> zrange(byte[] key, long start, long end) {
        return jedisCluster.zrange(key, start, end);
    }

    @Override
    public Long zrem(byte[] key, byte[]... member) {
        return jedisCluster.zrem(key, member);
    }

    @Override
    public Double zincrby(byte[] key, double score, byte[] member) {
        return jedisCluster.zincrby(key, score, member);
    }

    @Override
    public Double zincrby(byte[] key, double score, byte[] member, ZIncrByParams params) {
        return jedisCluster.zincrby(key, score, member, params);
    }

    @Override
    public Long zrank(byte[] key, byte[] member) {
        return jedisCluster.zrank(key, member);
    }

    @Override
    public Long zrevrank(byte[] key, byte[] member) {
        return jedisCluster.zrevrank(key, member);
    }

    @Override
    public Set<byte[]> zrevrange(byte[] key, long start, long end) {
        return jedisCluster.zrevrange(key, start, end);
    }

    @Override
    public Set<Tuple> zrangeWithScores(byte[] key, long start, long end) {
        return jedisCluster.zrangeWithScores(key, start, end);
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(byte[] key, long start, long end) {
        return jedisCluster.zrevrangeWithScores(key, start, end);
    }

    @Override
    public Long zcard(byte[] key) {
        return jedisCluster.zcard(key);
    }

    @Override
    public Double zscore(byte[] key, byte[] member) {
        return jedisCluster.zscore(key, member);
    }

    @Override
    public List<byte[]> sort(byte[] key) {
        return jedisCluster.sort(key);
    }

    @Override
    public List<byte[]> sort(byte[] key, SortingParams sortingParameters) {
        return jedisCluster.sort(key, sortingParameters);
    }

    @Override
    public Long zcount(byte[] key, double min, double max) {
        return jedisCluster.zcount(key, min, max);
    }

    @Override
    public Long zcount(byte[] key, byte[] min, byte[] max) {
        return jedisCluster.zcount(key, min, max);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
        return jedisCluster.zrangeByScore(key, min, max);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
        return jedisCluster.zrangeByScore(key, min, max);
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
        return jedisCluster.zrevrangeByScore(key, max, min);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
        return jedisCluster.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
        return jedisCluster.zrevrangeByScore(key, max, min);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return jedisCluster.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
        return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
        return jedisCluster.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
        return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
        return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
        return jedisCluster.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
        return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
        return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Long zremrangeByRank(byte[] key, long start, long end) {
        return jedisCluster.zremrangeByRank(key, start, end);
    }

    @Override
    public Long zremrangeByScore(byte[] key, double start, double end) {
        return jedisCluster.zremrangeByScore(key, start, end);
    }

    @Override
    public Long zremrangeByScore(byte[] key, byte[] start, byte[] end) {
        return jedisCluster.zremrangeByScore(key, start, end);
    }

    @Override
    public Long zlexcount(byte[] key, byte[] min, byte[] max) {
        return jedisCluster.zlexcount(key, min, max);
    }

    @Override
    public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
        return jedisCluster.zrangeByLex(key, min, max);
    }

    @Override
    public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return jedisCluster.zrangeByLex(key, min, max, offset, count);
    }

    @Override
    public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
        return jedisCluster.zrevrangeByLex(key, max, min);
    }

    @Override
    public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return jedisCluster.zrevrangeByLex(key, max, min, offset, count);
    }

    @Override
    public Long zremrangeByLex(byte[] key, byte[] min, byte[] max) {
        return jedisCluster.zremrangeByLex(key, min, max);
    }

    @Override
    public Long linsert(byte[] key, BinaryClient.LIST_POSITION where, byte[] pivot, byte[] value) {
        return jedisCluster.linsert(key, where, pivot, value);
    }

    @Override
    public Long lpushx(byte[] key, byte[]... arg) {
        return jedisCluster.lpushx(key, arg);
    }

    @Override
    public Long rpushx(byte[] key, byte[]... arg) {
        return jedisCluster.rpushx(key, arg);
    }

    @Override
    public Long del(byte[] key) {
        return jedisCluster.del(key);
    }

    @Override
    public Long bitcount(byte[] key) {
        return jedisCluster.bitcount(key);
    }

    @Override
    public Long bitcount(byte[] key, long start, long end) {
        return jedisCluster.bitcount(key, start, end);
    }

    @Override
    public Long pfadd(byte[] key, byte[]... elements) {
        return jedisCluster.pfadd(key, elements);
    }

    @Override
    public long pfcount(byte[] key) {
        return jedisCluster.pfcount(key);
    }

    @Override
    public Long geoadd(byte[] key, double longitude, double latitude, byte[] member) {
        return jedisCluster.geoadd(key, longitude, latitude, member);
    }

    @Override
    public Long geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
        return jedisCluster.geoadd(key, memberCoordinateMap);
    }

    @Override
    public Double geodist(byte[] key, byte[] member1, byte[] member2) {
        return jedisCluster.geodist(key, member1, member2);
    }

    @Override
    public Double geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
        return jedisCluster.geodist(key, member1, member2, unit);
    }

    @Override
    public List<byte[]> geohash(byte[] key, byte[]... members) {
        return jedisCluster.geohash(key, members);
    }

    @Override
    public List<GeoCoordinate> geopos(byte[] key, byte[]... members) {
        return jedisCluster.geopos(key, members);
    }

    @Override
    public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
        return jedisCluster.georadius(key, longitude, latitude, radius, unit);
    }

    @Override
    public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        return jedisCluster.georadius(key, longitude, latitude, radius, unit, param);
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit) {
        return jedisCluster.georadiusByMember(key, member, radius, unit);
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
        return jedisCluster.georadiusByMember(key, member, radius, unit, param);
    }

    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor) {
        return jedisCluster.hscan(key, cursor);
    }

    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor, ScanParams params) {
        return jedisCluster.hscan(key, cursor, params);
    }

    @Override
    public ScanResult<byte[]> sscan(byte[] key, byte[] cursor) {
        return jedisCluster.sscan(key, cursor);
    }

    @Override
    public ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams params) {
        return jedisCluster.sscan(key, cursor, params);
    }

    @Override
    public ScanResult<Tuple> zscan(byte[] key, byte[] cursor) {
        return jedisCluster.zscan(key, cursor);
    }

    @Override
    public ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams params) {
        return jedisCluster.zscan(key, cursor, params);
    }

    @Override
    public List<Long> bitfield(byte[] key, byte[]... arguments) {
        return jedisCluster.bitfield(key, arguments);
    }

    @Override
    public Object eval(byte[] script, int keyCount, byte[]... params) {
        return new JedisClusterCommand<Object>(jedisCluster.getConnectionHandler(), jedisCluster.getMaxAttempts()) {
            @Override
            public Object execute(Jedis connection) {
                Client client = connection.getClient();
                client.eval(script, toByteArray(keyCount), params);
                return client.getOne();
            }
        }.runBinary(keyCount, params);
    }

    @Override
    public Object evalsha(byte[] sha1, int keyCount, byte[]... params) {
        return new JedisClusterCommand<Object>(jedisCluster.getConnectionHandler(), jedisCluster.getMaxAttempts()) {
            @Override
            public Object execute(Jedis connection) {
                Client client = connection.getClient();
                client.evalsha(sha1, toByteArray(keyCount), params);
                return client.getOne();
            }
        }.runBinary(keyCount, params);
    }

    @Override
    public byte[] dump(final String key) {
        return new JedisClusterCommand<byte[]>(jedisCluster.getConnectionHandler(), jedisCluster.getMaxAttempts()) {
            @Override
            public byte[] execute(Jedis connection) {
                return connection.dump(key);
            }
        }.runBinary(SafeEncoder.encode(key));
    }

    @Override
    public byte[] dump(final byte[] key) {
        return new JedisClusterCommand<byte[]>(jedisCluster.getConnectionHandler(), jedisCluster.getMaxAttempts()) {
            @Override
            public byte[] execute(Jedis connection) {
                return connection.dump(key);
            }
        }.runBinary(key);
    }

    @Override
    public String restore(final byte[] key, final int ttl, final byte[] serializedValue) {
        return new JedisClusterCommand<String>(jedisCluster.getConnectionHandler(), jedisCluster.getMaxAttempts()) {
            @Override
            public String execute(Jedis connection) {
                return connection.restore(key, ttl, serializedValue);
            }
        }.runBinary(key);
    }

    @Override
    public String restore(final String key, final int ttl, final byte[] serializedValue) {
        return new JedisClusterCommand<String>(jedisCluster.getConnectionHandler(), jedisCluster.getMaxAttempts()) {
            @Override
            public String execute(Jedis connection) {
                return connection.restore(key, ttl, serializedValue);
            }
        }.runBinary(SafeEncoder.encode(key));
    }
}
