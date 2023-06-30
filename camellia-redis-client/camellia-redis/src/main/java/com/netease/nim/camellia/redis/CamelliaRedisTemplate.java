package com.netease.nim.camellia.redis;

import com.netease.nim.camellia.core.api.*;
import com.netease.nim.camellia.core.client.env.Monitor;
import com.netease.nim.camellia.core.client.env.MultiWriteType;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.hub.standard.StandardProxyGenerator;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.redis.base.resource.RedisClusterResource;
import com.netease.nim.camellia.redis.base.resource.RedisResource;
import com.netease.nim.camellia.redis.base.resource.RedisSentinelResource;
import com.netease.nim.camellia.redis.base.utils.SafeEncoder;
import com.netease.nim.camellia.tools.utils.BytesKey;
import com.netease.nim.camellia.core.util.ResourceSelector;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceTransferUtil;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.pipeline.*;
import com.netease.nim.camellia.redis.resource.*;
import com.netease.nim.camellia.redis.util.CamelliaRedisInitializer;
import com.netease.nim.camellia.redis.base.utils.CloseUtil;
import com.netease.nim.camellia.redis.base.utils.LogUtil;
import redis.clients.jedis.*;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;

import java.util.*;


/**
 * Created by caojiajun on 2019/7/22.
 */
public class CamelliaRedisTemplate implements ICamelliaRedisTemplate {

    private static final long defaultBid = -1;
    private static final String defaultBgroup = "local";
    private static final long defaultCheckIntervalMillis = 5000;
    private static final boolean defaultMonitorEnable = false;

    private final ReloadableProxyFactory<CamelliaRedisImpl> factory;
    private final CamelliaRedisEnv env;
    private final CamelliaApi service;
    private String md5;
    private PipelinePool pipelinePool;

    public CamelliaRedisTemplate(CamelliaRedisEnv env, CamelliaApi service, long bid, String bgroup,
                                 boolean monitorEnable, long checkIntervalMillis) {
        this.factory = new ReloadableProxyFactory.Builder<CamelliaRedisImpl>()
                .service(new ApiServiceWrapper(service, env))
                .clazz(CamelliaRedisImpl.class)
                .bid(bid)
                .bgroup(bgroup)
                .monitorEnable(monitorEnable)
                .checkIntervalMillis(checkIntervalMillis)
                .proxyEnv(env.getProxyEnv())
                .build();
        this.service = service;
        this.env = env;
        this.md5 = this.factory.getResponse().getMd5();
        this.pipelinePool = new PipelinePool(env);
    }

    public CamelliaRedisTemplate(CamelliaRedisEnv env, RedisTemplateResourceTableUpdater updater) {
        this(env, new LocalDynamicCamelliaApi(updater.getResourceTable(), RedisClientResourceUtil.RedisResourceTableChecker),
                defaultBid, defaultBgroup, defaultMonitorEnable, defaultCheckIntervalMillis);
        updater.addCallback(new ResourceTableUpdateCallback() {
            @Override
            public void callback(ResourceTable resourceTable) {
                if (service instanceof LocalDynamicCamelliaApi) {
                    ((LocalDynamicCamelliaApi) service).updateResourceTable(resourceTable);
                }
                reloadResourceTable();
            }
        });
    }

    public CamelliaRedisTemplate(RedisTemplateResourceTableUpdater updater) {
        this(CamelliaRedisEnv.defaultRedisEnv(), updater);
    }

    public CamelliaRedisTemplate(CamelliaRedisEnv env, String url, long bid, String bgroup,
                                 boolean monitorEnable, long checkIntervalMillis,
                                 int connectTimeoutMillis, int readTimeoutMillis) {
        this(env, CamelliaApiUtil.init(url, connectTimeoutMillis, readTimeoutMillis), bid, bgroup, monitorEnable, checkIntervalMillis);
    }

    public CamelliaRedisTemplate(CamelliaRedisEnv env, String url, long bid, String bgroup,
                                 boolean monitorEnable, long checkIntervalMillis,
                                 int connectTimeoutMillis, int readTimeoutMillis, Map<String, String> headerMap) {
        this(env, CamelliaApiUtil.init(url, connectTimeoutMillis, readTimeoutMillis, headerMap), bid, bgroup, monitorEnable, checkIntervalMillis);
    }

    public CamelliaRedisTemplate(CamelliaRedisEnv env, String url, long bid, String bgroup,
                                 boolean monitorEnable, long checkIntervalMillis) {
        this(env, CamelliaApiUtil.init(url), bid, bgroup, monitorEnable, checkIntervalMillis);
    }

    public CamelliaRedisTemplate(CamelliaRedisEnv env, String url, long bid, String bgroup,
                                 boolean monitorEnable, long checkIntervalMillis, Map<String, String> headerMap) {
        this(env, CamelliaApiUtil.init(url, headerMap), bid, bgroup, monitorEnable, checkIntervalMillis);
    }

    public CamelliaRedisTemplate(CamelliaRedisEnv env, CamelliaApi service, long bid, String bgroup) {
        this(env, service, bid, bgroup, defaultMonitorEnable, defaultCheckIntervalMillis);
    }

    public CamelliaRedisTemplate(CamelliaRedisEnv env, String url, long bid, String bgroup) {
        this(env, CamelliaApiUtil.init(url), bid, bgroup, defaultMonitorEnable, defaultCheckIntervalMillis);
    }

    public CamelliaRedisTemplate(CamelliaRedisEnv env, String url, long bid, String bgroup, Map<String, String> headerMap) {
        this(env, CamelliaApiUtil.init(url, headerMap), bid, bgroup, defaultMonitorEnable, defaultCheckIntervalMillis);
    }

    public CamelliaRedisTemplate(String url, long bid, String bgroup, boolean monitorEnable, long checkIntervalMillis) {
        this(CamelliaRedisEnv.defaultRedisEnv(), url, bid, bgroup, monitorEnable, checkIntervalMillis);
    }

    public CamelliaRedisTemplate(String url, long bid, String bgroup, boolean monitorEnable, long checkIntervalMillis, Map<String, String> headerMap) {
        this(CamelliaRedisEnv.defaultRedisEnv(), url, bid, bgroup, monitorEnable, checkIntervalMillis, headerMap);
    }

    public CamelliaRedisTemplate(CamelliaRedisEnv env, ResourceTable resourceTable) {
        this(env, new LocalCamelliaApi(resourceTable), defaultBid, defaultBgroup, defaultMonitorEnable, defaultCheckIntervalMillis);
    }

    public CamelliaRedisTemplate(ResourceTable resourceTable) {
        this(CamelliaRedisEnv.defaultRedisEnv(), resourceTable);
    }

    public CamelliaRedisTemplate(RedisResource redisResource) {
        this(CamelliaRedisEnv.defaultRedisEnv(), ResourceTableUtil.simpleTable(redisResource));
    }

    public CamelliaRedisTemplate(RedisClusterResource redisClusterResource) {
        this(CamelliaRedisEnv.defaultRedisEnv(), ResourceTableUtil.simpleTable(redisClusterResource));
    }

    public CamelliaRedisTemplate(RedisSentinelResource redisSentinelResource) {
        this(CamelliaRedisEnv.defaultRedisEnv(), ResourceTableUtil.simpleTable(redisSentinelResource));
    }

    public CamelliaRedisTemplate(Resource resource) {
        this(CamelliaRedisEnv.defaultRedisEnv(), ResourceTableUtil.simpleTable(resource));
    }

    public CamelliaRedisTemplate(String url) {
        this(RedisClientResourceUtil.parseResourceByUrl(new Resource(url)));
    }

    public CamelliaRedisTemplate(CamelliaRedisEnv env, ReloadableLocalFileCamelliaApi reloadableLocalFileCamelliaApi, long checkIntervalMillis) {
        this(env, reloadableLocalFileCamelliaApi, defaultBid, defaultBgroup, defaultMonitorEnable, checkIntervalMillis);
    }

    public CamelliaRedisTemplate(ReloadableLocalFileCamelliaApi reloadableLocalFileCamelliaApi) {
        this(CamelliaRedisEnv.defaultRedisEnv(), reloadableLocalFileCamelliaApi, defaultCheckIntervalMillis);
    }

    public final void reloadResourceTable() {
        factory.reload(false);
    }

    public CamelliaRedisEnv getRedisEnv() {
        return env;
    }

    public CamelliaApi getCamelliaApi() {
        return service;
    }

    @Override
    public ICamelliaRedisPipeline pipelined() {
        PipelinePool pipelinePool = this.pipelinePool;
        CamelliaApiResponse response = factory.getResponse();
        if (!Objects.equals(response.getMd5(), this.md5)) {
            pipelinePool = new PipelinePool(env);
            this.pipelinePool = pipelinePool;
            this.md5 = response.getMd5();
        }
        CamelliaRedisPipeline pipeline = pipelinePool.get();
        if (pipeline != null) return pipeline;
        final ResponseQueable queable = new ResponseQueable(env);
        final RedisClientPool redisClientPool = new RedisClientPool.DefaultRedisClientPool(env.getJedisPoolFactory(), env.getJedisClusterFactory());
        ResourceTable resourceTable = ResourceTransferUtil.transfer(response.getResourceTable(), resource -> {
            if (resource == null) return null;
            PipelineResource pipelineResource = new PipelineResource(resource);
            pipelineResource.setQueable(queable);
            pipelineResource.setClientPool(redisClientPool);
            pipelineResource.setRedisEnv(env);
            return pipelineResource;
        });
        ProxyEnv env = factory.getEnv();
        //pipeline的proxy对象在分片和双写场景下，不能使用分片并发和多写并发，因为在某些场景下可能有并发问题（获取Client、Client发出指令、把Response对象放到ResponseQueable这三个操作）
        //并且由于pipeline下，只有在调用sync方法时才会真正发出请求，因此使用分片并发和多写并发本身也没有什么意义
        ProxyEnv pipelineProxyEnv = new ProxyEnv.Builder(env)
                .shardingConcurrentEnable(false).multiWriteType(MultiWriteType.SINGLE_THREAD).build();
        StandardProxyGenerator<CamelliaRedisPipelineImpl> generator = new StandardProxyGenerator<>(CamelliaRedisPipelineImpl.class,
                resourceTable, null, pipelineProxyEnv);
        CamelliaRedisPipelineImpl pipelineProxy = generator.generate();
        return new CamelliaRedisPipeline(pipelineProxy, queable, redisClientPool, pipelinePool);
    }

    @Override
    public String set(byte[] key, byte[] value) {
        return factory.getProxy().set(key, value);
    }

    @Override
    public byte[] get(byte[] key) {
        return factory.getProxy().get(key);
    }

    @Override
    public String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
        return factory.getProxy().set(key, value, nxxx, expx, time);
    }

    @Override
    public String set(byte[] key, byte[] value, byte[] nxxx) {
        return factory.getProxy().set(key, value, nxxx);
    }

    @Override
    public Boolean exists(byte[] key) {
        return factory.getProxy().exists(key);
    }

    @Override
    public Long persist(byte[] key) {
        return factory.getProxy().persist(key);
    }

    @Override
    public String type(byte[] key) {
        return factory.getProxy().type(key);
    }

    @Override
    public Long expire(byte[] key, int seconds) {
        return factory.getProxy().expire(key, seconds);
    }

    @Override
    public Long pexpire(byte[] key, long milliseconds) {
        return factory.getProxy().pexpire(key, milliseconds);
    }

    @Override
    public Long expireAt(byte[] key, long unixTime) {
        return factory.getProxy().expireAt(key, unixTime);
    }

    @Override
    public Long pexpireAt(byte[] key, long millisecondsTimestamp) {
        return factory.getProxy().pexpireAt(key, millisecondsTimestamp);
    }

    @Override
    public Long ttl(byte[] key) {
        return factory.getProxy().ttl(key);
    }

    @Override
    public Long pttl(byte[] key) {
        return factory.getProxy().pttl(key);
    }

    @Override
    public Boolean setbit(byte[] key, long offset, boolean value) {
        return factory.getProxy().setbit(key, offset, value);
    }

    @Override
    public Boolean setbit(byte[] key, long offset, byte[] value) {
        return factory.getProxy().setbit(key, offset, value);
    }

    @Override
    public Boolean getbit(byte[] key, long offset) {
        return factory.getProxy().getbit(key, offset);
    }

    @Override
    public Long setrange(byte[] key, long offset, byte[] value) {
        return factory.getProxy().setrange(key, offset, value);
    }

    @Override
    public byte[] getrange(byte[] key, long startOffset, long endOffset) {
        return factory.getProxy().getrange(key, startOffset, endOffset);
    }

    @Override
    public byte[] getSet(byte[] key, byte[] value) {
        return factory.getProxy().getSet(key, value);
    }

    @Override
    public Long setnx(byte[] key, byte[] value) {
        return factory.getProxy().setnx(key, value);
    }

    @Override
    public String setex(byte[] key, int seconds, byte[] value) {
        return factory.getProxy().setex(key, seconds, value);
    }

    @Override
    public String psetex(byte[] key, long milliseconds, byte[] value) {
        return factory.getProxy().psetex(key, milliseconds, value);
    }

    @Override
    public Long decrBy(byte[] key, long integer) {
        return factory.getProxy().decrBy(key, integer);
    }

    @Override
    public Long decr(byte[] key) {
        return factory.getProxy().decr(key);
    }

    @Override
    public Long incrBy(byte[] key, long integer) {
        return factory.getProxy().incrBy(key, integer);
    }

    @Override
    public Double incrByFloat(byte[] key, double value) {
        return factory.getProxy().incrByFloat(key, value);
    }

    @Override
    public Long incr(byte[] key) {
        return factory.getProxy().incr(key);
    }

    @Override
    public Long append(byte[] key, byte[] value) {
        return factory.getProxy().append(key, value);
    }

    @Override
    public byte[] substr(byte[] key, int start, int end) {
        return factory.getProxy().substr(key, start, end);
    }

    @Override
    public Long hset(byte[] key, byte[] field, byte[] value) {
        return factory.getProxy().hset(key, field, value);
    }

    @Override
    public byte[] hget(byte[] key, byte[] field) {
        return factory.getProxy().hget(key, field);
    }

    @Override
    public Long hsetnx(byte[] key, byte[] field, byte[] value) {
        return factory.getProxy().hsetnx(key, field, value);
    }

    @Override
    public String hmset(byte[] key, Map<byte[], byte[]> hash) {
        return factory.getProxy().hmset(key, hash);
    }

    @Override
    public List<byte[]> hmget(byte[] key, byte[]... fields) {
        return factory.getProxy().hmget(key, fields);
    }

    @Override
    public Long hincrBy(byte[] key, byte[] field, long value) {
        return factory.getProxy().hincrBy(key, field, value);
    }

    @Override
    public Double hincrByFloat(byte[] key, byte[] field, double value) {
        return factory.getProxy().hincrByFloat(key, field, value);
    }

    @Override
    public Boolean hexists(byte[] key, byte[] field) {
        return factory.getProxy().hexists(key, field);
    }

    @Override
    public Long hdel(byte[] key, byte[]... field) {
        return factory.getProxy().hdel(key, field);
    }

    @Override
    public Long hlen(byte[] key) {
        return factory.getProxy().hlen(key);
    }

    @Override
    public Set<byte[]> hkeys(byte[] key) {
        return factory.getProxy().hkeys(key);
    }

    @Override
    public List<byte[]> hvals(byte[] key) {
        return factory.getProxy().hvals(key);
    }

    @Override
    public Map<byte[], byte[]> hgetAll(byte[] key) {
        return factory.getProxy().hgetAll(key);
    }

    @Override
    public Long rpush(byte[] key, byte[]... args) {
        return factory.getProxy().rpush(key, args);
    }

    @Override
    public Long lpush(byte[] key, byte[]... args) {
        return factory.getProxy().lpush(key, args);
    }

    @Override
    public Long llen(byte[] key) {
        return factory.getProxy().llen(key);
    }

    @Override
    public List<byte[]> lrange(byte[] key, long start, long end) {
        return factory.getProxy().lrange(key, start, end);
    }

    @Override
    public String ltrim(byte[] key, long start, long end) {
        return factory.getProxy().ltrim(key, start, end);
    }

    @Override
    public byte[] lindex(byte[] key, long index) {
        return factory.getProxy().lindex(key, index);
    }

    @Override
    public String lset(byte[] key, long index, byte[] value) {
        return factory.getProxy().lset(key, index, value);
    }

    @Override
    public Long lrem(byte[] key, long count, byte[] value) {
        return factory.getProxy().lrem(key, count, value);
    }

    @Override
    public byte[] lpop(byte[] key) {
        return factory.getProxy().lpop(key);
    }

    @Override
    public byte[] rpop(byte[] key) {
        return factory.getProxy().rpop(key);
    }

    @Override
    public Long sadd(byte[] key, byte[]... member) {
        return factory.getProxy().sadd(key, member);
    }

    @Override
    public Set<byte[]> smembers(byte[] key) {
        return factory.getProxy().smembers(key);
    }

    @Override
    public Long srem(byte[] key, byte[]... member) {
        return factory.getProxy().srem(key, member);
    }

    @Override
    public byte[] spop(byte[] key) {
        return factory.getProxy().spop(key);
    }

    @Override
    public Set<byte[]> spop(byte[] key, long count) {
        return factory.getProxy().spop(key, count);
    }

    @Override
    public Long scard(byte[] key) {
        return factory.getProxy().scard(key);
    }

    @Override
    public Boolean sismember(byte[] key, byte[] member) {
        return factory.getProxy().sismember(key, member);
    }

    @Override
    public byte[] srandmember(byte[] key) {
        return factory.getProxy().srandmember(key);
    }

    @Override
    public List<byte[]> srandmember(byte[] key, int count) {
        return factory.getProxy().srandmember(key, count);
    }

    @Override
    public Long strlen(byte[] key) {
        return factory.getProxy().strlen(key);
    }

    @Override
    public Long zadd(byte[] key, double score, byte[] member) {
        return factory.getProxy().zadd(key, score, member);
    }

    @Override
    public Long zadd(byte[] key, double score, byte[] member, ZAddParams params) {
        return factory.getProxy().zadd(key, score, member, params);
    }

    @Override
    public Long zadd(byte[] key, Map<byte[], Double> scoreMembers) {
        return factory.getProxy().zadd(key, scoreMembers);
    }

    @Override
    public Long zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
        return factory.getProxy().zadd(key, scoreMembers, params);
    }

    @Override
    public Set<byte[]> zrange(byte[] key, long start, long end) {
        return factory.getProxy().zrange(key, start, end);
    }

    @Override
    public Long zrem(byte[] key, byte[]... member) {
        return factory.getProxy().zrem(key, member);
    }

    @Override
    public Double zincrby(byte[] key, double score, byte[] member) {
        return factory.getProxy().zincrby(key, score, member);
    }

    @Override
    public Double zincrby(byte[] key, double score, byte[] member, ZIncrByParams params) {
        return factory.getProxy().zincrby(key, score, member, params);
    }

    @Override
    public Long zrank(byte[] key, byte[] member) {
        return factory.getProxy().zrank(key, member);
    }

    @Override
    public Long zrevrank(byte[] key, byte[] member) {
        return factory.getProxy().zrevrank(key, member);
    }

    @Override
    public Set<byte[]> zrevrange(byte[] key, long start, long end) {
        return factory.getProxy().zrevrange(key, start, end);
    }

    @Override
    public Set<Tuple> zrangeWithScores(byte[] key, long start, long end) {
        return factory.getProxy().zrangeWithScores(key, start, end);
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(byte[] key, long start, long end) {
        return factory.getProxy().zrevrangeWithScores(key, start, end);
    }

    @Override
    public Long zcard(byte[] key) {
        return factory.getProxy().zcard(key);
    }

    @Override
    public Double zscore(byte[] key, byte[] member) {
        return factory.getProxy().zscore(key, member);
    }

    @Override
    public List<byte[]> sort(byte[] key) {
        return factory.getProxy().sort(key);
    }

    @Override
    public List<byte[]> sort(byte[] key, SortingParams sortingParameters) {
        return factory.getProxy().sort(key, sortingParameters);
    }

    @Override
    public Long zcount(byte[] key, double min, double max) {
        return factory.getProxy().zcount(key, min, max);
    }

    @Override
    public Long zcount(byte[] key, byte[] min, byte[] max) {
        return factory.getProxy().zcount(key, min, max);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
        return factory.getProxy().zrangeByScore(key, min, max);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
        return factory.getProxy().zrangeByScore(key, min, max);
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
        return factory.getProxy().zrevrangeByScore(key, max, min);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
        return factory.getProxy().zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
        return factory.getProxy().zrevrangeByScore(key, max, min);
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return factory.getProxy().zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
        return factory.getProxy().zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
        return factory.getProxy().zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
        return factory.getProxy().zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
        return factory.getProxy().zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return factory.getProxy().zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
        return factory.getProxy().zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
        return factory.getProxy().zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return factory.getProxy().zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
        return factory.getProxy().zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return factory.getProxy().zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Long zremrangeByRank(byte[] key, long start, long end) {
        return factory.getProxy().zremrangeByRank(key, start, end);
    }

    @Override
    public Long zremrangeByScore(byte[] key, double start, double end) {
        return factory.getProxy().zremrangeByScore(key, start, end);
    }

    @Override
    public Long zremrangeByScore(byte[] key, byte[] start, byte[] end) {
        return factory.getProxy().zremrangeByScore(key, start, end);
    }

    @Override
    public Long zlexcount(byte[] key, byte[] min, byte[] max) {
        return factory.getProxy().zlexcount(key, min, max);
    }

    @Override
    public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
        return factory.getProxy().zrangeByLex(key, min, max);
    }

    @Override
    public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
        return factory.getProxy().zrangeByLex(key, min, max, offset, count);
    }

    @Override
    public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
        return factory.getProxy().zrevrangeByLex(key, max, min);
    }

    @Override
    public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
        return factory.getProxy().zrevrangeByLex(key, max, min, offset, count);
    }

    @Override
    public Long zremrangeByLex(byte[] key, byte[] min, byte[] max) {
        return factory.getProxy().zremrangeByLex(key, min, max);
    }

    @Override
    public Long linsert(byte[] key, BinaryClient.LIST_POSITION where, byte[] pivot, byte[] value) {
        return factory.getProxy().linsert(key, where, pivot, value);
    }

    @Override
    public Long lpushx(byte[] key, byte[]... arg) {
        return factory.getProxy().lpushx(key, arg);
    }

    @Override
    public Long rpushx(byte[] key, byte[]... arg) {
        return factory.getProxy().rpushx(key, arg);
    }

    @Override
    public Long del(byte[] key) {
        return factory.getProxy().del(key);
    }

    @Override
    public Long bitcount(byte[] key) {
        return factory.getProxy().bitcount(key);
    }

    @Override
    public Long bitcount(byte[] key, long start, long end) {
        return factory.getProxy().bitcount(key, start, end);
    }

    @Override
    public Long pfadd(byte[] key, byte[]... elements) {
        return factory.getProxy().pfadd(key, elements);
    }

    @Override
    public long pfcount(byte[] key) {
        return factory.getProxy().pfcount(key);
    }

    @Override
    public Long geoadd(byte[] key, double longitude, double latitude, byte[] member) {
        return factory.getProxy().geoadd(key, longitude, latitude, member);
    }

    @Override
    public Long geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
        return factory.getProxy().geoadd(key, memberCoordinateMap);
    }

    @Override
    public Double geodist(byte[] key, byte[] member1, byte[] member2) {
        return factory.getProxy().geodist(key, member1, member2);
    }

    @Override
    public Double geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
        return factory.getProxy().geodist(key, member1, member2, unit);
    }

    @Override
    public List<byte[]> geohash(byte[] key, byte[]... members) {
        return factory.getProxy().geohash(key, members);
    }

    @Override
    public List<GeoCoordinate> geopos(byte[] key, byte[]... members) {
        return factory.getProxy().geopos(key, members);
    }

    @Override
    public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
        return factory.getProxy().georadius(key, longitude, latitude, radius, unit);
    }

    @Override
    public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        return factory.getProxy().georadius(key, longitude, latitude, radius, unit, param);
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit) {
        return factory.getProxy().georadiusByMember(key, member, radius, unit);
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
        return factory.getProxy().georadiusByMember(key, member, radius, unit, param);
    }

    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor) {
        return factory.getProxy().hscan(key, cursor);
    }

    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor, ScanParams params) {
        return factory.getProxy().hscan(key, cursor, params);
    }

    @Override
    public ScanResult<byte[]> sscan(byte[] key, byte[] cursor) {
        return factory.getProxy().sscan(key, cursor);
    }

    @Override
    public ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams params) {
        return factory.getProxy().sscan(key, cursor, params);
    }

    @Override
    public ScanResult<Tuple> zscan(byte[] key, byte[] cursor) {
        return factory.getProxy().zscan(key, cursor);
    }

    @Override
    public ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams params) {
        return factory.getProxy().zscan(key, cursor, params);
    }

    @Override
    public List<Long> bitfield(byte[] key, byte[]... arguments) {
        return factory.getProxy().bitfield(key, arguments);
    }

    @Override
    public String set(String key, String value) {
        return factory.getProxy().set(key, value);
    }

    @Override
    public String set(String key, String value, String nxxx, String expx, long time) {
        return factory.getProxy().set(key, value, nxxx, expx, time);
    }

    @Override
    public String set(String key, String value, String nxxx) {
        return factory.getProxy().set(key, value, nxxx);
    }

    @Override
    public String get(String key) {
        return factory.getProxy().get(key);
    }

    @Override
    public Boolean exists(String key) {
        return factory.getProxy().exists(key);
    }

    @Override
    public Long persist(String key) {
        return factory.getProxy().persist(key);
    }

    @Override
    public String type(String key) {
        return factory.getProxy().type(key);
    }

    @Override
    public Long expire(String key, int seconds) {
        return factory.getProxy().expire(key, seconds);
    }

    @Override
    public Long pexpire(String key, long milliseconds) {
        return factory.getProxy().pexpire(key, milliseconds);
    }

    @Override
    public Long expireAt(String key, long unixTime) {
        return factory.getProxy().expireAt(key, unixTime);
    }

    @Override
    public Long pexpireAt(String key, long millisecondsTimestamp) {
        return factory.getProxy().pexpireAt(key, millisecondsTimestamp);
    }

    @Override
    public Long ttl(String key) {
        return factory.getProxy().ttl(key);
    }

    @Override
    public Long pttl(String key) {
        return factory.getProxy().pttl(key);
    }

    @Override
    public Boolean setbit(String key, long offset, boolean value) {
        return factory.getProxy().setbit(key, offset, value);
    }

    @Override
    public Boolean setbit(String key, long offset, String value) {
        return factory.getProxy().setbit(key, offset, value);
    }

    @Override
    public Boolean getbit(String key, long offset) {
        return factory.getProxy().getbit(key, offset);
    }

    @Override
    public Long setrange(String key, long offset, String value) {
        return factory.getProxy().setrange(key, offset, value);
    }

    @Override
    public String getrange(String key, long startOffset, long endOffset) {
        return factory.getProxy().getrange(key, startOffset, endOffset);
    }

    @Override
    public String getSet(String key, String value) {
        return factory.getProxy().getSet(key, value);
    }

    @Override
    public Long setnx(String key, String value) {
        return factory.getProxy().setnx(key, value);
    }

    @Override
    public String setex(String key, int seconds, String value) {
        return factory.getProxy().setex(key, seconds, value);
    }

    @Override
    public String psetex(String key, long milliseconds, String value) {
        return factory.getProxy().psetex(key, milliseconds, value);
    }

    @Override
    public Long decrBy(String key, long integer) {
        return factory.getProxy().decrBy(key, integer);
    }

    @Override
    public Long decr(String key) {
        return factory.getProxy().decr(key);
    }

    @Override
    public Long incrBy(String key, long integer) {
        return factory.getProxy().incrBy(key, integer);
    }

    @Override
    public Double incrByFloat(String key, double value) {
        return factory.getProxy().incrByFloat(key, value);
    }

    @Override
    public Long incr(String key) {
        return factory.getProxy().incr(key);
    }

    @Override
    public Long append(String key, String value) {
        return factory.getProxy().append(key, value);
    }

    @Override
    public String substr(String key, int start, int end) {
        return factory.getProxy().substr(key, start, end);
    }

    @Override
    public Long hset(String key, String field, String value) {
        return factory.getProxy().hset(key, field, value);
    }

    @Override
    public String hget(String key, String field) {
        return factory.getProxy().hget(key, field);
    }

    @Override
    public Long hsetnx(String key, String field, String value) {
        return factory.getProxy().hsetnx(key, field, value);
    }

    @Override
    public String hmset(String key, Map<String, String> hash) {
        return factory.getProxy().hmset(key, hash);
    }

    @Override
    public List<String> hmget(String key, String... fields) {
        return factory.getProxy().hmget(key, fields);
    }

    @Override
    public Long hincrBy(String key, String field, long value) {
        return factory.getProxy().hincrBy(key, field, value);
    }

    @Override
    public Double hincrByFloat(String key, String field, double value) {
        return factory.getProxy().hincrByFloat(key, field, value);
    }

    @Override
    public Boolean hexists(String key, String field) {
        return factory.getProxy().hexists(key, field);
    }

    @Override
    public Long hdel(String key, String... field) {
        return factory.getProxy().hdel(key, field);
    }

    @Override
    public Long hlen(String key) {
        return factory.getProxy().hlen(key);
    }

    @Override
    public Set<String> hkeys(String key) {
        return factory.getProxy().hkeys(key);
    }

    @Override
    public List<String> hvals(String key) {
        return factory.getProxy().hvals(key);
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        return factory.getProxy().hgetAll(key);
    }

    @Override
    public Long rpush(String key, String... string) {
        return factory.getProxy().rpush(key, string);
    }

    @Override
    public Long lpush(String key, String... string) {
        return factory.getProxy().lpush(key, string);
    }

    @Override
    public Long llen(String key) {
        return factory.getProxy().llen(key);
    }

    @Override
    public List<String> lrange(String key, long start, long end) {
        return factory.getProxy().lrange(key, start, end);
    }

    @Override
    public String ltrim(String key, long start, long end) {
        return factory.getProxy().ltrim(key, start, end);
    }

    @Override
    public String lindex(String key, long index) {
        return factory.getProxy().lindex(key, index);
    }

    @Override
    public String lset(String key, long index, String value) {
        return factory.getProxy().lset(key, index, value);
    }

    @Override
    public Long lrem(String key, long count, String value) {
        return factory.getProxy().lrem(key, count, value);
    }

    @Override
    public String lpop(String key) {
        return factory.getProxy().lpop(key);
    }

    @Override
    public String rpop(String key) {
        return factory.getProxy().rpop(key);
    }

    @Override
    public Long sadd(String key, String... member) {
        return factory.getProxy().sadd(key, member);
    }

    @Override
    public Set<String> smembers(String key) {
        return factory.getProxy().smembers(key);
    }

    @Override
    public Long srem(String key, String... member) {
        return factory.getProxy().srem(key, member);
    }

    @Override
    public String spop(String key) {
        return factory.getProxy().spop(key);
    }

    @Override
    public Set<String> spop(String key, long count) {
        return factory.getProxy().spop(key, count);
    }

    @Override
    public Long scard(String key) {
        return factory.getProxy().scard(key);
    }

    @Override
    public Boolean sismember(String key, String member) {
        return factory.getProxy().sismember(key, member);
    }

    @Override
    public String srandmember(String key) {
        return factory.getProxy().srandmember(key);
    }

    @Override
    public List<String> srandmember(String key, int count) {
        return factory.getProxy().srandmember(key, count);
    }

    @Override
    public Long strlen(String key) {
        return factory.getProxy().strlen(key);
    }

    @Override
    public Long zadd(String key, double score, String member) {
        return factory.getProxy().zadd(key, score, member);
    }

    @Override
    public Long zadd(String key, double score, String member, ZAddParams params) {
        return factory.getProxy().zadd(key, score, member, params);
    }

    @Override
    public Long zadd(String key, Map<String, Double> scoreMembers) {
        return factory.getProxy().zadd(key, scoreMembers);
    }

    @Override
    public Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
        return factory.getProxy().zadd(key, scoreMembers, params);
    }

    @Override
    public Set<String> zrange(String key, long start, long end) {
        return factory.getProxy().zrange(key, start, end);
    }

    @Override
    public Long zrem(String key, String... member) {
        return factory.getProxy().zrem(key, member);
    }

    @Override
    public Double zincrby(String key, double score, String member) {
        return factory.getProxy().zincrby(key, score, member);
    }

    @Override
    public Double zincrby(String key, double score, String member, ZIncrByParams params) {
        return factory.getProxy().zincrby(key, score, member, params);
    }

    @Override
    public Long zrank(String key, String member) {
        return factory.getProxy().zrank(key, member);
    }

    @Override
    public Long zrevrank(String key, String member) {
        return factory.getProxy().zrevrank(key, member);
    }

    @Override
    public Set<String> zrevrange(String key, long start, long end) {
        return factory.getProxy().zrevrange(key, start, end);
    }

    @Override
    public Set<Tuple> zrangeWithScores(String key, long start, long end) {
        return factory.getProxy().zrangeWithScores(key, start, end);
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
        return factory.getProxy().zrevrangeWithScores(key, start, end);
    }

    @Override
    public Long zcard(String key) {
        return factory.getProxy().zcard(key);
    }

    @Override
    public Double zscore(String key, String member) {
        return factory.getProxy().zscore(key, member);
    }

    @Override
    public List<String> sort(String key) {
        return factory.getProxy().sort(key);
    }

    @Override
    public List<String> sort(String key, SortingParams sortingParameters) {
        return factory.getProxy().sort(key, sortingParameters);
    }

    @Override
    public Long zcount(String key, double min, double max) {
        return factory.getProxy().zcount(key, min, max);
    }

    @Override
    public Long zcount(String key, String min, String max) {
        return factory.getProxy().zcount(key, min, max);
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max) {
        return factory.getProxy().zrangeByScore(key, min, max);
    }

    @Override
    public Set<String> zrangeByScore(String key, String min, String max) {
        return factory.getProxy().zrangeByScore(key, min, max);
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min) {
        return factory.getProxy().zrevrangeByScore(key, max, min);
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        return factory.getProxy().zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<String> zrevrangeByScore(String key, String max, String min) {
        return factory.getProxy().zrevrangeByScore(key, max, min);
    }

    @Override
    public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
        return factory.getProxy().zrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        return factory.getProxy().zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        return factory.getProxy().zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
        return factory.getProxy().zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        return factory.getProxy().zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
        return factory.getProxy().zrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
        return factory.getProxy().zrangeByScoreWithScores(key, min, max);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
        return factory.getProxy().zrevrangeByScoreWithScores(key, max, min);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
        return factory.getProxy().zrangeByScoreWithScores(key, min, max, offset, count);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        return factory.getProxy().zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
        return factory.getProxy().zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    @Override
    public Long zremrangeByRank(String key, long start, long end) {
        return factory.getProxy().zremrangeByRank(key, start, end);
    }

    @Override
    public Long zremrangeByScore(String key, double start, double end) {
        return factory.getProxy().zremrangeByScore(key, start, end);
    }

    @Override
    public Long zremrangeByScore(String key, String start, String end) {
        return factory.getProxy().zremrangeByScore(key, start, end);
    }

    @Override
    public Long zlexcount(String key, String min, String max) {
        return factory.getProxy().zlexcount(key, min, max);
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max) {
        return factory.getProxy().zrangeByLex(key, min, max);
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max, int offset, int count) {
        return factory.getProxy().zrangeByLex(key, min, max, offset, count);
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min) {
        return factory.getProxy().zrevrangeByLex(key, max, min);
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
        return factory.getProxy().zrevrangeByLex(key, max, min, offset, count);
    }

    @Override
    public Long zremrangeByLex(String key, String min, String max) {
        return factory.getProxy().zremrangeByLex(key, min, max);
    }

    @Override
    public Long linsert(String key, BinaryClient.LIST_POSITION where, String pivot, String value) {
        return factory.getProxy().linsert(key, where, pivot, value);
    }

    @Override
    public Long lpushx(String key, String... string) {
        return factory.getProxy().lpushx(key, string);
    }

    @Override
    public Long rpushx(String key, String... string) {
        return factory.getProxy().rpushx(key, string);
    }

    @Override
    public Long del(String key) {
        return factory.getProxy().del(key);
    }

    @Override
    public String echo(String string) {
        return factory.getProxy().echo(string);
    }

    @Override
    public Long bitcount(String key) {
        return factory.getProxy().bitcount(key);
    }

    @Override
    public Long bitcount(String key, long start, long end) {
        return factory.getProxy().bitcount(key, start, end);
    }

    @Override
    public Long bitpos(String key, boolean value) {
        return factory.getProxy().bitpos(key, value);
    }

    @Override
    public Long bitpos(String key, boolean value, BitPosParams params) {
        return factory.getProxy().bitpos(key, value, params);
    }

    @Override
    public Long bitpos(byte[] key, boolean value) {
        return factory.getProxy().bitpos(key, value);
    }

    @Override
    public Long bitpos(byte[] key, boolean value, BitPosParams params) {
        return factory.getProxy().bitpos(key, value, params);
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor) {
        return factory.getProxy().hscan(key, cursor);
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
        return factory.getProxy().hscan(key, cursor, params);
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor) {
        return factory.getProxy().sscan(key, cursor);
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
        return factory.getProxy().sscan(key, cursor, params);
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor) {
        return factory.getProxy().zscan(key, cursor);
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
        return factory.getProxy().zscan(key, cursor, params);
    }

    @Override
    public Long pfadd(String key, String... elements) {
        return factory.getProxy().pfadd(key, elements);
    }

    @Override
    public long pfcount(String key) {
        return factory.getProxy().pfcount(key);
    }

    @Override
    public Long geoadd(String key, double longitude, double latitude, String member) {
        return factory.getProxy().geoadd(key, longitude, latitude, member);
    }

    @Override
    public Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
        return factory.getProxy().geoadd(key, memberCoordinateMap);
    }

    @Override
    public Double geodist(String key, String member1, String member2) {
        return factory.getProxy().geodist(key, member1, member2);
    }

    @Override
    public Double geodist(String key, String member1, String member2, GeoUnit unit) {
        return factory.getProxy().geodist(key, member1, member2, unit);
    }

    @Override
    public List<String> geohash(String key, String... members) {
        return factory.getProxy().geohash(key, members);
    }

    @Override
    public List<GeoCoordinate> geopos(String key, String... members) {
        return factory.getProxy().geopos(key, members);
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
        return factory.getProxy().georadius(key, longitude, latitude, radius, unit);
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        return factory.getProxy().georadius(key, longitude, latitude, radius, unit, param);
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
        return factory.getProxy().georadiusByMember(key, member, radius, unit);
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
        return factory.getProxy().georadiusByMember(key, member, radius, unit, param);
    }

    @Override
    public List<Long> bitfield(String key, String... arguments) {
        return factory.getProxy().bitfield(key, arguments);
    }

    @Override
    public Long del(byte[]... keys) {
        if (keys == null) return 0L;
        if (keys.length == 0) return 0L;
        return factory.getProxy().del(keys);
    }

    @Override
    public Long exists(byte[]... keys) {
        if (keys == null) return 0L;
        if (keys.length == 0) return 0L;
        return factory.getProxy().exists(keys);
    }

    @Override
    public List<byte[]> mget(byte[]... keys) {
        if (keys == null) return Collections.emptyList();
        if (keys.length == 0) return Collections.emptyList();
        Map<byte[], byte[]> mget = factory.getProxy().mget(keys);
        if (mget == null) return null;
        List<byte[]> list = new ArrayList<>(keys.length);
        Map<BytesKey, byte[]> map = new HashMap<>();
        for (Map.Entry<byte[], byte[]> entry : mget.entrySet()) {
            byte[] key = entry.getKey();
            map.put(new BytesKey(key), entry.getValue());
        }
        for (byte[] key : keys) {
            list.add(map.get(new BytesKey(key)));
        }
        return list;
    }

    @Override
    public String mset(byte[]... keysvalues) {
        if (keysvalues == null) return null;
        if (keysvalues.length == 0) return null;
        if (keysvalues.length % 2 != 0) {
            throw new CamelliaRedisException("keysvalues not match");
        }
        Map<byte[], byte[]> map = new HashMap<>();
        for (int i = 0; i < keysvalues.length / 2; i++) {
            map.put(keysvalues[i * 2], keysvalues[i * 2 + 1]);
        }
        return factory.getProxy().mset(map);
    }

    @Override
    public String mset(String... keysvalues) {
        if (keysvalues == null) return null;
        if (keysvalues.length == 0) return null;
        if (keysvalues.length % 2 != 0) {
            throw new CamelliaRedisException("keysvalues not match");
        }
        Map<byte[], byte[]> map = new HashMap<>();
        for (int i = 0; i < keysvalues.length / 2; i++) {
            map.put(SafeEncoder.encode(keysvalues[i * 2]), SafeEncoder.encode(keysvalues[i * 2 + 1]));
        }
        return factory.getProxy().mset(map);
    }

    @Override
    public Long del(String... keys) {
        if (keys == null) return 0L;
        if (keys.length == 0) return 0L;
        return factory.getProxy().del(keys);
    }

    @Override
    public Long exists(String... keys) {
        if (keys == null) return 0L;
        if (keys.length == 0) return 0L;
        return factory.getProxy().exists(keys);
    }

    @Override
    public List<String> mget(String... keys) {
        if (keys == null) return Collections.emptyList();
        if (keys.length == 0) return Collections.emptyList();
        Map<String, String> mget = factory.getProxy().mget(keys);
        if (mget == null) return null;
        List<String> list = new ArrayList<>(keys.length);
        for (String key : keys) {
            list.add(mget.get(key));
        }
        return list;
    }

    @Override
    public byte[] dump(String key) {
        return factory.getProxy().dump(key);
    }

    @Override
    public byte[] dump(byte[] key) {
        return factory.getProxy().dump(key);
    }

    @Override
    public String restore(byte[] key, int ttl, byte[] serializedValue) {
        return factory.getProxy().restore(key, ttl, serializedValue);
    }

    @Override
    public String restore(String key, int ttl, byte[] serializedValue) {
        return factory.getProxy().restore(key, ttl, serializedValue);
    }

    @Override
    public Object eval(String script, int keyCount, String... params) {
        return eval(SafeEncoder.encode(script), keyCount, SafeEncoder.encodeMany(params));
    }

    @Override
    public Object eval(String script, List<String> keys, List<String> args) {
        return eval(script, keys.size(), getParams(keys, args));
    }

    private static String[] getParams(List<String> keys, List<String> args) {
        int keyCount = keys.size();
        int argCount = args.size();
        String[] params = new String[keyCount + args.size()];
        for (int i = 0; i < keyCount; i++) {
            params[i] = keys.get(i);
        }
        for (int i = 0; i < argCount; i++) {
            params[keyCount + i] = args.get(i);
        }
        return params;
    }

    @Override
    public Object eval(String script) {
        return eval(script, 0);
    }

    @Override
    public Object eval(byte[] script, List<byte[]> keys, List<byte[]> args) {
        return eval(script, toByteArray(keys.size()), getParamsWithBinary(keys, args));
    }

    private static byte[] toByteArray(final int value) {
        return SafeEncoder.encode(String.valueOf(value));
    }

    private static byte[][] getParamsWithBinary(List<byte[]> keys, List<byte[]> args) {
        final int keyCount = keys.size();
        final int argCount = args.size();
        byte[][] params = new byte[keyCount + argCount][];

        for (int i = 0; i < keyCount; i++)
            params[i] = keys.get(i);

        for (int i = 0; i < argCount; i++)
            params[keyCount + i] = args.get(i);

        return params;
    }

    @Override
    public Object eval(byte[] script, byte[] keyCount, byte[]... params) {
        return eval(script, Integer.parseInt(SafeEncoder.encode(keyCount)), params);
    }

    @Override
    public Object eval(byte[] script) {
        return eval(script, 0);
    }

    @Override
    public Object evalsha(String sha1) {
        return evalsha(sha1, 0);
    }

    @Override
    public Object evalsha(String sha1, List<String> keys, List<String> args) {
        return evalsha(sha1, keys.size(), getParams(keys, args));
    }

    @Override
    public Object evalsha(String sha1, int keyCount, String... params) {
        return evalsha(SafeEncoder.encode(sha1), keyCount, SafeEncoder.encodeMany(params));
    }

    @Override
    public Object evalsha(byte[] sha1) {
        return evalsha(sha1, 0);
    }

    @Override
    public Object evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args) {
        return evalsha(sha1, keys.size(), getParamsWithBinary(keys, args));
    }

    @Override
    public Object eval(final byte[] script, final int keyCount, final byte[]... params) {
        return writeInvoke((resource, redis) -> {
            if (LogUtil.isDebugEnabled()) {
                List<String> list = new ArrayList<>();
                for (int i = 0; i < keyCount; i++) {
                    list.add(SafeEncoder.encode(params[i]));
                }
                LogUtil.debugLog(redis.getClass().getSimpleName(), "eval", resource, SafeEncoder.encode(script), list.toArray(new String[0]));
            }
            try {
                return redis.eval(script, keyCount, params);
            } finally {
                Monitor monitor = factory.getEnv().getMonitor();
                if (monitor != null) {
                    monitor.incrWrite(resource.getUrl(), redis.getClass().getName(), "eval(byte[],int,byte[])");
                }
            }
        }, keyCount, params);
    }

    @Override
    public Object evalsha(final byte[] sha1, final int keyCount, final byte[]... params) {
        return writeInvoke((resource, redis) -> {
            if (LogUtil.isDebugEnabled()) {
                List<String> list = new ArrayList<>();
                for (int i = 0; i < keyCount; i++) {
                    list.add(SafeEncoder.encode(params[i]));
                }
                LogUtil.debugLog(redis.getClass().getSimpleName(), "evalsha", resource, SafeEncoder.encode(sha1), list.toArray(new String[0]));
            }
            try {
                return redis.evalsha(sha1, keyCount, params);
            } finally {
                Monitor monitor = factory.getEnv().getMonitor();
                if (monitor != null) {
                    monitor.incrWrite(resource.getUrl(), redis.getClass().getName(), "evalsha(byte[],int,byte[])");
                }
            }
        }, keyCount, params);
    }

    /**
     * 上层业务使用完毕后，请自行close jedis
     */
    @Override
    public Jedis getWriteJedis(byte[]... keys) {
        if (keys == null || keys.length == 0) {
            throw new CamelliaRedisException("keys is null or empty");
        }
        ResourceSelector selector = factory.getResourceSelector();
        String url = null;
        for (byte[] key : keys) {
            List<Resource> writeResources = selector.getWriteResources(key);
            if (writeResources == null || writeResources.size() > 1) {
                throw new CamelliaRedisException("not support while in multi-write mode");
            }
            if (url != null && !url.equalsIgnoreCase(writeResources.get(0).getUrl())) {
                throw new CamelliaRedisException("ERR keys in request not in same resources");
            }
            url = writeResources.get(0).getUrl();
        }
        ICamelliaRedis redis = CamelliaRedisInitializer.init(new Resource(url), env);
        return redis.getJedis(keys[0]);
    }


    /**
     * 上层业务使用完毕后，请自行close jedis
     */
    @Override
    public Jedis getReadJedis(byte[]... keys) {
        if (keys == null || keys.length == 0) {
            throw new CamelliaRedisException("keys is null or empty");
        }
        ResourceSelector selector = factory.getResourceSelector();
        String url = null;
        for (byte[] key : keys) {
            Resource readResource = selector.getReadResource(key);
            if (url != null && !url.equalsIgnoreCase(readResource.getUrl())) {
                throw new CamelliaRedisException("ERR keys in request not in same resources");
            }
            url = readResource.getUrl();
        }
        ICamelliaRedis redis = CamelliaRedisInitializer.init(new Resource(url), env);
        return redis.getJedis(keys[0]);
    }

    /**
     * 上层业务使用完毕后，请自行close jedis
     */
    @Override
    public Jedis getWriteJedis(String... keys) {
        return getWriteJedis(SafeEncoder.encodeMany(keys));
    }

    /**
     * 上层业务使用完毕后，请自行close jedis
     */
    @Override
    public Jedis getReadJedis(String... keys) {
        return getReadJedis(SafeEncoder.encodeMany(keys));
    }

    /**
     * 上层业务使用完毕后，请自行close jedis
     */
    @Override
    public List<Jedis> getJedisList() {
        ResourceSelector selector = factory.getResourceSelector();
        Set<Resource> allResources = selector.getAllResources();
        List<Jedis> jedisList = new ArrayList<>();
        for (Resource resource : allResources) {
            ICamelliaRedis redis = CamelliaRedisInitializer.init(resource, env);
            List<Jedis> list = redis.getJedisList();
            if (list != null && !list.isEmpty()) {
                jedisList.addAll(list);
            }
        }
        return jedisList;
    }

    /**
     * 上层业务使用完毕后，请自行close jedis
     */
    @Override
    public List<Jedis> getWriteJedisList() {
        ResourceSelector selector = factory.getResourceSelector();
        List<Resource> allResources = selector.getAllWriteResources();
        List<Jedis> jedisList = new ArrayList<>();
        for (Resource resource : allResources) {
            ICamelliaRedis redis = CamelliaRedisInitializer.init(resource, env);
            List<Jedis> list = redis.getJedisList();
            if (list != null && !list.isEmpty()) {
                jedisList.addAll(list);
            }
        }
        return jedisList;
    }

    /**
     * 上层业务使用完毕后，请自行close jedis
     */
    @Override
    public List<Jedis> getReadJedisList() {
        ResourceSelector selector = factory.getResourceSelector();
        List<Resource> allResources = selector.getAllReadResources();
        List<Jedis> jedisList = new ArrayList<>();
        for (Resource resource : allResources) {
            ICamelliaRedis redis = CamelliaRedisInitializer.init(resource, env);
            List<Jedis> list = redis.getJedisList();
            if (list != null && !list.isEmpty()) {
                jedisList.addAll(list);
            }
        }
        return jedisList;
    }

    @Override
    public <T> T executeRead(CamelliaRedisCommandTask<T> task, String... keys) {
        return executeRead(task, SafeEncoder.encodeMany(keys));
    }

    @Override
    public <T> T executeRead(CamelliaRedisCommandTask<T> task, byte[]... keys) {
        Jedis jedis = getReadJedis(keys);
        try {
            return task.execute(jedis);
        } finally {
            CloseUtil.closeQuietly(jedis);
        }
    }

    @Override
    public <T> T executeWrite(CamelliaRedisCommandTask<T> task, String... keys) {
        return executeWrite(task, SafeEncoder.encodeMany(keys));
    }

    @Override
    public <T> T executeWrite(CamelliaRedisCommandTask<T> task, byte[]... keys) {
        if (keys == null || keys.length == 0) {
            throw new CamelliaRedisException("keys is null or empty");
        }
        ResourceSelector selector = factory.getResourceSelector();
        Map<String, Resource> writeResourceMap = new HashMap<>();
        for (byte[] key : keys) {
            List<Resource> resources = selector.getWriteResources(key);
            if (writeResourceMap.isEmpty()) {
                for (Resource resource : resources) {
                    writeResourceMap.put(resource.getUrl(), resource);
                }
            } else {
                if (writeResourceMap.size() != resources.size()) {
                    throw new CamelliaRedisException("ERR keys in request not in same resources");
                }
                for (Resource resource : resources) {
                    if (!writeResourceMap.containsKey(resource.getUrl())) {
                        throw new CamelliaRedisException("ERR keys in request not in same resources");
                    }
                }
            }
        }

        T result = null;
        for (String url : writeResourceMap.keySet()) {
            ICamelliaRedis redis = CamelliaRedisInitializer.init(new Resource(url), env);
            //只取第一个key，如果是redis-cluster，需要业务自己保证多个key的情况下是分布在相同的redis-node上的，否则会报错
            Jedis jedis = redis.getJedis(keys[0]);
            try {
                if (result == null) {
                    result = task.execute(jedis);
                } else {
                    task.execute(jedis);
                }
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
        return result;
    }

    private interface WriteInvoker {
        Object invoke(Resource resource, ICamelliaRedis redis);
    }

    private Object writeInvoke(WriteInvoker writeInvoker, int keyCount, byte[]... params) {
        if (params.length < keyCount) {
            throw new CamelliaRedisException("keyCount/params not match");
        }
        ResourceSelector selector = factory.getResourceSelector();
        List<Resource> writeResources;
        if (keyCount < 0) {
            throw new CamelliaRedisException("ERR Number of keys can't be negative");
        }
        if (keyCount == 0) {
            writeResources = selector.getWriteResources(new byte[0]);
        } else if (keyCount == 1) {
            writeResources = selector.getWriteResources(params[0]);
        } else {
            writeResources = selector.getWriteResources(params[0]);
            for (int i = 1; i < keyCount; i++) {
                List<Resource> resources = selector.getWriteResources(params[i]);
                if (writeResources.size() != resources.size()) {
                    throw new CamelliaRedisException("ERR keys in request not in same resources");
                }
                for (int j = 0; j < writeResources.size(); j++) {
                    Resource resource1 = writeResources.get(j);
                    Resource resource2 = resources.get(j);
                    if (!resource1.getUrl().equals(resource2.getUrl())) {
                        throw new CamelliaRedisException("ERR keys in request not in same resources");
                    }
                }
            }
        }
        Object result = null;
        for (Resource resource : writeResources) {
            ICamelliaRedis redis = CamelliaRedisInitializer.init(resource, env);
            if (result == null) {
                result = writeInvoker.invoke(resource, redis);
            } else {
                writeInvoker.invoke(resource, redis);
            }
        }
        return result;
    }

    private static class ApiServiceWrapper implements CamelliaApi {

        private final CamelliaApi service;
        private final CamelliaRedisEnv env;

        ApiServiceWrapper(CamelliaApi service, CamelliaRedisEnv camelliaRedisEnv) {
            this.service = service;
            this.env = camelliaRedisEnv;
        }

        @Override
        public CamelliaApiResponse getResourceTable(Long bid,
                                                    String bgroup,
                                                    String md5) {
            CamelliaApiResponse response = service.getResourceTable(bid, bgroup, md5);
            ResourceTable resourceTable = ResourceTransferUtil.transfer(response.getResourceTable(), resource -> {
                ResourceWrapper resourceWrapper = new ResourceWrapper(resource);
                resourceWrapper.setEnv(env);
                return resourceWrapper;
            });
            response.setResourceTable(resourceTable);
            return response;
        }

        @Override
        public CamelliaApiV2Response getResourceTableV2(Long bid, String bgroup, String md5) {
            return ResourceTableUtil.toV2Response(getResourceTable(bid, bgroup, md5));
        }

        @Override
        public boolean reportStats(ResourceStats resourceStats) {
            return service.reportStats(resourceStats);
        }

    }
}
