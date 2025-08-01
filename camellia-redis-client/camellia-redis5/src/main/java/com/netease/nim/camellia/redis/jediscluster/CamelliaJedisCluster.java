package com.netease.nim.camellia.redis.jediscluster;


import com.netease.nim.camellia.redis.ICamelliaRedis;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.base.resource.RedisClusterResource;
import com.netease.nim.camellia.redis.base.utils.SafeEncoder;
import com.netease.nim.camellia.redis.util.SetParamsUtils;
import redis.clients.jedis.*;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.GeoRadiusResponse;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;

import java.util.*;


/**
 * 封装了JedisCluster的接口
 * Created by caojiajun on 2019/7/22.
 */
public class CamelliaJedisCluster implements ICamelliaRedis {

    private final JedisClusterWrapper jedisCluster;

    public CamelliaJedisCluster(RedisClusterResource resource, CamelliaRedisEnv env) {
        this.jedisCluster = env.getJedisClusterFactory().getJedisCluster(resource);
    }

    @Override
    public List<Jedis> getJedisList() {
        return jedisCluster.getJedisList();
    }

    @Override
    public Jedis getJedis(byte[] key) {
        return jedisCluster.getJedis(key);
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
        return jedisCluster.set(key, value, SetParamsUtils.setParams(nxxx, expx, time));
    }

    @Override
    public String set(byte[] key, byte[] value, SetParams setParams) {
        return jedisCluster.set(key, value, setParams);
    }

    @Override
    public String set(String key, String value) {
        return jedisCluster.set(key, value);
    }

    @Override
    public String set(String key, String value, SetParams setParams) {
        return jedisCluster.set(key, value, setParams);
    }

    @Override
    public String set(String key, String value, String nxxx, String expx, long time) {
        return jedisCluster.set(key, value, SetParamsUtils.setParams(nxxx, expx, time));
    }

    @Override
    public String set(String key, String value, String nxxx) {
        return jedisCluster.set(key, value, SetParamsUtils.setParams(nxxx, null, 0));
    }

    @Override
    public String set(final byte[] key, final byte[] value, final byte[] nxxx) {
        SetParams setParams = SetParamsUtils.setParams(nxxx);
        return jedisCluster.set(key, value, setParams);
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
    public Long linsert(String key, ListPosition where, String pivot, String value) {
        return jedisCluster.linsert(key, where, pivot, value);
    }

    @Override
    public Long linsert(byte[] key, ListPosition where, byte[] pivot, byte[] value) {
        return jedisCluster.linsert(key, where, pivot, value);
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
    public List<Boolean> smismember(String key, String... members) {
        return jedisCluster.smismember(key, members);
    }

    @Override
    public List<Boolean> smismember(byte[] key, byte[]... members) {
        return jedisCluster.smismember(key, members);
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
    public List<String> zrange(String key, long start, long end) {
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
    public List<String> zrevrange(String key, long start, long end) {
        return jedisCluster.zrevrange(key, start, end);
    }

    @Override
    public List<Tuple> zrangeWithScores(String key, long start, long end) {
        return jedisCluster.zrangeWithScores(key, start, end);
    }

    @Override
    public List<Tuple> zrevrangeWithScores(String key, long start, long end) {
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
    public List<String> zrangeByScore(String key, double min, double max) {
        return jedisCluster.zrangeByScore(key, min, max);
    }

    @Override
    public List<String> zrangeByScore(String key, String min, String max) {
        return jedisCluster.zrangeByScore(key, min, max);
    }

    @Override
    public List<String> zrevrangeByScore(String key, double max, double min) {
        return jedisCluster.zrevrangeByScore(key, max, min);
    }

    @Override
    public List<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        return jedisCluster.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public List<String> zrevrangeByScore(String key, String max, String min) {
        return jedisCluster.zrevrangeByScore(key, max, min);
    }

    @Override
    public List<String> zrangeByScore(String key, String min, String max, int offset, int count) {
        return jedisCluster.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public List<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        return jedisCluster.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
        return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public List<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
        return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
        return jedisCluster.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
        return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
        return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
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
    public List<String> zrangeByLex(String key, String min, String max) {
        return jedisCluster.zrangeByLex(key, min, max);
    }

    @Override
    public List<String> zrangeByLex(String key, String min, String max, int offset, int count) {
        return jedisCluster.zrangeByLex(key, min, max, offset, count);
    }

    @Override
    public List<String> zrevrangeByLex(String key, String max, String min) {
        return jedisCluster.zrevrangeByLex(key, max, min);
    }

    @Override
    public List<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
        return jedisCluster.zrevrangeByLex(key, max, min, offset, count);
    }

    @Override
    public Long zremrangeByLex(String key, String min, String max) {
        return jedisCluster.zremrangeByLex(key, min, max);
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
        return jedisCluster.bitpos(key, value);
    }

    @Override
    public Long bitpos(final byte[] key, final boolean value, final BitPosParams params) {
        return jedisCluster.bitpos(key, value, params);
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
        if (keys.length == 1) {
            return jedisCluster.del(keys);
        } else {
            ClusterPipeline pipeline = jedisCluster.pipelined();
            List<Response<Long>> list = new ArrayList<>(keys.length);
            for (byte[] key : keys) {
                Response<Long> response = pipeline.del(key);
                list.add(response);
            }
            pipeline.sync();
            Long result = 0L;
            for (Response<Long> response : list) {
                result += response.get();
            }
            return result;
        }
    }

    @Override
    public Long exists(byte[]... keys) {
        if (keys == null) return 0L;
        if (keys.length == 0) return 0L;
        if (keys.length == 1) {
            return jedisCluster.exists(keys);
        } else {
            ClusterPipeline pipeline = jedisCluster.pipelined();
            List<Response<Boolean>> list = new ArrayList<>(keys.length);
            for (byte[] key : keys) {
                Response<Boolean> response = pipeline.exists(key);
                list.add(response);
            }
            pipeline.sync();
            long result = 0L;
            for (Response<Boolean> response : list) {
                result += response.get() ? 0 : 1;
            }
            return result;
        }
    }

    @Override
    public Map<byte[], byte[]> mget(byte[]... keys) {
        if (keys == null) return Collections.emptyMap();
        if (keys.length == 0) return Collections.emptyMap();
        final Map<byte[], byte[]> retMap = new HashMap<>();
        if (keys.length == 1) {
            List<byte[]> mget = jedisCluster.mget(keys);
            retMap.put(keys[0], mget.get(0));
        } else {
            ClusterPipeline pipeline = jedisCluster.pipelined();
            List<Response<byte[]>> list = new ArrayList<>(keys.length);
            for (byte[] key : keys) {
                Response<byte[]> response = pipeline.get(key);
                list.add(response);
            }
            pipeline.sync();
            for (int i=0; i<list.size(); i++) {
                retMap.put(keys[i], list.get(i).get());
            }
        }
        return retMap;
    }

    private static final String OK = "OK";

    @Override
    public String mset(Map<byte[], byte[]> keysvalues) {
        if (keysvalues == null) return null;
        if (keysvalues.isEmpty()) return null;
        if (keysvalues.size() == 1) {
            Map.Entry<byte[], byte[]> next = keysvalues.entrySet().iterator().next();
            return jedisCluster.mset(next.getKey(), next.getValue());
        } else {
            ClusterPipeline pipeline = jedisCluster.pipelined();
            List<Response<String>> list = new ArrayList<>();
            for (Map.Entry<byte[], byte[]> entry : keysvalues.entrySet()) {
                Response<String> response = pipeline.set(entry.getKey(), entry.getValue());
                list.add(response);
            }
            pipeline.sync();
            for (Response<String> response : list) {
                if (!response.get().equalsIgnoreCase(OK)) {
                    throw new CamelliaRedisException(response.get());
                }
            }
        }
        return OK;
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
        return jedisCluster.psetex(key, milliseconds, value);
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
        return jedisCluster.hvals(key);
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
    public List<byte[]> zrange(byte[] key, long start, long end) {
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
    public List<byte[]> zrevrange(byte[] key, long start, long end) {
        return jedisCluster.zrevrange(key, start, end);
    }

    @Override
    public List<Tuple> zrangeWithScores(byte[] key, long start, long end) {
        return jedisCluster.zrangeWithScores(key, start, end);
    }

    @Override
    public List<Tuple> zrevrangeWithScores(byte[] key, long start, long end) {
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
    public List<Double> zmscore(String key, String... members) {
        return jedisCluster.zmscore(key, members);
    }

    @Override
    public List<Double> zmscore(byte[] key, byte[]... members) {
        return jedisCluster.zmscore(key, members);
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
    public List<byte[]> zrangeByScore(byte[] key, double min, double max) {
        return jedisCluster.zrangeByScore(key, min, max);
    }

    @Override
    public List<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
        return jedisCluster.zrangeByScore(key, min, max);
    }

    @Override
    public List<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
        return jedisCluster.zrevrangeByScore(key, max, min);
    }

    @Override
    public List<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
        return jedisCluster.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public List<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
        return jedisCluster.zrevrangeByScore(key, max, min);
    }

    @Override
    public List<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return jedisCluster.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public List<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
        return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
        return jedisCluster.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
        return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
        return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public List<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
        return jedisCluster.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
        return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
        return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
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
    public List<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
        return jedisCluster.zrangeByLex(key, min, max);
    }

    @Override
    public List<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return jedisCluster.zrangeByLex(key, min, max, offset, count);
    }

    @Override
    public List<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
        return jedisCluster.zrevrangeByLex(key, max, min);
    }

    @Override
    public List<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return jedisCluster.zrevrangeByLex(key, max, min, offset, count);
    }

    @Override
    public Long zremrangeByLex(byte[] key, byte[] min, byte[] max) {
        return jedisCluster.zremrangeByLex(key, min, max);
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
        return jedisCluster.eval(script, keyCount, params);
    }

    @Override
    public Object evalsha(byte[] sha1, int keyCount, byte[]... params) {
        return jedisCluster.evalsha(sha1, keyCount, params);
    }

    @Override
    public byte[] dump(final String key) {
        return jedisCluster.dump(key);
    }

    @Override
    public byte[] dump(final byte[] key) {
        return jedisCluster.dump(key);
    }

    @Override
    public String restore(final byte[] key, final int ttl, final byte[] serializedValue) {
        return jedisCluster.restore(key, ttl, serializedValue);
    }

    @Override
    public String restore(final String key, final int ttl, final byte[] serializedValue) {
        return jedisCluster.restore(key, ttl, serializedValue);
    }
}
