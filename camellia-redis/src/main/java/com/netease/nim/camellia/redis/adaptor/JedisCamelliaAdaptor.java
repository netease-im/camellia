package com.netease.nim.camellia.redis.adaptor;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import redis.clients.jedis.*;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;
import redis.clients.util.Pool;

import redis.clients.util.Slowlog;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * Created by caojiajun on 2021/7/26
 */
public class JedisCamelliaAdaptor extends Jedis {

    private final CamelliaRedisTemplate template;
    private final JedisPoolCamelliaAdaptor jedisPoolCamelliaAdaptor;
    private ICamelliaRedisPipeline pipelined;
    private final AtomicBoolean open = new AtomicBoolean(false);

    JedisCamelliaAdaptor(CamelliaRedisTemplate template, JedisPoolCamelliaAdaptor jedisPoolCamelliaAdaptor) {
        this.template = template;
        this.jedisPoolCamelliaAdaptor = jedisPoolCamelliaAdaptor;
    }

    public CamelliaRedisTemplate getCamelliaRedisTemplate() {
        return template;
    }

    void open() {
        open.set(true);
    }

    @Override
    public Pipeline pipelined() {
        if (pipelined == null) {
            pipelined = template.pipelined();
        }
        return new PipelineCamelliaAdaptor(pipelined);
    }

    @Override
    public void close() {
        if (pipelined != null) {
            pipelined.close();
            pipelined = null;
        }
        open.set(false);
        if (jedisPoolCamelliaAdaptor != null) {
            jedisPoolCamelliaAdaptor.returnJedisCamelliaAdaptor(this);
        }
    }

    private void check() {
        if (!open.get()) {
            throw new CamelliaRedisException("JedisCamelliaAdaptor is closed");
        }
    }

    @Override
    public String ping() {
        check();
        //do nothing
        return "pong";
    }

    @Override
    public String quit() {
        check();
        return "ok";
    }

    @Override
    public String set(String key, String value) {
        check();
        return template.set(key, value);
    }

    @Override
    public String set(String key, String value, String nxxx, String expx, long time) {
        check();
        return template.set(key, value, nxxx, expx, time);
    }

    @Override
    public String get(String key) {
        check();
        return template.get(key);
    }

    @Override
    public Long exists(String... keys) {
        check();
        return template.exists(keys);
    }

    @Override
    public Boolean exists(String key) {
        check();
        return template.exists(key);
    }

    @Override
    public Long del(String... keys) {
        check();
        return template.del(keys);
    }

    @Override
    public Long del(String key) {
        check();
        return template.del(key);
    }

    @Override
    public String type(String key) {
        check();
        return template.type(key);
    }

    @Override
    public Long expire(String key, int seconds) {
        check();
        return template.expire(key, seconds);
    }

    @Override
    public Long expireAt(String key, long unixTime) {
        check();
        return template.expireAt(key, unixTime);
    }

    @Override
    public Long ttl(String key) {
        check();
        return template.ttl(key);
    }

    @Override
    public String getSet(String key, String value) {
        check();
        return template.getSet(key, value);
    }

    @Override
    public List<String> mget(String... keys) {
        check();
        return template.mget(keys);
    }

    @Override
    public Long setnx(String key, String value) {
        check();
        return template.setnx(key, value);
    }

    @Override
    public String setex(String key, int seconds, String value) {
        check();
        return template.setex(key, seconds, value);
    }

    @Override
    public String mset(String... keysvalues) {
        check();
        return template.mset(keysvalues);
    }

    @Override
    public Long decrBy(String key, long decrement) {
        check();
        return template.decrBy(key, decrement);
    }

    @Override
    public Long decr(String key) {
        check();
        return template.decr(key);
    }

    @Override
    public Long incrBy(String key, long increment) {
        check();
        return template.incrBy(key, increment);
    }

    @Override
    public Double incrByFloat(String key, double increment) {
        check();
        return template.incrByFloat(key, increment);
    }

    @Override
    public Long incr(String key) {
        check();
        return template.incr(key);
    }

    @Override
    public Long append(String key, String value) {
        check();
        return template.append(key, value);
    }

    @Override
    public String substr(String key, int start, int end) {
        check();
        return template.substr(key, start, end);
    }

    @Override
    public Long hset(String key, String field, String value) {
        check();
        return template.hset(key, field, value);
    }

    @Override
    public String hget(String key, String field) {
        check();
        return template.hget(key, field);
    }

    @Override
    public Long hsetnx(String key, String field, String value) {
        check();
        return template.hsetnx(key, field, value);
    }

    @Override
    public String hmset(String key, Map<String, String> hash) {
        check();
        return template.hmset(key, hash);
    }

    @Override
    public List<String> hmget(String key, String... fields) {
        check();
        return template.hmget(key, fields);
    }

    @Override
    public Long hincrBy(String key, String field, long value) {
        check();
        return template.hincrBy(key, field, value);
    }

    @Override
    public Double hincrByFloat(String key, String field, double value) {
        check();
        return template.hincrByFloat(key, field, value);
    }

    @Override
    public Boolean hexists(String key, String field) {
        check();
        return template.hexists(key, field);
    }

    @Override
    public Long hdel(String key, String... fields) {
        check();
        return template.hdel(key, fields);
    }

    @Override
    public Long hlen(String key) {
        check();
        return template.hlen(key);
    }

    @Override
    public Set<String> hkeys(String key) {
        check();
        return template.hkeys(key);
    }

    @Override
    public List<String> hvals(String key) {
        check();
        return template.hvals(key);
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        check();
        return template.hgetAll(key);
    }

    @Override
    public Long rpush(String key, String... strings) {
        check();
        return template.rpush(key, strings);
    }

    @Override
    public Long lpush(String key, String... strings) {
        check();
        return template.lpush(key, strings);
    }

    @Override
    public Long llen(String key) {
        check();
        return template.llen(key);
    }

    @Override
    public List<String> lrange(String key, long start, long stop) {
        check();
        return template.lrange(key, start, stop);
    }

    @Override
    public String ltrim(String key, long start, long stop) {
        check();
        return template.ltrim(key, start, stop);
    }

    @Override
    public String lindex(String key, long index) {
        check();
        return template.lindex(key, index);
    }

    @Override
    public String lset(String key, long index, String value) {
        check();
        return template.lset(key, index, value);
    }

    @Override
    public Long lrem(String key, long count, String value) {
        check();
        return template.lrem(key, count, value);
    }

    @Override
    public String lpop(String key) {
        check();
        return template.lpop(key);
    }

    @Override
    public String rpop(String key) {
        check();
        return template.rpop(key);
    }

    @Override
    public Long sadd(String key, String... members) {
        check();
        return template.sadd(key, members);
    }

    @Override
    public Set<String> smembers(String key) {
        check();
        return template.smembers(key);
    }

    @Override
    public Long srem(String key, String... members) {
        check();
        return template.srem(key, members);
    }

    @Override
    public String spop(String key) {
        check();
        return template.spop(key);
    }

    @Override
    public Set<String> spop(String key, long count) {
        check();
        return template.spop(key, count);
    }

    @Override
    public Long scard(String key) {
        check();
        return template.scard(key);
    }

    @Override
    public Boolean sismember(String key, String member) {
        check();
        return template.sismember(key, member);
    }

    @Override
    public String srandmember(String key) {
        check();
        return template.srandmember(key);
    }

    @Override
    public List<String> srandmember(String key, int count) {
        check();
        return template.srandmember(key, count);
    }

    @Override
    public Long zadd(String key, double score, String member) {
        check();
        return template.zadd(key, score, member);
    }

    @Override
    public Long zadd(String key, double score, String member, ZAddParams params) {
        check();
        return template.zadd(key, score, member, params);
    }

    @Override
    public Long zadd(String key, Map<String, Double> scoreMembers) {
        check();
        return template.zadd(key, scoreMembers);
    }

    @Override
    public Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
        check();
        return template.zadd(key, scoreMembers, params);
    }

    @Override
    public Set<String> zrange(String key, long start, long stop) {
        check();
        return template.zrange(key, start, stop);
    }

    @Override
    public Long zrem(String key, String... members) {
        check();
        return template.zrem(key, members);
    }

    @Override
    public Double zincrby(String key, double increment, String member) {
        check();
        return template.zincrby(key, increment, member);
    }

    @Override
    public Double zincrby(String key, double increment, String member, ZIncrByParams params) {
        check();
        return template.zincrby(key, increment, member, params);
    }

    @Override
    public Long zrank(String key, String member) {
        check();
        return template.zrank(key, member);
    }

    @Override
    public Long zrevrank(String key, String member) {
        check();
        return template.zrevrank(key, member);
    }

    @Override
    public Set<String> zrevrange(String key, long start, long stop) {
        check();
        return template.zrevrange(key, start, stop);
    }

    @Override
    public Set<Tuple> zrangeWithScores(String key, long start, long stop) {
        check();
        return template.zrangeWithScores(key, start, stop);
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(String key, long start, long stop) {
        check();
        return template.zrevrangeWithScores(key, start, stop);
    }

    @Override
    public Long zcard(String key) {
        check();
        return template.zcard(key);
    }

    @Override
    public Double zscore(String key, String member) {
        check();
        return template.zscore(key, member);
    }

    @Override
    public List<String> sort(String key) {
        check();
        return template.sort(key);
    }

    @Override
    public List<String> sort(String key, SortingParams sortingParameters) {
        check();
        return template.sort(key, sortingParameters);
    }

    @Override
    public Long zcount(String key, double min, double max) {
        check();
        return template.zcount(key, min, max);
    }

    @Override
    public Long zcount(String key, String min, String max) {
        check();
        return template.zcount(key, min, max);
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max) {
        check();
        return template.zrangeByScore(key, min, max);
    }

    @Override
    public Set<String> zrangeByScore(String key, String min, String max) {
        check();
        return template.zrangeByScore(key, min, max);
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        check();
        return template.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
        check();
        return template.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        check();
        return template.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
        check();
        return template.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        check();
        return template.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
        check();
        return template.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min) {
        check();
        return template.zrevrangeByScore(key, max, min);
    }

    @Override
    public Set<String> zrevrangeByScore(String key, String max, String min) {
        check();
        return template.zrevrangeByScore(key, max, min);
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        check();
        return template.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
        check();
        return template.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        check();
        return template.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
        check();
        return template.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
        check();
        return template.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
        check();
        return template.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Long zremrangeByRank(String key, long start, long stop) {
        check();
        return template.zremrangeByRank(key, start, stop);
    }

    @Override
    public Long zremrangeByScore(String key, double min, double max) {
        check();
        return template.zremrangeByScore(key, min, max);
    }

    @Override
    public Long zremrangeByScore(String key, String min, String max) {
        check();
        return template.zremrangeByScore(key, min, max);
    }

    @Override
    public Long zlexcount(String key, String min, String max) {
        check();
        return template.zlexcount(key, min, max);
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max) {
        check();
        return template.zrangeByLex(key, min, max);
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max, int offset, int count) {
        check();
        return template.zrangeByLex(key, min, max, offset, count);
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min) {
        check();
        return template.zrevrangeByLex(key, max, min);
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
        check();
        return template.zrevrangeByLex(key, max, min, offset, count);
    }

    @Override
    public Long zremrangeByLex(String key, String min, String max) {
        check();
        return template.zremrangeByLex(key, min, max);
    }

    @Override
    public Long strlen(String key) {
        check();
        return template.strlen(key);
    }

    @Override
    public Long lpushx(String key, String... string) {
        check();
        return template.lpushx(key, string);
    }

    @Override
    public Long persist(String key) {
        check();
        return template.persist(key);
    }

    @Override
    public Long rpushx(String key, String... string) {
        check();
        return template.rpushx(key, string);
    }

    @Override
    public String echo(String string) {
        check();
        return template.echo(string);
    }

    @Override
    public Long linsert(String key, BinaryClient.LIST_POSITION where, String pivot, String value) {
        check();
        return template.linsert(key, where, pivot, value);
    }

    @Override
    public Boolean setbit(String key, long offset, boolean value) {
        check();
        return template.setbit(key, offset, value);
    }

    @Override
    public Boolean setbit(String key, long offset, String value) {
        check();
        return template.setbit(key, offset, value);
    }

    @Override
    public Boolean getbit(String key, long offset) {
        check();
        return template.getbit(key, offset);
    }

    @Override
    public Long setrange(String key, long offset, String value) {
        check();
        return template.setrange(key, offset, value);
    }

    @Override
    public String getrange(String key, long startOffset, long endOffset) {
        check();
        return template.getrange(key, startOffset, endOffset);
    }

    @Override
    public Long bitpos(String key, boolean value) {
        check();
        return template.bitpos(key, value);
    }

    @Override
    public Long bitpos(String key, boolean value, BitPosParams params) {
        check();
        return template.bitpos(key, value, params);
    }

    @Override
    public Object eval(String script, int keyCount, String... params) {
        check();
        return template.eval(script, keyCount, params);
    }

    @Override
    public Object eval(String script, List<String> keys, List<String> args) {
        check();
        return template.eval(script, keys, args);
    }

    @Override
    public Object eval(String script) {
        check();
        return template.eval(script);
    }

    @Override
    public Object evalsha(String sha1) {
        check();
        return template.evalsha(sha1);
    }

    @Override
    public Object evalsha(String sha1, List<String> keys, List<String> args) {
        check();
        return template.evalsha(sha1, keys, args);
    }

    @Override
    public Object evalsha(String sha1, int keyCount, String... params) {
        check();
        return template.evalsha(sha1, keyCount, params);
    }

    @Override
    public Long bitcount(String key) {
        check();
        return template.bitcount(key);
    }

    @Override
    public Long bitcount(String key, long start, long end) {
        check();
        return template.bitcount(key, start, end);
    }

    @Override
    public byte[] dump(String key) {
        check();
        return template.dump(key);
    }

    @Override
    public String restore(String key, int ttl, byte[] serializedValue) {
        check();
        return template.restore(key, ttl, serializedValue);
    }

    @Override
    public Long pexpire(String key, int milliseconds) {
        check();
        return template.pexpire(key, milliseconds);
    }

    @Override
    public Long pexpire(String key, long milliseconds) {
        check();
        return template.pexpire(key, milliseconds);
    }

    @Override
    public Long pexpireAt(String key, long millisecondsTimestamp) {
        check();
        return template.pexpireAt(key, millisecondsTimestamp);
    }

    @Override
    public Long pttl(String key) {
        check();
        return template.pttl(key);
    }

    @Override
    public String psetex(String key, int milliseconds, String value) {
        check();
        return template.psetex(key, milliseconds, value);
    }

    @Override
    public String psetex(String key, long milliseconds, String value) {
        check();
        return template.psetex(key, milliseconds, value);
    }

    @Override
    public String set(String key, String value, String nxxx) {
        check();
        return template.set(key, value, nxxx);
    }

    @Override
    public String set(String key, String value, String nxxx, String expx, int time) {
        check();
        return template.set(key, value, nxxx, expx, time);
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, int cursor) {
        check();
        return template.hscan(key, String.valueOf(cursor));
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, int cursor, ScanParams params) {
        check();
        return template.hscan(key, String.valueOf(cursor), params);
    }

    @Override
    public ScanResult<String> sscan(String key, int cursor) {
        check();
        return template.sscan(key, String.valueOf(cursor));
    }

    @Override
    public ScanResult<String> sscan(String key, int cursor, ScanParams params) {
        check();
        return template.sscan(key, String.valueOf(cursor), params);
    }

    @Override
    public ScanResult<Tuple> zscan(String key, int cursor) {
        check();
        return template.zscan(key, String.valueOf(cursor));
    }

    @Override
    public ScanResult<Tuple> zscan(String key, int cursor, ScanParams params) {
        check();
        return template.zscan(key, String.valueOf(cursor), params);
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor) {
        check();
        return template.hscan(key, cursor);
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
        check();
        return template.hscan(key, cursor, params);
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor) {
        check();
        return template.sscan(key, cursor);
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
        check();
        return template.sscan(key, cursor, params);
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor) {
        check();
        return template.zscan(key, cursor);
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
        check();
        return template.zscan(key, cursor, params);
    }

    @Override
    public Long pfadd(String key, String... elements) {
        check();
        return template.pfadd(key, elements);
    }

    @Override
    public long pfcount(String key) {
        check();
        return template.pfcount(key);
    }

    @Override
    public Long geoadd(String key, double longitude, double latitude, String member) {
        check();
        return template.geoadd(key, longitude, latitude, member);
    }

    @Override
    public Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
        check();
        return template.geoadd(key, memberCoordinateMap);
    }

    @Override
    public Double geodist(String key, String member1, String member2) {
        check();
        return template.geodist(key, member1, member2);
    }

    @Override
    public Double geodist(String key, String member1, String member2, GeoUnit unit) {
        check();
        return template.geodist(key, member1, member2, unit);
    }

    @Override
    public List<String> geohash(String key, String... members) {
        check();
        return template.geohash(key, members);
    }

    @Override
    public List<GeoCoordinate> geopos(String key, String... members) {
        check();
        return template.geopos(key, members);
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
        check();
        return template.georadius(key, longitude, latitude, radius, unit);
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        check();
        return template.georadius(key, longitude, latitude, radius, unit, param);
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
        check();
        return template.georadiusByMember(key, member, radius, unit);
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
        check();
        return template.georadiusByMember(key, member, radius, unit, param);
    }

    @Override
    public List<Long> bitfield(String key, String... arguments) {
        check();
        return template.bitfield(key, arguments);
    }

    @Override
    public String set(byte[] key, byte[] value) {
        check();
        return template.set(key, value);
    }

    @Override
    public String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
        check();
        return template.set(key, value, nxxx, expx, time);
    }

    @Override
    public byte[] get(byte[] key) {
        check();
        return template.get(key);
    }

    @Override
    public Long exists(byte[]... keys) {
        check();
        return template.exists(keys);
    }

    @Override
    public Boolean exists(byte[] key) {
        check();
        return template.exists(key);
    }

    @Override
    public Long del(byte[]... keys) {
        check();
        return template.del(keys);
    }

    @Override
    public Long del(byte[] key) {
        check();
        return template.del(key);
    }

    @Override
    public String type(byte[] key) {
        check();
        return template.type(key);
    }

    @Override
    public Long expire(byte[] key, int seconds) {
        check();
        return template.expire(key, seconds);
    }

    @Override
    public Long expireAt(byte[] key, long unixTime) {
        check();
        return template.expireAt(key, unixTime);
    }

    @Override
    public Long ttl(byte[] key) {
        check();
        return template.ttl(key);
    }

    @Override
    public byte[] getSet(byte[] key, byte[] value) {
        check();
        return template.getSet(key, value);
    }

    @Override
    public List<byte[]> mget(byte[]... keys) {
        check();
        return template.mget(keys);
    }

    @Override
    public Long setnx(byte[] key, byte[] value) {
        check();
        return template.setnx(key, value);
    }

    @Override
    public String setex(byte[] key, int seconds, byte[] value) {
        check();
        return template.setex(key, seconds, value);
    }

    @Override
    public String mset(byte[]... keysvalues) {
        check();
        return template.mset(keysvalues);
    }

    @Override
    public Long decrBy(byte[] key, long decrement) {
        check();
        return template.decrBy(key, decrement);
    }

    @Override
    public Long decr(byte[] key) {
        check();
        return template.decr(key);
    }

    @Override
    public Long incrBy(byte[] key, long increment) {
        check();
        return template.incrBy(key, increment);
    }

    @Override
    public Double incrByFloat(byte[] key, double increment) {
        check();
        return template.incrByFloat(key, increment);
    }

    @Override
    public Long incr(byte[] key) {
        check();
        return template.incr(key);
    }

    @Override
    public Long append(byte[] key, byte[] value) {
        check();
        return template.append(key, value);
    }

    @Override
    public byte[] substr(byte[] key, int start, int end) {
        check();
        return template.substr(key, start, end);
    }

    @Override
    public Long hset(byte[] key, byte[] field, byte[] value) {
        check();
        return template.hset(key, field, value);
    }

    @Override
    public byte[] hget(byte[] key, byte[] field) {
        check();
        return template.hget(key, field);
    }

    @Override
    public Long hsetnx(byte[] key, byte[] field, byte[] value) {
        check();
        return template.hsetnx(key, field, value);
    }

    @Override
    public String hmset(byte[] key, Map<byte[], byte[]> hash) {
        check();
        return template.hmset(key, hash);
    }

    @Override
    public List<byte[]> hmget(byte[] key, byte[]... fields) {
        check();
        return template.hmget(key, fields);
    }

    @Override
    public Long hincrBy(byte[] key, byte[] field, long value) {
        check();
        return template.hincrBy(key, field, value);
    }

    @Override
    public Double hincrByFloat(byte[] key, byte[] field, double value) {
        check();
        return template.hincrByFloat(key, field, value);
    }

    @Override
    public Boolean hexists(byte[] key, byte[] field) {
        check();
        return template.hexists(key, field);
    }

    @Override
    public Long hdel(byte[] key, byte[]... fields) {
        check();
        return template.hdel(key, fields);
    }

    @Override
    public Long hlen(byte[] key) {
        check();
        return template.hlen(key);
    }

    @Override
    public Set<byte[]> hkeys(byte[] key) {
        check();
        return template.hkeys(key);
    }

    @Override
    public List<byte[]> hvals(byte[] key) {
        check();
        return template.hvals(key);
    }

    @Override
    public Map<byte[], byte[]> hgetAll(byte[] key) {
        check();
        return template.hgetAll(key);
    }

    @Override
    public Long rpush(byte[] key, byte[]... strings) {
        check();
        return template.rpush(key, strings);
    }

    @Override
    public Long lpush(byte[] key, byte[]... strings) {
        check();
        return template.lpush(key, strings);
    }

    @Override
    public Long llen(byte[] key) {
        check();
        return template.llen(key);
    }

    @Override
    public List<byte[]> lrange(byte[] key, long start, long stop) {
        check();
        return template.lrange(key, start, stop);
    }

    @Override
    public String ltrim(byte[] key, long start, long stop) {
        check();
        return template.ltrim(key, start, stop);
    }

    @Override
    public byte[] lindex(byte[] key, long index) {
        check();
        return template.lindex(key, index);
    }

    @Override
    public String lset(byte[] key, long index, byte[] value) {
        check();
        return template.lset(key, index, value);
    }

    @Override
    public Long lrem(byte[] key, long count, byte[] value) {
        check();
        return template.lrem(key, count, value);
    }

    @Override
    public byte[] lpop(byte[] key) {
        check();
        return template.lpop(key);
    }

    @Override
    public byte[] rpop(byte[] key) {
        check();
        return template.rpop(key);
    }

    @Override
    public Long sadd(byte[] key, byte[]... members) {
        check();
        return template.sadd(key, members);
    }

    @Override
    public Set<byte[]> smembers(byte[] key) {
        check();
        return template.smembers(key);
    }

    @Override
    public Long srem(byte[] key, byte[]... member) {
        check();
        return template.srem(key, member);
    }

    @Override
    public byte[] spop(byte[] key) {
        check();
        return template.spop(key);
    }

    @Override
    public Set<byte[]> spop(byte[] key, long count) {
        check();
        return template.spop(key, count);
    }

    @Override
    public Long scard(byte[] key) {
        check();
        return template.scard(key);
    }

    @Override
    public Boolean sismember(byte[] key, byte[] member) {
        check();
        return template.sismember(key, member);
    }

    @Override
    public byte[] srandmember(byte[] key) {
        check();
        return template.srandmember(key);
    }

    @Override
    public List<byte[]> srandmember(byte[] key, int count) {
        check();
        return template.srandmember(key, count);
    }

    @Override
    public Long zadd(byte[] key, double score, byte[] member) {
        check();
        return template.zadd(key, score, member);
    }

    @Override
    public Long zadd(byte[] key, double score, byte[] member, ZAddParams params) {
        check();
        return template.zadd(key, score, member, params);
    }

    @Override
    public Long zadd(byte[] key, Map<byte[], Double> scoreMembers) {
        check();
        return template.zadd(key, scoreMembers);
    }

    @Override
    public Long zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
        check();
        return template.zadd(key, scoreMembers, params);
    }

    @Override
    public Set<byte[]> zrange(byte[] key, long start, long stop) {
        check();
        return template.zrange(key, start, stop);
    }

    @Override
    public Long zrem(byte[] key, byte[]... members) {
        check();
        return template.zrem(key, members);
    }

    @Override
    public Double zincrby(byte[] key, double increment, byte[] member) {
        check();
        return template.zincrby(key, increment, member);
    }

    @Override
    public Double zincrby(byte[] key, double increment, byte[] member, ZIncrByParams params) {
        check();
        return template.zincrby(key, increment, member, params);
    }

    @Override
    public Long zrank(byte[] key, byte[] member) {
        check();
        return template.zrank(key, member);
    }

    @Override
    public Long zrevrank(byte[] key, byte[] member) {
        check();
        return template.zrevrank(key, member);
    }

    @Override
    public Set<byte[]> zrevrange(byte[] key, long start, long stop) {
        check();
        return template.zrevrange(key, start, stop);
    }

    @Override
    public Set<Tuple> zrangeWithScores(byte[] key, long start, long stop) {
        check();
        return template.zrangeWithScores(key, start, stop);
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(byte[] key, long start, long stop) {
        check();
        return template.zrevrangeWithScores(key, start, stop);
    }

    @Override
    public Long zcard(byte[] key) {
        check();
        return template.zcard(key);
    }

    @Override
    public Double zscore(byte[] key, byte[] member) {
        check();
        return template.zscore(key, member);
    }

    @Override
    public List<byte[]> sort(byte[] key) {
        check();
        return template.sort(key);
    }

    @Override
    public List<byte[]> sort(byte[] key, SortingParams sortingParameters) {
        check();
        return template.sort(key, sortingParameters);
    }

    @Override
    public Long zcount(byte[] key, double min, double max) {
        check();
        return template.zcount(key, min, max);
    }

    @Override
    public Long zcount(byte[] key, byte[] min, byte[] max) {
        check();
        return template.zcount(key, min, max);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
        check();
        return template.zrangeByScore(key, min, max);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
        check();
        return template.zrangeByScore(key, min, max);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
        check();
        return template.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
        check();
        return template.zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
        check();
        return template.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
        check();
        return template.zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
        check();
        return template.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
        check();
        return template.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
        check();
        return template.zrevrangeByScore(key, max, min);
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
        check();
        return template.zrevrangeByScore(key, max, min);
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
        check();
        return template.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
        check();
        return template.zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
        check();
        return template.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
        check();
        return template.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
        check();
        return template.zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
        check();
        return template.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Long zremrangeByRank(byte[] key, long start, long stop) {
        check();
        return template.zremrangeByRank(key, start, stop);
    }

    @Override
    public Long zremrangeByScore(byte[] key, double min, double max) {
        check();
        return template.zremrangeByScore(key, min, max);
    }

    @Override
    public Long zremrangeByScore(byte[] key, byte[] min, byte[] max) {
        check();
        return template.zremrangeByScore(key, min, max);
    }

    @Override
    public Long zlexcount(byte[] key, byte[] min, byte[] max) {
        check();
        return template.zlexcount(key, min, max);
    }

    @Override
    public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
        check();
        return template.zrangeByLex(key, min, max);
    }

    @Override
    public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
        check();
        return template.zrangeByLex(key, min, max, offset, count);
    }

    @Override
    public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
        check();
        return template.zrevrangeByLex(key, max, min);
    }

    @Override
    public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
        check();
        return template.zrevrangeByLex(key, max, min, offset, count);
    }

    @Override
    public Long zremrangeByLex(byte[] key, byte[] min, byte[] max) {
        check();
        return template.zremrangeByLex(key, min, max);
    }

    @Override
    public Long strlen(byte[] key) {
        check();
        return template.strlen(key);
    }

    @Override
    public Long lpushx(byte[] key, byte[]... string) {
        check();
        return template.lpushx(key, string);
    }

    @Override
    public Long persist(byte[] key) {
        check();
        return template.persist(key);
    }

    @Override
    public Long rpushx(byte[] key, byte[]... string) {
        check();
        return template.rpushx(key, string);
    }

    @Override
    public byte[] echo(byte[] string) {
        check();
        return string;
    }

    @Override
    public Long linsert(byte[] key, BinaryClient.LIST_POSITION where, byte[] pivot, byte[] value) {
        check();
        return template.linsert(key, where, pivot, value);
    }

    @Override
    public Boolean setbit(byte[] key, long offset, boolean value) {
        check();
        return template.setbit(key, offset, value);
    }

    @Override
    public Boolean setbit(byte[] key, long offset, byte[] value) {
        check();
        return template.setbit(key, offset, value);
    }

    @Override
    public Boolean getbit(byte[] key, long offset) {
        check();
        return template.getbit(key, offset);
    }

    @Override
    public Long bitpos(byte[] key, boolean value) {
        check();
        return template.bitpos(key, value);
    }

    @Override
    public Long bitpos(byte[] key, boolean value, BitPosParams params) {
        check();
        return template.bitpos(key, value, params);
    }

    @Override
    public Long setrange(byte[] key, long offset, byte[] value) {
        check();
        return template.setrange(key, offset, value);
    }

    @Override
    public byte[] getrange(byte[] key, long startOffset, long endOffset) {
        check();
        return template.getrange(key, startOffset, endOffset);
    }

    @Override
    public Object eval(byte[] script, List<byte[]> keys, List<byte[]> args) {
        check();
        return template.eval(script, keys, args);
    }

    @Override
    public Object eval(byte[] script, byte[] keyCount, byte[]... params) {
        check();
        return template.eval(script, keyCount, params);
    }

    @Override
    public Object eval(byte[] script, int keyCount, byte[]... params) {
        check();
        return template.eval(script, keyCount, params);
    }

    @Override
    public Object eval(byte[] script) {
        check();
        return template.eval(script);
    }

    @Override
    public Object evalsha(byte[] sha1) {
        check();
        return template.evalsha(sha1);
    }

    @Override
    public Object evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args) {
        check();
        return template.evalsha(sha1, keys, args);
    }

    @Override
    public Object evalsha(byte[] sha1, int keyCount, byte[]... params) {
        check();
        return template.evalsha(sha1, keyCount, params);
    }

    @Override
    public Long bitcount(byte[] key) {
        check();
        return template.bitcount(key);
    }

    @Override
    public Long bitcount(byte[] key, long start, long end) {
        check();
        return template.bitcount(key, start, end);
    }

    @Override
    public byte[] dump(byte[] key) {
        check();
        return template.dump(key);
    }

    @Override
    public String restore(byte[] key, int ttl, byte[] serializedValue) {
        check();
        return template.restore(key, ttl, serializedValue);
    }

    @Override
    public Long pexpire(byte[] key, int milliseconds) {
        check();
        return template.pexpire(key, milliseconds);
    }

    @Override
    public Long pexpire(byte[] key, long milliseconds) {
        check();
        return template.pexpire(key, milliseconds);
    }

    @Override
    public Long pexpireAt(byte[] key, long millisecondsTimestamp) {
        check();
        return template.pexpireAt(key, millisecondsTimestamp);
    }

    @Override
    public Long pttl(byte[] key) {
        check();
        return template.pttl(key);
    }

    @Override
    public String psetex(byte[] key, int milliseconds, byte[] value) {
        check();
        return template.psetex(key, milliseconds, value);
    }

    @Override
    public String psetex(byte[] key, long milliseconds, byte[] value) {
        check();
        return template.psetex(key, milliseconds, value);
    }

    @Override
    public String set(byte[] key, byte[] value, byte[] nxxx) {
        check();
        return template.set(key, value, nxxx);
    }

    @Override
    public String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, int time) {
        check();
        return template.set(key, value, nxxx, expx, time);
    }

    @Override
    public Long pfadd(byte[] key, byte[]... elements) {
        check();
        return template.pfadd(key, elements);
    }

    @Override
    public long pfcount(byte[] key) {
        check();
        return template.pfcount(key);
    }

    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor) {
        check();
        return template.hscan(key, cursor);
    }

    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor, ScanParams params) {
        check();
        return template.hscan(key, cursor, params);
    }

    @Override
    public ScanResult<byte[]> sscan(byte[] key, byte[] cursor) {
        check();
        return template.sscan(key, cursor);
    }

    @Override
    public ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams params) {
        check();
        return template.sscan(key, cursor, params);
    }

    @Override
    public ScanResult<Tuple> zscan(byte[] key, byte[] cursor) {
        check();
        return template.zscan(key, cursor);
    }

    @Override
    public ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams params) {
        check();
        return template.zscan(key, cursor, params);
    }

    @Override
    public Long geoadd(byte[] key, double longitude, double latitude, byte[] member) {
        check();
        return template.geoadd(key, longitude, latitude, member);
    }

    @Override
    public Long geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
        check();
        return template.geoadd(key, memberCoordinateMap);
    }

    @Override
    public Double geodist(byte[] key, byte[] member1, byte[] member2) {
        check();
        return template.geodist(key, member1, member2);
    }

    @Override
    public Double geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
        check();
        return template.geodist(key, member1, member2, unit);
    }

    @Override
    public List<byte[]> geohash(byte[] key, byte[]... members) {
        check();
        return template.geohash(key, members);
    }

    @Override
    public List<GeoCoordinate> geopos(byte[] key, byte[]... members) {
        check();
        return template.geopos(key, members);
    }

    @Override
    public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
        check();
        return template.georadius(key, longitude, latitude, radius, unit);
    }

    @Override
    public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        check();
        return template.georadius(key, longitude, latitude, radius, unit, param);
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit) {
        check();
        return template.georadiusByMember(key, member, radius, unit);
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
        check();
        return template.georadiusByMember(key, member, radius, unit, param);
    }

    @Override
    public List<Long> bitfield(byte[] key, byte[]... arguments) {
        check();
        return template.bitfield(key, arguments);
    }

    @Override
    public byte[] rpoplpush(byte[] srckey, byte[] dstkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String flushDB() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Set<byte[]> keys(byte[] pattern) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public byte[] randomBinaryKey() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String rename(byte[] oldkey, byte[] newkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long renamenx(byte[] oldkey, byte[] newkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long dbSize() {
        throw new UnsupportedOperationException("not support");
    }


    @Override
    public String select(int index) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long move(byte[] key, int dbIndex) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String flushAll() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long msetnx(byte[]... keysvalues) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Transaction multi() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<Object> multi(TransactionBlock jedisTransaction) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    protected void checkIsInMultiOrPipeline() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String watch(byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String unwatch() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Set<String> keys(String pattern) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String randomKey() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String rename(String oldkey, String newkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long renamenx(String oldkey, String newkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long move(String key, int dbIndex) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String pfmerge(byte[] destkey, byte[]... sourcekeys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long pfcount(byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public ScanResult<byte[]> scan(byte[] cursor) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public ScanResult<byte[]> scan(byte[] cursor, ScanParams params) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clientKill(byte[] client) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clientGetname() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clientList() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clientSetname(byte[] name) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<String> time() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String migrate(byte[] host, int port, byte[] key, int destinationDb, int timeout) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long waitReplicas(int replicas, long timeout) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long bitop(BitOP op, byte[] destKey, byte[]... srcKeys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String scriptFlush() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long scriptExists(byte[] sha1) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<Long> scriptExists(byte[]... sha1) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public byte[] scriptLoad(byte[] script) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String scriptKill() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String slowlogReset() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long slowlogLen() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<byte[]> slowlogGetBinary() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<byte[]> slowlogGetBinary(long entries) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long objectRefcount(byte[] key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public byte[] objectEncoding(byte[] key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long objectIdletime(byte[] key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long publish(byte[] channel, byte[] message) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public void subscribe(BinaryJedisPubSub jedisPubSub, byte[]... channels) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public void sync() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public void psubscribe(BinaryJedisPubSub jedisPubSub, byte[]... patterns) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long getDB() {
        return 0L;
    }

    @Override
    public String debug(DebugParams params) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Client getClient() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public byte[] brpoplpush(byte[] source, byte[] destination, int timeout) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long msetnx(String... keysvalues) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String rpoplpush(String srckey, String dstkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long smove(String srckey, String dstkey, String member) {
        throw new UnsupportedOperationException("not support");
    }


    @Override
    public Set<String> sinter(String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long sinterstore(String dstkey, String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Set<String> sunion(String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long sunionstore(String dstkey, String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Set<String> sdiff(String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long sdiffstore(String dstkey, String... keys) {
        throw new UnsupportedOperationException("not support");
    }


    @Override
    public String watch(String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<String> blpop(int timeout, String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<String> blpop(String... args) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<String> brpop(String... args) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<String> blpop(String arg) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<String> brpop(String arg) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long sort(String key, SortingParams sortingParameters, String dstkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long sort(String key, String dstkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<String> brpop(int timeout, String... keys) {
        throw new UnsupportedOperationException("not support");
    }


    @Override
    public Long zunionstore(byte[] dstkey, byte[]... sets) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long zunionstore(byte[] dstkey, ZParams params, byte[]... sets) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long zinterstore(byte[] dstkey, byte[]... sets) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long zinterstore(byte[] dstkey, ZParams params, byte[]... sets) {
        throw new UnsupportedOperationException("not support");
    }


    @Override
    public Long zunionstore(String dstkey, String... sets) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long zunionstore(String dstkey, ZParams params, String... sets) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long zinterstore(String dstkey, String... sets) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long zinterstore(String dstkey, ZParams params, String... sets) {
        throw new UnsupportedOperationException("not support");
    }


    @Override
    public String brpoplpush(String source, String destination, int timeout) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<String> configGet(String pattern) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String configSet(String parameter, String value) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public void subscribe(JedisPubSub jedisPubSub, String... channels) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long publish(String channel, String message) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public void psubscribe(JedisPubSub jedisPubSub, String... patterns) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Boolean scriptExists(String sha1) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<Boolean> scriptExists(String... sha1) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String scriptLoad(String script) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<Slowlog> slowlogGet() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<Slowlog> slowlogGet(long entries) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long objectRefcount(String key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String objectEncoding(String key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long objectIdletime(String key) {
        throw new UnsupportedOperationException("not support");
    }


    @Override
    public Long bitop(BitOP op, String destKey, String... srcKeys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<Map<String, String>> sentinelMasters() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<String> sentinelGetMasterAddrByName(String masterName) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long sentinelReset(String pattern) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<Map<String, String>> sentinelSlaves(String masterName) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String sentinelFailover(String masterName) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String sentinelMonitor(String masterName, String ip, int port, int quorum) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String sentinelRemove(String masterName) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String sentinelSet(String masterName, Map<String, String> parameterMap) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clientKill(String client) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clientSetname(String name) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String migrate(String host, int port, String key, int destinationDb, int timeout) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public ScanResult<String> scan(int cursor) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public ScanResult<String> scan(int cursor, ScanParams params) {
        throw new UnsupportedOperationException("not support");
    }


    @Override
    public ScanResult<String> scan(String cursor) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public ScanResult<String> scan(String cursor, ScanParams params) {
        throw new UnsupportedOperationException("not support");
    }


    @Override
    public long pfcount(String... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String pfmerge(String destkey, String... sourcekeys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<String> blpop(int timeout, String key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<String> brpop(int timeout, String key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String save() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String bgsave() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String bgrewriteaof() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long lastsave() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String shutdown() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String info() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String info(String section) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public void monitor(JedisMonitor jedisMonitor) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String slaveof(String host, int port) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String slaveofNoOne() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<byte[]> configGet(byte[] pattern) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String configResetStat() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public byte[] configSet(byte[] parameter, byte[] value) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    protected Set<Tuple> getTupledSet() {
        throw new UnsupportedOperationException("not support");
    }


    @Override
    public Long smove(byte[] srckey, byte[] dstkey, byte[] member) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Set<byte[]> sinter(byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long sinterstore(byte[] dstkey, byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Set<byte[]> sunion(byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long sunionstore(byte[] dstkey, byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Set<byte[]> sdiff(byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long sdiffstore(byte[] dstkey, byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }


    @Override
    public List<byte[]> blpop(int timeout, byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long sort(byte[] key, SortingParams sortingParameters, byte[] dstkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long sort(byte[] key, byte[] dstkey) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<byte[]> brpop(int timeout, byte[]... keys) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<byte[]> blpop(byte[] arg) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<byte[]> brpop(byte[] arg) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<byte[]> blpop(byte[]... args) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<byte[]> brpop(byte[]... args) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String auth(String password) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<Object> pipelined(PipelineBlock jedisPipeline) {
        throw new UnsupportedOperationException("not support");
    }



    @Override
    public String clusterNodes() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String readonly() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clusterMeet(String ip, int port) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clusterReset(JedisCluster.Reset resetType) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clusterAddSlots(int... slots) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clusterDelSlots(int... slots) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clusterInfo() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<String> clusterGetKeysInSlot(int slot, int count) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clusterSetSlotNode(int slot, String nodeId) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clusterSetSlotMigrating(int slot, String nodeId) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clusterSetSlotImporting(int slot, String nodeId) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clusterSetSlotStable(int slot) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clusterForget(String nodeId) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clusterFlushSlots() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long clusterKeySlot(String key) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long clusterCountKeysInSlot(int slot) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clusterSaveConfig() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clusterReplicate(String nodeId) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<String> clusterSlaves(String nodeId) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String clusterFailover() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<Object> clusterSlots() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public String asking() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<String> pubsubChannels(String pattern) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Long pubsubNumPat() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public Map<String, String> pubsubNumSub(String... channels) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public void setDataSource(Pool<Jedis> jedisPool) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public void connect() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public void disconnect() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public void resetState() {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public boolean isConnected() {
        throw new UnsupportedOperationException("not support");
    }
}
