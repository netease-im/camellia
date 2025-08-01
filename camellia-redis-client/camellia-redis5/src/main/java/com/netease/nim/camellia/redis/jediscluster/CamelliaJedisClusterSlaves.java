package com.netease.nim.camellia.redis.jediscluster;

import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.ICamelliaRedis;
import com.netease.nim.camellia.redis.base.resource.RedisClusterResource;
import com.netease.nim.camellia.redis.base.resource.RedisClusterSlavesResource;
import redis.clients.jedis.*;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.GeoRadiusResponse;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by caojiajun on 2024/11/11
 */
public class CamelliaJedisClusterSlaves implements ICamelliaRedis {

    private final ClusterCommandObjects clusterCommandObjects = new ClusterCommandObjects();

    private final JedisClusterWrapper jedisCluster;
    private final JedisClusterWrapper jedisClusterSlaves;

    private final RedisClusterSlavesResource resource;

    public CamelliaJedisClusterSlaves(RedisClusterSlavesResource resource, CamelliaRedisEnv env) {
        RedisClusterResource redisClusterResource = new RedisClusterResource(resource.getNodes(), resource.getUserName(), resource.getPassword());
        this.jedisClusterSlaves = env.getJedisClusterFactory().getJedisCluster(redisClusterResource);
        this.jedisCluster = env.getJedisClusterFactory().getJedisCluster(resource);
        this.resource = resource;
    }

    @Override
    public Jedis getJedis(byte[] key) {
        NodeType nodeType = selectNodeType();
        if (nodeType == NodeType.slave) {
            return jedisClusterSlaves.getSlaveJedis(key);
        } else {
            return jedisCluster.getJedis(key);
        }
    }

    @Override
    public List<Jedis> getJedisList() {
        List<Jedis> list = new ArrayList<>();
        if (resource.isWithMaster()) {
            List<Jedis> jedisList = jedisCluster.getJedisList();
            list.addAll(jedisList);
        }
        List<Jedis> jedisList = jedisClusterSlaves.getSlaveJedisList();
        list.addAll(jedisList);
        return list;
    }

    private static enum NodeType {
        master,
        slave,
        ;
    }

    private NodeType selectNodeType() {
        if (resource.isWithMaster()) {
            boolean slave = ThreadLocalRandom.current().nextBoolean();
            if (slave) {
                return NodeType.slave;
            } else {
                return NodeType.master;
            }
        }
        return NodeType.slave;
    }

    @Override
    public byte[] get(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.get(key));
        } else {
            return jedisCluster.get(key);
        }
    }

    @Override
    public Boolean exists(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.exists(key));
        } else {
            return jedisCluster.exists(key);
        }
    }

    @Override
    public String type(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.type(key));
        } else {
            return jedisCluster.type(key);
        }
    }

    @Override
    public Long ttl(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.ttl(key));
        } else {
            return jedisCluster.ttl(key);
        }
    }

    @Override
    public Long pttl(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.pttl(key));
        } else {
            return jedisCluster.pttl(key);
        }
    }

    @Override
    public Boolean getbit(byte[] key, long offset) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.getbit(key, offset));
        } else {
            return jedisCluster.getbit(key, offset);
        }
    }

    @Override
    public byte[] getrange(byte[] key, long startOffset, long endOffset) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.getrange(key, startOffset, endOffset));
        } else {
            return jedisCluster.getrange(key, startOffset, endOffset);
        }
    }

    @Override
    public byte[] hget(byte[] key, byte[] field) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hget(key, field));
        } else {
            return jedisCluster.hget(key, field);
        }
    }

    @Override
    public List<byte[]> hmget(byte[] key, byte[]... fields) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hmget(key, fields));
        } else {
            return jedisCluster.hmget(key, fields);
        }
    }

    @Override
    public String set(byte[] key, byte[] value) {
        return "";
    }

    @Override
    public String set(byte[] key, byte[] value, SetParams setParams) {
        return "";
    }

    @Override
    public String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
        return "";
    }

    @Override
    public Long persist(byte[] key) {
        return 0L;
    }

    @Override
    public Long expire(byte[] key, int seconds) {
        return 0L;
    }

    @Override
    public Long pexpire(byte[] key, long milliseconds) {
        return 0L;
    }

    @Override
    public Long expireAt(byte[] key, long unixTime) {
        return 0L;
    }

    @Override
    public Long pexpireAt(byte[] key, long millisecondsTimestamp) {
        return 0L;
    }

    @Override
    public Boolean setbit(byte[] key, long offset, boolean value) {
        return null;
    }

    @Override
    public Long setrange(byte[] key, long offset, byte[] value) {
        return 0L;
    }

    @Override
    public byte[] getSet(byte[] key, byte[] value) {
        return new byte[0];
    }

    @Override
    public Long setnx(byte[] key, byte[] value) {
        return 0L;
    }

    @Override
    public String setex(byte[] key, int seconds, byte[] value) {
        return "";
    }

    @Override
    public String psetex(byte[] key, long milliseconds, byte[] value) {
        return "";
    }

    @Override
    public Long decrBy(byte[] key, long integer) {
        return 0L;
    }

    @Override
    public Long decr(byte[] key) {
        return 0L;
    }

    @Override
    public Long incrBy(byte[] key, long integer) {
        return 0L;
    }

    @Override
    public Double incrByFloat(byte[] key, double value) {
        return 0.0;
    }

    @Override
    public Long incr(byte[] key) {
        return 0L;
    }

    @Override
    public Long append(byte[] key, byte[] value) {
        return 0L;
    }

    @Override
    public byte[] substr(byte[] key, int start, int end) {
        return new byte[0];
    }

    @Override
    public Long hset(byte[] key, byte[] field, byte[] value) {
        return 0L;
    }

    @Override
    public Long hsetnx(byte[] key, byte[] field, byte[] value) {
        return 0L;
    }

    @Override
    public String hmset(byte[] key, Map<byte[], byte[]> hash) {
        return "";
    }

    @Override
    public Long hincrBy(byte[] key, byte[] field, long value) {
        return 0L;
    }

    @Override
    public Double hincrByFloat(byte[] key, byte[] field, double value) {
        return 0.0;
    }

    @Override
    public Boolean hexists(byte[] key, byte[] field) {
        return null;
    }

    @Override
    public Long hdel(byte[] key, byte[]... field) {
        return 0L;
    }

    @Override
    public Long hlen(byte[] key) {
        return 0L;
    }

    @Override
    public Set<byte[]> hkeys(byte[] key) {
        return Set.of();
    }

    @Override
    public List<byte[]> hvals(byte[] key) {
        return List.of();
    }

    @Override
    public Map<byte[], byte[]> hgetAll(byte[] key) {
        return Map.of();
    }

    @Override
    public Long rpush(byte[] key, byte[]... args) {
        return 0L;
    }

    @Override
    public Long lpush(byte[] key, byte[]... args) {
        return 0L;
    }

    @Override
    public Long llen(byte[] key) {
        return 0L;
    }

    @Override
    public List<byte[]> lrange(byte[] key, long start, long end) {
        return List.of();
    }

    @Override
    public String ltrim(byte[] key, long start, long end) {
        return "";
    }

    @Override
    public byte[] lindex(byte[] key, long index) {
        return new byte[0];
    }

    @Override
    public String lset(byte[] key, long index, byte[] value) {
        return "";
    }

    @Override
    public Long lrem(byte[] key, long count, byte[] value) {
        return 0L;
    }

    @Override
    public byte[] lpop(byte[] key) {
        return new byte[0];
    }

    @Override
    public byte[] rpop(byte[] key) {
        return new byte[0];
    }

    @Override
    public Long sadd(byte[] key, byte[]... member) {
        return 0L;
    }

    @Override
    public Set<byte[]> smembers(byte[] key) {
        return Set.of();
    }

    @Override
    public Long srem(byte[] key, byte[]... member) {
        return 0L;
    }

    @Override
    public byte[] spop(byte[] key) {
        return new byte[0];
    }

    @Override
    public Set<byte[]> spop(byte[] key, long count) {
        return Set.of();
    }

    @Override
    public Long scard(byte[] key) {
        return 0L;
    }

    @Override
    public Boolean sismember(byte[] key, byte[] member) {
        return null;
    }

    @Override
    public byte[] srandmember(byte[] key) {
        return new byte[0];
    }

    @Override
    public List<byte[]> srandmember(byte[] key, int count) {
        return List.of();
    }

    @Override
    public Long strlen(byte[] key) {
        return 0L;
    }

    @Override
    public Long zadd(byte[] key, double score, byte[] member) {
        return 0L;
    }

    @Override
    public Long zadd(byte[] key, double score, byte[] member, ZAddParams params) {
        return 0L;
    }

    @Override
    public Long zadd(byte[] key, Map<byte[], Double> scoreMembers) {
        return 0L;
    }

    @Override
    public Long zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
        return 0L;
    }

    @Override
    public List<byte[]> zrange(byte[] key, long start, long end) {
        return List.of();
    }

    @Override
    public Long zrem(byte[] key, byte[]... member) {
        return 0L;
    }

    @Override
    public Double zincrby(byte[] key, double score, byte[] member) {
        return 0.0;
    }

    @Override
    public Double zincrby(byte[] key, double score, byte[] member, ZIncrByParams params) {
        return 0.0;
    }

    @Override
    public Long zrank(byte[] key, byte[] member) {
        return 0L;
    }

    @Override
    public Long zrevrank(byte[] key, byte[] member) {
        return 0L;
    }

    @Override
    public List<byte[]> zrevrange(byte[] key, long start, long end) {
        return List.of();
    }

    @Override
    public List<Tuple> zrangeWithScores(byte[] key, long start, long end) {
        return List.of();
    }

    @Override
    public List<Tuple> zrevrangeWithScores(byte[] key, long start, long end) {
        return List.of();
    }

    @Override
    public Long zcard(byte[] key) {
        return 0L;
    }

    @Override
    public Double zscore(byte[] key, byte[] member) {
        return 0.0;
    }

    @Override
    public List<Double> zmscore(String key, String... members) {
        return List.of();
    }

    @Override
    public List<Double> zmscore(byte[] key, byte[]... members) {
        return List.of();
    }

    @Override
    public List<byte[]> sort(byte[] key) {
        return List.of();
    }

    @Override
    public List<byte[]> sort(byte[] key, SortingParams sortingParameters) {
        return List.of();
    }

    @Override
    public Long zcount(byte[] key, double min, double max) {
        return 0L;
    }

    @Override
    public Long zcount(byte[] key, byte[] min, byte[] max) {
        return 0L;
    }

    @Override
    public List<byte[]> zrangeByScore(byte[] key, double min, double max) {
        return List.of();
    }

    @Override
    public List<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
        return List.of();
    }

    @Override
    public List<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
        return List.of();
    }

    @Override
    public List<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
        return List.of();
    }

    @Override
    public List<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
        return List.of();
    }

    @Override
    public List<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return List.of();
    }

    @Override
    public List<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
        return List.of();
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
        return List.of();
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
        return List.of();
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
        return List.of();
    }

    @Override
    public List<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return List.of();
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
        return List.of();
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
        return List.of();
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return List.of();
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
        return List.of();
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return List.of();
    }

    @Override
    public Long zremrangeByRank(byte[] key, long start, long end) {
        return 0L;
    }

    @Override
    public Long zremrangeByScore(byte[] key, double start, double end) {
        return 0L;
    }

    @Override
    public Long zremrangeByScore(byte[] key, byte[] start, byte[] end) {
        return 0L;
    }

    @Override
    public Long zlexcount(byte[] key, byte[] min, byte[] max) {
        return 0L;
    }

    @Override
    public List<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
        return List.of();
    }

    @Override
    public List<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return List.of();
    }

    @Override
    public List<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
        return List.of();
    }

    @Override
    public List<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return List.of();
    }

    @Override
    public Long zremrangeByLex(byte[] key, byte[] min, byte[] max) {
        return 0L;
    }

    @Override
    public Long lpushx(byte[] key, byte[]... arg) {
        return 0L;
    }

    @Override
    public Long rpushx(byte[] key, byte[]... arg) {
        return 0L;
    }

    @Override
    public Long del(byte[] key) {
        return 0L;
    }

    @Override
    public Long bitcount(byte[] key) {
        return 0L;
    }

    @Override
    public Long bitcount(byte[] key, long start, long end) {
        return 0L;
    }

    @Override
    public Long pfadd(byte[] key, byte[]... elements) {
        return 0L;
    }

    @Override
    public long pfcount(byte[] key) {
        return 0;
    }

    @Override
    public Long geoadd(byte[] key, double longitude, double latitude, byte[] member) {
        return 0L;
    }

    @Override
    public Long geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
        return 0L;
    }

    @Override
    public Double geodist(byte[] key, byte[] member1, byte[] member2) {
        return 0.0;
    }

    @Override
    public Double geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
        return 0.0;
    }

    @Override
    public List<byte[]> geohash(byte[] key, byte[]... members) {
        return List.of();
    }

    @Override
    public List<GeoCoordinate> geopos(byte[] key, byte[]... members) {
        return List.of();
    }

    @Override
    public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
        return List.of();
    }

    @Override
    public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        return List.of();
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit) {
        return List.of();
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
        return List.of();
    }

    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor) {
        return null;
    }

    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor, ScanParams params) {
        return null;
    }

    @Override
    public ScanResult<byte[]> sscan(byte[] key, byte[] cursor) {
        return null;
    }

    @Override
    public ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams params) {
        return null;
    }

    @Override
    public ScanResult<Tuple> zscan(byte[] key, byte[] cursor) {
        return null;
    }

    @Override
    public ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams params) {
        return null;
    }

    @Override
    public List<Long> bitfield(byte[] key, byte[]... arguments) {
        return List.of();
    }

    @Override
    public String set(String key, String value) {
        return "";
    }

    @Override
    public String set(String key, String value, SetParams setParams) {
        return "";
    }

    @Override
    public String set(String key, String value, String nxxx, String expx, long time) {
        return "";
    }

    @Override
    public String set(String key, String value, String nxxx) {
        return "";
    }

    @Override
    public String set(byte[] key, byte[] value, byte[] nxxx) {
        return "";
    }

    @Override
    public String get(String key) {
        return "";
    }

    @Override
    public Boolean exists(String key) {
        return null;
    }

    @Override
    public Long persist(String key) {
        return 0L;
    }

    @Override
    public String type(String key) {
        return "";
    }

    @Override
    public Long expire(String key, int seconds) {
        return 0L;
    }

    @Override
    public Long pexpire(String key, long milliseconds) {
        return 0L;
    }

    @Override
    public Long expireAt(String key, long unixTime) {
        return 0L;
    }

    @Override
    public Long pexpireAt(String key, long millisecondsTimestamp) {
        return 0L;
    }

    @Override
    public Long ttl(String key) {
        return 0L;
    }

    @Override
    public Long pttl(String key) {
        return 0L;
    }

    @Override
    public Boolean setbit(String key, long offset, boolean value) {
        return null;
    }

    @Override
    public Boolean getbit(String key, long offset) {
        return null;
    }

    @Override
    public Long setrange(String key, long offset, String value) {
        return 0L;
    }

    @Override
    public String getrange(String key, long startOffset, long endOffset) {
        return "";
    }

    @Override
    public String getSet(String key, String value) {
        return "";
    }

    @Override
    public Long setnx(String key, String value) {
        return 0L;
    }

    @Override
    public String setex(String key, int seconds, String value) {
        return "";
    }

    @Override
    public String psetex(String key, long milliseconds, String value) {
        return "";
    }

    @Override
    public Long decrBy(String key, long integer) {
        return 0L;
    }

    @Override
    public Long decr(String key) {
        return 0L;
    }

    @Override
    public Long incrBy(String key, long integer) {
        return 0L;
    }

    @Override
    public Double incrByFloat(String key, double value) {
        return 0.0;
    }

    @Override
    public Long incr(String key) {
        return 0L;
    }

    @Override
    public Long append(String key, String value) {
        return 0L;
    }

    @Override
    public String substr(String key, int start, int end) {
        return "";
    }

    @Override
    public Long hset(String key, String field, String value) {
        return 0L;
    }

    @Override
    public String hget(String key, String field) {
        return "";
    }

    @Override
    public Long hsetnx(String key, String field, String value) {
        return 0L;
    }

    @Override
    public String hmset(String key, Map<String, String> hash) {
        return "";
    }

    @Override
    public List<String> hmget(String key, String... fields) {
        return List.of();
    }

    @Override
    public Long hincrBy(String key, String field, long value) {
        return 0L;
    }

    @Override
    public Double hincrByFloat(String key, String field, double value) {
        return 0.0;
    }

    @Override
    public Boolean hexists(String key, String field) {
        return null;
    }

    @Override
    public Long hdel(String key, String... field) {
        return 0L;
    }

    @Override
    public Long hlen(String key) {
        return 0L;
    }

    @Override
    public Set<String> hkeys(String key) {
        return Set.of();
    }

    @Override
    public List<String> hvals(String key) {
        return List.of();
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        return Map.of();
    }

    @Override
    public Long rpush(String key, String... string) {
        return 0L;
    }

    @Override
    public Long lpush(String key, String... string) {
        return 0L;
    }

    @Override
    public Long llen(String key) {
        return 0L;
    }

    @Override
    public Long linsert(String key, ListPosition where, String pivot, String value) {
        return 0L;
    }

    @Override
    public Long linsert(byte[] key, ListPosition where, byte[] pivot, byte[] value) {
        return 0L;
    }

    @Override
    public List<String> lrange(String key, long start, long end) {
        return List.of();
    }

    @Override
    public String ltrim(String key, long start, long end) {
        return "";
    }

    @Override
    public String lindex(String key, long index) {
        return "";
    }

    @Override
    public String lset(String key, long index, String value) {
        return "";
    }

    @Override
    public Long lrem(String key, long count, String value) {
        return 0L;
    }

    @Override
    public String lpop(String key) {
        return "";
    }

    @Override
    public String rpop(String key) {
        return "";
    }

    @Override
    public Long sadd(String key, String... member) {
        return 0L;
    }

    @Override
    public Set<String> smembers(String key) {
        return Set.of();
    }

    @Override
    public Long srem(String key, String... member) {
        return 0L;
    }

    @Override
    public String spop(String key) {
        return "";
    }

    @Override
    public Set<String> spop(String key, long count) {
        return Set.of();
    }

    @Override
    public Long scard(String key) {
        return 0L;
    }

    @Override
    public Boolean sismember(String key, String member) {
        return null;
    }

    @Override
    public List<Boolean> smismember(String key, String... members) {
        return List.of();
    }

    @Override
    public List<Boolean> smismember(byte[] key, byte[]... members) {
        return List.of();
    }

    @Override
    public String srandmember(String key) {
        return "";
    }

    @Override
    public List<String> srandmember(String key, int count) {
        return List.of();
    }

    @Override
    public Long strlen(String key) {
        return 0L;
    }

    @Override
    public Long zadd(String key, double score, String member) {
        return 0L;
    }

    @Override
    public Long zadd(String key, double score, String member, ZAddParams params) {
        return 0L;
    }

    @Override
    public Long zadd(String key, Map<String, Double> scoreMembers) {
        return 0L;
    }

    @Override
    public Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
        return 0L;
    }

    @Override
    public List<String> zrange(String key, long start, long end) {
        return List.of();
    }

    @Override
    public Long zrem(String key, String... member) {
        return 0L;
    }

    @Override
    public Double zincrby(String key, double score, String member) {
        return 0.0;
    }

    @Override
    public Double zincrby(String key, double score, String member, ZIncrByParams params) {
        return 0.0;
    }

    @Override
    public Long zrank(String key, String member) {
        return 0L;
    }

    @Override
    public Long zrevrank(String key, String member) {
        return 0L;
    }

    @Override
    public List<String> zrevrange(String key, long start, long end) {
        return List.of();
    }

    @Override
    public List<Tuple> zrangeWithScores(String key, long start, long end) {
        return List.of();
    }

    @Override
    public List<Tuple> zrevrangeWithScores(String key, long start, long end) {
        return List.of();
    }

    @Override
    public Long zcard(String key) {
        return 0L;
    }

    @Override
    public Double zscore(String key, String member) {
        return 0.0;
    }

    @Override
    public List<String> sort(String key) {
        return List.of();
    }

    @Override
    public List<String> sort(String key, SortingParams sortingParameters) {
        return List.of();
    }

    @Override
    public Long zcount(String key, double min, double max) {
        return 0L;
    }

    @Override
    public Long zcount(String key, String min, String max) {
        return 0L;
    }

    @Override
    public List<String> zrangeByScore(String key, double min, double max) {
        return List.of();
    }

    @Override
    public List<String> zrangeByScore(String key, String min, String max) {
        return List.of();
    }

    @Override
    public List<String> zrevrangeByScore(String key, double max, double min) {
        return List.of();
    }

    @Override
    public List<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        return List.of();
    }

    @Override
    public List<String> zrevrangeByScore(String key, String max, String min) {
        return List.of();
    }

    @Override
    public List<String> zrangeByScore(String key, String min, String max, int offset, int count) {
        return List.of();
    }

    @Override
    public List<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        return List.of();
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        return List.of();
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
        return List.of();
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        return List.of();
    }

    @Override
    public List<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
        return List.of();
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
        return List.of();
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
        return List.of();
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
        return List.of();
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        return List.of();
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
        return List.of();
    }

    @Override
    public Long zremrangeByRank(String key, long start, long end) {
        return 0L;
    }

    @Override
    public Long zremrangeByScore(String key, double start, double end) {
        return 0L;
    }

    @Override
    public Long zremrangeByScore(String key, String start, String end) {
        return 0L;
    }

    @Override
    public Long zlexcount(String key, String min, String max) {
        return 0L;
    }

    @Override
    public List<String> zrangeByLex(String key, String min, String max) {
        return List.of();
    }

    @Override
    public List<String> zrangeByLex(String key, String min, String max, int offset, int count) {
        return List.of();
    }

    @Override
    public List<String> zrevrangeByLex(String key, String max, String min) {
        return List.of();
    }

    @Override
    public List<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
        return List.of();
    }

    @Override
    public Long zremrangeByLex(String key, String min, String max) {
        return 0L;
    }

    @Override
    public Long lpushx(String key, String... string) {
        return 0L;
    }

    @Override
    public Long rpushx(String key, String... string) {
        return 0L;
    }

    @Override
    public Long del(String key) {
        return 0L;
    }

    @Override
    public Long bitcount(String key) {
        return 0L;
    }

    @Override
    public Long bitcount(String key, long start, long end) {
        return 0L;
    }

    @Override
    public Long bitpos(String key, boolean value) {
        return 0L;
    }

    @Override
    public Long bitpos(byte[] key, boolean value) {
        return 0L;
    }

    @Override
    public Long bitpos(String key, boolean value, BitPosParams params) {
        return 0L;
    }

    @Override
    public Long bitpos(byte[] key, boolean value, BitPosParams params) {
        return 0L;
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor) {
        return null;
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
        return null;
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor) {
        return null;
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
        return null;
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor) {
        return null;
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
        return null;
    }

    @Override
    public Long pfadd(String key, String... elements) {
        return 0L;
    }

    @Override
    public long pfcount(String key) {
        return 0;
    }

    @Override
    public Long geoadd(String key, double longitude, double latitude, String member) {
        return 0L;
    }

    @Override
    public Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
        return 0L;
    }

    @Override
    public Double geodist(String key, String member1, String member2) {
        return 0.0;
    }

    @Override
    public Double geodist(String key, String member1, String member2, GeoUnit unit) {
        return 0.0;
    }

    @Override
    public List<String> geohash(String key, String... members) {
        return List.of();
    }

    @Override
    public List<GeoCoordinate> geopos(String key, String... members) {
        return List.of();
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
        return List.of();
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        return List.of();
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
        return List.of();
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
        return List.of();
    }

    @Override
    public List<Long> bitfield(String key, String... arguments) {
        return List.of();
    }

    @Override
    public Long del(byte[]... keys) {
        return 0L;
    }

    @Override
    public Long exists(byte[]... keys) {
        return 0L;
    }

    @Override
    public Map<byte[], byte[]> mget(byte[]... keys) {
        return Map.of();
    }

    @Override
    public String mset(Map<byte[], byte[]> keysvalues) {
        return "";
    }

    @Override
    public Long del(String... keys) {
        return 0L;
    }

    @Override
    public Long exists(String... keys) {
        return 0L;
    }

    @Override
    public Map<String, String> mget(String... keys) {
        return Map.of();
    }

    @Override
    public Object eval(byte[] script, int keyCount, byte[]... params) {
        return null;
    }

    @Override
    public Object evalsha(byte[] sha1, int keyCount, byte[]... params) {
        return null;
    }

    @Override
    public byte[] dump(String key) {
        return new byte[0];
    }

    @Override
    public byte[] dump(byte[] key) {
        return new byte[0];
    }

    @Override
    public String restore(byte[] key, int ttl, byte[] serializedValue) {
        return "";
    }

    @Override
    public String restore(String key, int ttl, byte[] serializedValue) {
        return "";
    }
}
