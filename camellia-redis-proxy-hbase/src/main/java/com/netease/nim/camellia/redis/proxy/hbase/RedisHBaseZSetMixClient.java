package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.core.util.SysUtils;
import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import com.netease.nim.camellia.redis.proxy.hbase.model.HBase2RedisRebuildResult;
import com.netease.nim.camellia.redis.proxy.hbase.model.KeyStatus;
import com.netease.nim.camellia.redis.proxy.hbase.model.SetFromList;
import com.netease.nim.camellia.redis.proxy.hbase.model.RedisHBaseType;
import com.netease.nim.camellia.redis.proxy.hbase.monitor.ReadOpeType;
import com.netease.nim.camellia.redis.proxy.hbase.monitor.RedisHBaseMonitor;
import com.netease.nim.camellia.redis.proxy.hbase.monitor.WriteOpeType;
import com.netease.nim.camellia.redis.proxy.hbase.util.FreqUtil;
import com.netease.nim.camellia.redis.proxy.util.BytesKey;
import com.netease.nim.camellia.redis.proxy.util.ErrorHandlerUtil;
import com.netease.nim.camellia.redis.toolkit.localcache.LocalCache;
import com.netease.nim.camellia.redis.toolkit.lock.CamelliaRedisLock;
import io.netty.util.HashedWheelTimer;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Response;
import redis.clients.jedis.Tuple;
import redis.clients.util.SafeEncoder;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

import static com.netease.nim.camellia.redis.proxy.hbase.util.RedisHBaseUtil.*;

/**
 *
 * Created by caojiajun on 2020/2/24.
 */
public class RedisHBaseZSetMixClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisHBaseZSetMixClient.class);

    private final CamelliaRedisTemplate redisTemplate;
    private final CamelliaHBaseTemplate hBaseTemplate;
    private final HBaseWriteAsyncExecutor hBaseWriteAsyncExecutor;
    private final LongAdder timerCount = new LongAdder();
    private final HashedWheelTimer timer = new HashedWheelTimer(new CamelliaThreadFactory("cache-rebuild-timer"));
    private final ExecutorService exec = new ThreadPoolExecutor(SysUtils.getCpuNum(),
            SysUtils.getCpuNum(), 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100000), new CamelliaThreadFactory("cache-rebuild-exec"));

    public RedisHBaseZSetMixClient(CamelliaRedisTemplate redisTemplate, CamelliaHBaseTemplate hBaseTemplate, HBaseWriteAsyncExecutor hBaseWriteAsyncExecutor) {
        this.hBaseTemplate = hBaseTemplate;
        this.redisTemplate = redisTemplate;
        this.hBaseWriteAsyncExecutor = hBaseWriteAsyncExecutor;
    }

    /**
     *
     */
    public Long del(byte[]... keys) {
        List<byte[]> toDeleteRedisKeyList = new ArrayList<>();
        List<Delete> toDeleteHBaseList = new ArrayList<>();
        Map<BytesKey, List<Delete>> deleteMap = new HashMap<>();
        for (byte[] key : keys) {
            toDeleteRedisKeyList.add(redisKey(key));
            Delete delete1 = new Delete(buildRowKey(key));
            if (RedisHBaseConfiguration.hbaseWALAsyncEnable()) {
                delete1.setDurability(Durability.ASYNC_WAL);
            }
            toDeleteHBaseList.add(delete1);
            List<Delete> list = deleteMap.computeIfAbsent(new BytesKey(key), k -> new ArrayList<>());
            Delete delete2 = new Delete(buildRowKey(key));
            if (RedisHBaseConfiguration.hbaseWALAsyncEnable()) {
                delete2.setDurability(Durability.ASYNC_WAL);
            }
            list.add(delete2);
            try {
                Set<byte[]> zrange = zrange(key, 0, -1);
                if (zrange != null && !zrange.isEmpty()) {
                    for (byte[] value : zrange) {
                        byte[] valueRefKey = buildValueRefKey(key, value);
                        toDeleteRedisKeyList.add(redisKey(valueRefKey));
                        Delete delete3 = new Delete(valueRefKey);
                        if (RedisHBaseConfiguration.hbaseWALAsyncEnable()) {
                            delete3.setDurability(Durability.ASYNC_WAL);
                        }
                        toDeleteHBaseList.add(delete3);
                        Delete delete4 = new Delete(valueRefKey);
                        if (RedisHBaseConfiguration.hbaseWALAsyncEnable()) {
                            delete4.setDurability(Durability.ASYNC_WAL);
                        }
                        list.add(delete4);
                    }
                }
            } catch (Exception e) {
                logger.error("zrange for delete error, key = {}, ex = {}", SafeEncoder.encode(key), e.toString());
            }
        }
        if (!toDeleteRedisKeyList.isEmpty()) {
            redisTemplate.del(toDeleteRedisKeyList.toArray(new byte[0][0]));
        }
        if (RedisHBaseConfiguration.hbaseWriteOpeAsyncEnable()) {
            List<Delete> list = new ArrayList<>();
            for (Map.Entry<BytesKey, List<Delete>> entry : deleteMap.entrySet()) {
                BytesKey redisKey = entry.getKey();
                List<Delete> deleteList = entry.getValue();
                if (checkRedisKeyExists(redisTemplate, redisKey.getKey())) {
                    hBaseWriteAsyncExecutor.delete(redisKey.getKey(), deleteList);
                } else {
                    list.addAll(deleteList);
                }
            }
            if (!list.isEmpty()) {
                List<List<Delete>> split = split(list, RedisHBaseConfiguration.hbaseWriteBatchMaxSize());
                for (List<Delete> deleteList : split) {
                    hBaseTemplate.delete(RedisHBaseConfiguration.hbaseTableName(), deleteList);
                }
            }
        } else {
            List<List<Delete>> split = split(toDeleteHBaseList, RedisHBaseConfiguration.hbaseWriteBatchMaxSize());
            for (List<Delete> deleteList : split) {
                hBaseTemplate.delete(RedisHBaseConfiguration.hbaseTableName(), deleteList);
            }
        }
        return (long) keys.length;
    }

    /**
     *
     */
    private static final String zadd_method = "zadd(byte[],Map)";
    public Long zadd(byte[] key, Map<byte[], Double> scoreMembers) {
        CamelliaRedisLock redisLock = null;
        try {
            List<Put> putList = new ArrayList<>();
            Long ret = null;
            boolean cacheExists = false;
            if (checkRedisKeyExists(redisTemplate, key)) {//说明存在
                ret = _redis_zadd(key, scoreMembers, putList);
                cacheExists = true;
            }
            if (!cacheExists) {
                redisLock = CamelliaRedisLock.newLock(redisTemplate, lockKey(key), RedisHBaseConfiguration.lockAcquireTimeoutMillis(), RedisHBaseConfiguration.lockExpireMillis());
                if (!redisLock.tryLock()) {
                    boolean lock = redisLock.lock();
                    if (!lock) {
                        logger.warn("zadd lock fail, key = {}", SafeEncoder.encode(key));
                        if (RedisHBaseConfiguration.errorIfLockFail()) {
                            throw new CamelliaRedisException("zadd lock fail, please retry");
                        }
                        RedisHBaseMonitor.incrHBaseDegradedCount("zadd_lock_fail");
                    }
                    if (lock) {
                        cacheExists = checkRedisKeyExists(redisTemplate, key);
                        if (cacheExists) {
                            ret = _redis_zadd(key, scoreMembers, putList);
                        }
                    }
                }
            }
            if (cacheExists) {
                RedisHBaseMonitor.incrWrite(zadd_method, WriteOpeType.REDIS_HIT);
            } else {
                RedisHBaseMonitor.incrWrite(zadd_method, WriteOpeType.HBASE_ONLY);
            }
            if (!cacheExists) {
                if (RedisHBaseConfiguration.zsetHBaseDegradedAsync()) {
                    submitCacheRebuildDelayTask(key);
                    RedisHBaseMonitor.incrHBaseDegradedCount("zadd_delay_task");
                } else {
                    byte[] freqKey = hbaseGetFreqKey(key);
                    boolean pass = FreqUtil.freq(redisTemplate, freqKey,
                            RedisHBaseConfiguration.hbaseGetFreqThreshold(), RedisHBaseConfiguration.hbaseGetFreqMillis());
                    if (pass) {
                        if (FreqUtil.hbaseGetStandaloneFreqOfWrite()) {
                            checkZSetExists(key);
                        } else {
                            logger.warn("zadd hbaseGetStandaloneFreq fail, will submit cache rebuild delay task, key = {}", SafeEncoder.encode(key));
                            submitCacheRebuildDelayTask(key);
                            RedisHBaseMonitor.incrHBaseDegradedCount("zadd_hbase_get_standalone_freq");
                        }
                    } else {
                        logger.warn("zadd freq fail, will submit cache rebuild delay task, key = {}", SafeEncoder.encode(key));
                        submitCacheRebuildDelayTask(key);
                        RedisHBaseMonitor.incrHBaseDegradedCount("zadd_freq_fail_delay_task");
                    }
                }
            }
            Put put = new Put(buildRowKey(key));
            if (RedisHBaseConfiguration.hbaseWALAsyncEnable()) {
                put.setDurability(Durability.ASYNC_WAL);
            }
            for (Map.Entry<byte[], Double> entry : scoreMembers.entrySet()) {
                byte[] value = entry.getKey();
                Double score = entry.getValue();
                put.addColumn(CF_D, dataQualifier(value), Bytes.toBytes(score));
            }
            put.addColumn(CF_D, COL_TYPE, RedisHBaseType.ZSET.raw());
            putList.add(put);
            if (cacheExists && RedisHBaseConfiguration.hbaseWriteOpeAsyncEnable()) {
                hBaseWriteAsyncExecutor.put(key, putList);
            } else {
                if (RedisHBaseConfiguration.zsetHBaseDegradedAsync()) {
                    hBaseWriteAsyncExecutor.put(key, putList);
                } else {
                    List<List<Put>> split = split(putList, RedisHBaseConfiguration.hbaseWriteBatchMaxSize());
                    for (List<Put> puts : split) {
                        hBaseTemplate.put(RedisHBaseConfiguration.hbaseTableName(), puts);
                    }
                }
            }
            if (ret != null) {
                return ret;
            } else {
                return (long) scoreMembers.size();
            }
        } finally {
            if (redisLock != null) {
                redisLock.release();
            }
            boolean cacheNull = RedisHBaseConfiguration.isHBaseCacheNull();
            if (cacheNull) {
                redisTemplate.setex(nullCacheKey(key), RedisHBaseConfiguration.notNullCacheExpireSeconds(), NULL_CACHE_NO);
            }

        }
    }

    /**
     *
     */
    private static final String zcard_method = "zcard(byte[])";
    public Long zcard(byte[] key) {
        Long zcard = redisTemplate.zcard(redisKey(key));
        if (zcard != null && zcard > 0) {
            RedisHBaseMonitor.incrRead(zcard_method, ReadOpeType.REDIS_ONLY);
            return zcard;
        } else {
            KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zcard_method);
            if (keyStatus == KeyStatus.NULL) {
                return 0L;
            }
            return redisTemplate.zcard(redisKey(key));
        }
    }

    /**
     *
     */
    private static final String zcount_method = "zcount(byte[],byte[],byte[])";
    public Long zcount(byte[] key, byte[] min, byte[] max) {
        Long zcount = redisTemplate.zcount(redisKey(key), min, max);
        if (zcount != null && zcount > 0) {
            RedisHBaseMonitor.incrRead(zcount_method, ReadOpeType.REDIS_ONLY);
            return zcount;
        } else {
            if (checkRedisKeyExists(redisTemplate, key)) {
                RedisHBaseMonitor.incrRead(zcount_method, ReadOpeType.REDIS_ONLY);
                return 0L;
            } else {
                HBase2RedisRebuildResult rebuildResult = rebuildZSet(key, false, false, true);
                if (rebuildFail4Read(rebuildResult, zcount_method)) {
                    return 0L;
                }
                return redisTemplate.zcount(redisKey(key), min, max);
            }
        }
    }

    /**
     *
     */
    private static final String zrem_method = "zrem(byte[],byte[][])";
    public Long zrem(byte[] key, byte[]... member) {
        RedisHBaseMonitor.incrWrite(zrem_method, WriteOpeType.REDIS_HBASE_ALL);
        if (RedisHBaseConfiguration.hbaseWriteOpeAsyncEnable()) {
            return _zrem(key, checkRedisKeyExists(redisTemplate, key), member);
        } else {
            return _zrem(key, false, member);
        }
    }

    //
    private Long _zrem(byte[] key, boolean cacheExists, byte[]... member) {
        try (ICamelliaRedisPipeline pipelined = redisTemplate.pipelined()) {
            int pipelineSize = 0;
            List<byte[]> list = new ArrayList<>(member.length * 2);
            List<Delete> deleteList = new ArrayList<>();
            Delete delete = new Delete(buildRowKey(key));
            if (RedisHBaseConfiguration.hbaseWALAsyncEnable()) {
                delete.setDurability(Durability.ASYNC_WAL);
            }
            for (byte[] bytes : member) {
                list.add(bytes);
                delete.addColumns(CF_D, dataQualifier(bytes));
                if (bytes.length > RedisHBaseConfiguration.ZSET_VALUE_REF_THRESHOLD_MIN) {
                    byte[] valueRefKey = buildValueRefKey(key, bytes);
                    pipelined.del(redisKey(valueRefKey));
                    pipelineSize ++;
                    if (pipelineSize > RedisHBaseConfiguration.redisPipelineMaxBatchSize()) {
                        pipelined.sync();
                        pipelineSize = 0;
                    }
                    list.add(valueRefKey);
                    Delete delete1 = new Delete(valueRefKey);
                    if (RedisHBaseConfiguration.hbaseWALAsyncEnable()) {
                        delete1.setDurability(Durability.ASYNC_WAL);
                    }
                    deleteList.add(delete1);
                }
            }
            deleteList.add(delete);
            Response<Long> zrem = pipelined.zrem(redisKey(key), list.toArray(new byte[0][0]));
            pipelined.sync();
            if (RedisHBaseConfiguration.hbaseWriteOpeAsyncEnable() && cacheExists) {
                hBaseWriteAsyncExecutor.delete(key, deleteList);
            } else {
                List<List<Delete>> split = split(deleteList, RedisHBaseConfiguration.hbaseWriteBatchMaxSize());
                for (List<Delete> deletes : split) {
                    hBaseTemplate.delete(RedisHBaseConfiguration.hbaseTableName(), deletes);
                }
            }
            return zrem.get();
        }
    }

    /**
     *
     */
    private static final String zlexcount_method = "zlexcount(byte[],byte[],byte[])";
    public Long zlexcount(byte[] key, byte[] min, byte[] max) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zlexcount_method);
        if (keyStatus == KeyStatus.NULL) {
            return 0L;
        }
        return redisTemplate.zlexcount(redisKey(key), min, max);
    }

    /**
     *
     */
    private static final String zrange_method = "zrange(byte[],long,long)";
    public Set<byte[]> zrange(byte[] key, long start, long end) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrange_method);
        if (keyStatus == KeyStatus.NULL) {
            return new HashSet<>();
        }
        Set<byte[]> set = redisTemplate.zrange(redisKey(key), start, end);
        return checkAndGetOriginalSet(key, set, true);
    }

    /**
     *
     */
    private static final String zrevrange_method = "zrevrange(byte[],long,long)";
    public Set<byte[]> zrevrange(byte[] key, long start, long end) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrevrange_method);
        if (keyStatus == KeyStatus.NULL) {
            return new HashSet<>();
        }
        Set<byte[]> set = redisTemplate.zrevrange(redisKey(key), start, end);
        return checkAndGetOriginalSet(key, set, true);
    }

    /**
     *
     */
    private static final String zrevrangeByScoreWithScores_offset_method = "zrevrangeByScoreWithScores(byte[],byte[],byte[],int,int)";
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrevrangeByScoreWithScores_offset_method);
        if (keyStatus == KeyStatus.NULL) {
            return new HashSet<>();
        }
        Set<Tuple> set = redisTemplate.zrevrangeByScoreWithScores(redisKey(key), max, min, offset, count);
        return checkAndGetOriginalTupleSet(key, set, true);
    }

    /**
     *
     */
    private static final String zrangeByLex_offset_method = "zrangeByLex(byte[],bye[],byte[],int,int)";
    public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrangeByLex_offset_method);
        if (keyStatus == KeyStatus.NULL) {
            return new HashSet<>();
        }
        Set<byte[]> set = redisTemplate.zrangeByLex(redisKey(key), min, max, offset, count);
        return checkAndGetOriginalSet(key, set, true);
    }

    /**
     *
     */
    private static final String zrangeByLex_method = "zrangeByLex(byte[],byte[],byte[])";
    public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrangeByLex_method);
        if (keyStatus == KeyStatus.NULL) {
            return new HashSet<>();
        }
        Set<byte[]> set = redisTemplate.zrangeByLex(redisKey(key), min, max);
        return checkAndGetOriginalSet(key, set, true);
    }

    /**
     *
     */
    private static final String zrevrangeByLex_method = "zrevrangeByLex(byte[],byte[],byte[])";
    public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrevrangeByLex_method);
        if (keyStatus == KeyStatus.NULL) {
            return new HashSet<>();
        }
        Set<byte[]> set = redisTemplate.zrevrangeByLex(redisKey(key), max, min);
        return checkAndGetOriginalSet(key, set, true);
    }

    /**
     *
     */
    private static final String zrevrangeByLex_offset_method = "zrevrangeByLex(byte[],bye[],byte[],int,int)";
    public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrevrangeByLex_offset_method);
        if (keyStatus == KeyStatus.NULL) {
            return new HashSet<>();
        }
        Set<byte[]> set = redisTemplate.zrevrangeByLex(redisKey(key), max, min, offset, count);
        return checkAndGetOriginalSet(key, set, true);
    }

    /**
     *
     */
    private static final String zrevrangeByScoreWithScores_method = "zrevrangeByScoreWithScores(byte[],byte[],byte[])";
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrevrangeByScoreWithScores_method);
        if (keyStatus == KeyStatus.NULL) {
            return new HashSet<>();
        }
        Set<Tuple> set = redisTemplate.zrevrangeByScoreWithScores(redisKey(key), max, min);
        return checkAndGetOriginalTupleSet(key, set, true);
    }

    /**
     *
     */
    private static final String zrevrangeByScore_offset_method = "zrevrangeByScore(byte[],byte[],byte[],int,int)";
    public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrevrangeByScore_offset_method);
        if (keyStatus == KeyStatus.NULL) {
            return new HashSet<>();
        }
        Set<byte[]> set = redisTemplate.zrevrangeByScore(redisKey(key), max, min, offset, count);
        return checkAndGetOriginalSet(key, set, true);
    }

    /**
     *
     */
    private static final String zrevrangeByScore_method = "zrevrangeByScore(byte[],byte[],byte[])";
    public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrevrangeByScore_method);
        if (keyStatus == KeyStatus.NULL) {
            return new HashSet<>();
        }
        Set<byte[]> set = redisTemplate.zrevrangeByScore(redisKey(key), max, min);
        return checkAndGetOriginalSet(key, set, true);
    }

    /**
     *
     */
    private static final String zrevrangeWithScores_method = "zrevrangeWithScores(byte[],int,int)";
    public Set<Tuple> zrevrangeWithScores(byte[] key, long start, long end) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrevrangeWithScores_method);
        if (keyStatus == KeyStatus.NULL) {
            return new HashSet<>();
        }
        Set<Tuple> set = redisTemplate.zrevrangeWithScores(redisKey(key), start, end);
        return checkAndGetOriginalTupleSet(key, set, true);
    }

    /**
     *
     */
    private static final String zrangeWithScores_method = "zrangeWithScores(byte[],long,long)";
    public Set<Tuple> zrangeWithScores(byte[] key, long start, long end) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrangeWithScores_method);
        if (keyStatus == KeyStatus.NULL) {
            return new HashSet<>();
        }
        Set<Tuple> set = redisTemplate.zrangeWithScores(redisKey(key), start, end);
        return checkAndGetOriginalTupleSet(key, set, true);
    }

    /**
     *
     */
    private static final String zrangeByScore_method = "zrangeByScore(byte[],byte[],byte[])";
    public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrangeByScore_method);
        if (keyStatus == KeyStatus.NULL) {
            return new HashSet<>();
        }
        Set<byte[]> set = redisTemplate.zrangeByScore(redisKey(key), min, max);
        return checkAndGetOriginalSet(key, set, true);
    }

    /**
     *
     */
    private static final String zrangeByScore_offset_method = "zrangeByScore(byte[],byte[],byte[],int,int)";
    public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrangeByScore_offset_method);
        if (keyStatus == KeyStatus.NULL) {
            return new HashSet<>();
        }
        Set<byte[]> set = redisTemplate.zrangeByScore(redisKey(key), min, max, offset, count);
        return checkAndGetOriginalSet(key, set, true);
    }

    /**
     *
     */
    private static final String zrangeByScoreWithScores_method = "zrangeByScoreWithScores(byte[],byte[],byte[])";
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrangeByScoreWithScores_method);
        if (keyStatus == KeyStatus.NULL) {
            return new HashSet<>();
        }
        Set<Tuple> set = redisTemplate.zrangeByScoreWithScores(redisKey(key), min, max);
        return checkAndGetOriginalTupleSet(key, set, true);
    }

    /**
     *
     */
    private static final String zscore_method = "zscore(byte[],byte[])";
    public Double zscore(byte[] key, byte[] member)  {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zscore_method);
        if (keyStatus == KeyStatus.NULL) {
            return null;
        }
        if (member.length > RedisHBaseConfiguration.zsetValueRefThreshold()) {
            byte[] valueRefKey = buildValueRefKey(key, member);
            Double score = redisTemplate.zscore(redisKey(key), valueRefKey);
            if (score != null) {
                return score;
            }
            return redisTemplate.zscore(redisKey(key), member);
        } else {
            Double score = redisTemplate.zscore(redisKey(key), member);
            if (score != null) {
                return score;
            }
            byte[] valueRefKey = buildValueRefKey(key, member);
            return redisTemplate.zscore(redisKey(key), valueRefKey);
        }
    }

    /**
     *
     */
    private static final String zincrby_method = "zincrby(byte[],double,byte[])";
    public Double zincrby(byte[] key, double score, byte[] member) {
        CamelliaRedisLock redisLock = CamelliaRedisLock.newLock(redisTemplate, lockKey(key), RedisHBaseConfiguration.lockAcquireTimeoutMillis(), RedisHBaseConfiguration.lockExpireMillis());
        try {
            boolean lock = redisLock.lock();
            if (!lock) {
                logger.warn("zincrby lock fail, key = {}", SafeEncoder.encode(key));
                if (RedisHBaseConfiguration.errorIfLockFail()) {
                    throw new CamelliaRedisException("zincrby lock fail, please retry");
                }
                RedisHBaseMonitor.incrHBaseDegradedCount("zincrby_lock_fail");
            }
            KeyStatus keyStatus = checkCacheAndRebuild4Write(key, zincrby_method);
            if (keyStatus == KeyStatus.NULL) {
                //说明是一个新的zset
                List<Put> putList = new ArrayList<>();
                Map<byte[], Double> scoreMembers = new HashMap<>();
                scoreMembers.put(member, score);
                _redis_zadd(key, scoreMembers, putList);
                Put put = new Put(buildRowKey(key));
                if (RedisHBaseConfiguration.hbaseWALAsyncEnable()) {
                    put.setDurability(Durability.ASYNC_WAL);
                }
                put.addColumn(CF_D, dataQualifier(member), Bytes.toBytes(score));
                put.addColumn(CF_D, COL_TYPE, RedisHBaseType.ZSET.raw());
                putList.add(put);
                List<List<Put>> split = split(putList, RedisHBaseConfiguration.hbaseWriteBatchMaxSize());
                for (List<Put> puts : split) {
                    hBaseTemplate.put(RedisHBaseConfiguration.hbaseTableName(), puts);
                }
                return score;
            } else {
                return _zincrby(key, score, member);
            }
        } finally {
            redisLock.release();
            boolean cacheNull = RedisHBaseConfiguration.isHBaseCacheNull();
            if (cacheNull) {
                redisTemplate.setex(nullCacheKey(key), RedisHBaseConfiguration.notNullCacheExpireSeconds(), NULL_CACHE_NO);
            }
        }
    }

    private Double _zincrby(byte[] key, double score, byte[] member) {
        List<Put> putList = new ArrayList<>();
        Double finalScore = null;
        if (member.length > RedisHBaseConfiguration.zsetValueRefThreshold()) {
            byte[] valueRefKey = buildValueRefKey(key, member);
            Double zscore = redisTemplate.zscore(redisKey(key), valueRefKey);
            if (zscore != null) {
                finalScore = redisTemplate.zincrby(redisKey(key), score, valueRefKey);
            } else {
                zscore = redisTemplate.zscore(redisKey(key), member);
                if (zscore != null) {
                    finalScore = redisTemplate.zincrby(redisKey(key), score, member);
                }
            }
        } else {
            Double zscore = redisTemplate.zscore(redisKey(key), member);
            if (zscore != null) {
                finalScore = redisTemplate.zincrby(redisKey(key), score, member);
            } else {
                byte[] valueRefKey = buildValueRefKey(key, member);
                zscore = redisTemplate.zscore(redisKey(key), valueRefKey);
                if (zscore != null) {
                    finalScore = redisTemplate.zincrby(redisKey(key), score, valueRefKey);
                }
            }
        }
        if (finalScore == null) {//说明是一个新的member
            finalScore = score;
            Map<byte[], Double> scoreMembers = new HashMap<>();
            scoreMembers.put(member, score);
            _redis_zadd(key, scoreMembers, putList);
        }
        Put put = new Put(buildRowKey(key));
        if (RedisHBaseConfiguration.hbaseWALAsyncEnable()) {
            put.setDurability(Durability.ASYNC_WAL);
        }
        put.addColumn(CF_D, member, Bytes.toBytes(finalScore));
        putList.add(put);
        if (RedisHBaseConfiguration.hbaseWriteOpeAsyncEnable()) {
            hBaseWriteAsyncExecutor.put(key, putList);
        } else {
            List<List<Put>> split = split(putList, RedisHBaseConfiguration.hbaseWriteBatchMaxSize());
            for (List<Put> puts : split) {
                hBaseTemplate.put(RedisHBaseConfiguration.hbaseTableName(), puts);
            }
        }
        return finalScore;
    }

    /**
     *
     */
    private static final String zrangeByScoreWithScores_offset_method = "zrangeByScoreWithScores(byte[],byte[],byte[],int,int)";
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrangeByScoreWithScores_offset_method);
        if (keyStatus == KeyStatus.NULL) {
            return new HashSet<>();
        }
        Set<Tuple> set = redisTemplate.zrangeByScoreWithScores(redisKey(key), min, max, offset, count);
        return checkAndGetOriginalTupleSet(key, set, true);
    }

    /**
     *
     */
    private static final String zremrangeByRank_method = "zremrangeByRank(byte[],long,long)";
    public Long zremrangeByRank(byte[] key, long start, long stop) {
        KeyStatus keyStatus = checkCacheAndRebuild4Write(key, zremrangeByRank_method);
        if (keyStatus == KeyStatus.NULL) {
            return 0L;
        }
        return _zremrangeByRank(key, start, stop);
    }

    //
    private long _zremrangeByRank(byte[] key, long start, long stop) {
        Set<byte[]> set = redisTemplate.zrange(redisKey(key), start, stop);
        set = checkAndGetOriginalSet(key, set, false);
        if (!set.isEmpty()) {
            _zrem(key, true, set.toArray(new byte[0][0]));
        }
        return set.size();
    }

    /**
     *
     */
    private static final String zremrangeByScore_method = "zremrangeByScore(byte[],byte[],byte[])";
    public Long zremrangeByScore(byte[] key, byte[] start, byte[] end) {
        KeyStatus keyStatus = checkCacheAndRebuild4Write(key, zremrangeByScore_method);
        if (keyStatus == KeyStatus.NULL) {
            return 0L;
        }
        return _zremrangeByScore(key, start, end);
    }

    //
    private long _zremrangeByScore(byte[] key, byte[] start, byte[] end) {
        Set<byte[]> set = redisTemplate.zrangeByScore(redisKey(key), start, end);
        set = checkAndGetOriginalSet(key, set, false);
        if (!set.isEmpty()) {
            _zrem(key, true, set.toArray(new byte[0][0]));
        }
        return set.size();
    }

    /**
     *
     */
    private static final String zremrangeByLex_method = "zremrangeByLex(byte[],byte[],byte[])";
    public Long zremrangeByLex(byte[] key, byte[] min, byte[] max) {
        KeyStatus keyStatus = checkCacheAndRebuild4Write(key, zremrangeByLex_method);
        if (keyStatus == KeyStatus.NULL) {
            return 0L;
        }
        return _zremrangeByLex(key, min, max);
    }

    //
    private long _zremrangeByLex(byte[] key, byte[] min, byte[] max) {
        Set<byte[]> set = redisTemplate.zrangeByLex(redisKey(key), min, max);
        set = checkAndGetOriginalSet(key, set, false);
        if (!set.isEmpty()) {
            _zrem(key, true, set.toArray(new byte[0][0]));
        }
        return set.size();
    }

    /**
     *
     */
    private static final String zrank_method = "zrank(byte[],byte[])";
    public Long zrank(byte[] key, byte[] member) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrank_method);
        if (keyStatus == KeyStatus.NULL) {
            return null;
        }
        return _zrank(key, member);
    }

    private Long _zrank(byte[] key, byte[] member) {
        if (member.length > RedisHBaseConfiguration.zsetValueRefThreshold()) {
            byte[] valueRefKey = buildValueRefKey(key, member);
            Long zrank = redisTemplate.zrank(redisKey(key), valueRefKey);
            if (zrank != null) {
                return zrank;
            }
            zrank = redisTemplate.zrank(redisKey(key), member);
            return zrank;
        } else {
            Long zrank = redisTemplate.zrank(redisKey(key), member);
            if (zrank != null) {
                return zrank;
            }
            byte[] valueRefKey = buildValueRefKey(key, member);
            zrank = redisTemplate.zrank(redisKey(key), valueRefKey);
            return zrank;
        }
    }

    /**
     *
     */
    private static final String zrevrank_method = "zrevrank(byte[],byte[])";
    public Long zrevrank(byte[] key, byte[] member) {
        KeyStatus keyStatus = checkCacheAndRebuild4Read(key, zrevrank_method);
        if (keyStatus == KeyStatus.NULL) {
            return null;
        }
        return _zrevrank(key, member);
    }

    private Long _zrevrank(byte[] key, byte[] member) {
        if (member.length > RedisHBaseConfiguration.zsetValueRefThreshold()) {
            byte[] valueRefKey = buildValueRefKey(key, member);
            Long zrank = redisTemplate.zrevrank(redisKey(key), valueRefKey);
            if (zrank != null) {
                return zrank;
            }
            return redisTemplate.zrevrank(redisKey(key), member);
        } else {
            Long zrank = redisTemplate.zrevrank(redisKey(key), member);
            if (zrank != null) {
                return zrank;
            }
            byte[] valueRefKey = buildValueRefKey(key, member);
            return redisTemplate.zrevrank(redisKey(key), valueRefKey);
        }
    }

    //
    private static final String zmember_valueRef_4_read = "zmember_valueRef_4_read";
    private static final String zmember_valueRef_4_write = "zmember_valueRef_4_write";
    private Set<Tuple> checkAndGetOriginalTupleSet(byte[] key, Set<Tuple> tupleSet, boolean forRead) {
        if (tupleSet.isEmpty()) return tupleSet;
        Map<BytesKey, Tuple> map = new HashMap<>();
        List<byte[]> valueRefKeyList = new ArrayList<>();
        Map<BytesKey, Tuple> tmpMap = new HashMap<>();
        for (Tuple tuple : tupleSet) {
            byte[] value = tuple.getBinaryElement();
            boolean valueRefKey = isValueRefKey(key, value);
            BytesKey redisKey = new BytesKey(tuple.getBinaryElement());
            if (valueRefKey) {
                valueRefKeyList.add(value);
                tmpMap.put(redisKey, tuple);
            } else {
                map.put(redisKey, tuple);
            }
        }
        if (!valueRefKeyList.isEmpty()) {
            List<byte[]> mget;
            if (valueRefKeyList.size() > RedisHBaseConfiguration.redisMGetMaxBatchSize()) {
                mget = new ArrayList<>(valueRefKeyList.size());
                List<List<byte[]>> split = split(valueRefKeyList, RedisHBaseConfiguration.redisMGetMaxBatchSize());
                for (List<byte[]> subList : split) {
                    List<byte[]> subMGet = redisTemplate.mget(subList.toArray(new byte[0][0]));
                    mget.addAll(subMGet);
                }
            } else {
                mget = redisTemplate.mget(redisKey(valueRefKeyList.toArray(new byte[0][0])));
            }
            String zmember_valueRef_method = forRead ? zmember_valueRef_4_read : zmember_valueRef_4_write;
            List<Get> getList = new ArrayList<>();
            for (int i = 0; i < mget.size(); i++) {
                byte[] valueRefKey = valueRefKeyList.get(i);
                byte[] originalValue = mget.get(i);
                if (originalValue != null) {
                    BytesKey redisKey = new BytesKey(valueRefKey);
                    Tuple tuple = tmpMap.get(redisKey);
                    map.put(redisKey, new Tuple(originalValue, tuple.getScore()));
                    RedisHBaseMonitor.incrRead(zmember_valueRef_method, ReadOpeType.REDIS_ONLY);
                } else {
                    getList.add(new Get(valueRefKey));
                }
            }
            if (!getList.isEmpty()) {
                boolean pass;
                if (forRead) {
                    pass = FreqUtil.zmemberHbaseGetStandaloneFreqOfRead();
                } else {
                    pass = FreqUtil.zmemberHbaseGetStandaloneFreqOfWrite();
                }
                if (pass) {
                    Map<byte[], byte[]> cacheRebuildMap = new HashMap<>(getList.size());
                    List<List<Get>> split = split(getList, RedisHBaseConfiguration.hbaseReadBatchMaxSize());
                    Result[] results = new Result[getList.size()];
                    int index = 0;
                    for (List<Get> gets : split) {
                        Result[] resultArray = hBaseTemplate.get(RedisHBaseConfiguration.hbaseTableName(), gets);
                        for (Result result : resultArray) {
                            results[index] = result;
                            index++;
                        }
                    }
                    for (Result result : results) {
                        byte[] originalValue = result.getValue(CF_D, COL_DATA);
                        byte[] valueRefKey = result.getRow();
                        if (originalValue != null) {
                            BytesKey redisKey = new BytesKey(valueRefKey);
                            Tuple tuple = tmpMap.get(redisKey);
                            map.put(redisKey, new Tuple(originalValue, tuple.getScore()));

                            cacheRebuildMap.put(valueRefKey, originalValue);
                            RedisHBaseMonitor.incrRead(zmember_valueRef_method, ReadOpeType.HIT_TO_HBASE);
                        } else {
                            RedisHBaseMonitor.incrRead(zmember_valueRef_method, ReadOpeType.HIT_TO_HBASE_AND_MISS);
                        }
                    }
                    if (!cacheRebuildMap.isEmpty()) {
                        try (ICamelliaRedisPipeline pipelined = redisTemplate.pipelined()) {
                            int pipelineSize = 0;
                            for (Map.Entry<byte[], byte[]> entry : cacheRebuildMap.entrySet()) {
                                byte[] k = entry.getKey();
                                byte[] v = entry.getValue();
                                pipelined.setex(redisKey(k), RedisHBaseConfiguration.zsetValueRefExpireSeconds(), v);
                                pipelineSize++;
                                if (pipelineSize > RedisHBaseConfiguration.redisPipelineMaxBatchSize()) {
                                    pipelined.sync();
                                    pipelineSize = 0;
                                }
                            }
                            pipelined.sync();
                        }
                    }
                } else {
                    RedisHBaseMonitor.incrRead(zmember_valueRef_method, ReadOpeType.HIT_TO_HBASE_DEGRADED);
                }
            }
        }
        List<Tuple> ret = new ArrayList<>();
        for (Tuple tuple : tupleSet) {
            BytesKey redisKey = new BytesKey(tuple.getBinaryElement());
            Tuple originalTuble = map.get(redisKey);
            if (originalTuble != null) {
                ret.add(originalTuble);
            } else {
                logger.warn("missing tuple, key = {}, value = {}",
                        SafeEncoder.encode(key), Bytes.toHex(redisKey.getKey()));
            }
        }
        return SetFromList.of(ret);
    }

    //
    private Set<byte[]> checkAndGetOriginalSet(byte[] key, Set<byte[]> set, boolean forRead) {
        if (set.isEmpty()) return set;
        Map<BytesKey, byte[]> map = new HashMap<>();
        List<byte[]> valueRefKeyList = new ArrayList<>();
        for (byte[] value : set) {
            boolean isValueRefKey = isValueRefKey(key, value);
            if (isValueRefKey) {
                valueRefKeyList.add(value);
            } else {
                map.put(new BytesKey(value), value);
            }
        }
        if (!valueRefKeyList.isEmpty()) {
            List<byte[]> mget;
            if (valueRefKeyList.size() > RedisHBaseConfiguration.redisMGetMaxBatchSize()) {
                mget = new ArrayList<>(valueRefKeyList.size());
                List<List<byte[]>> split = split(valueRefKeyList, RedisHBaseConfiguration.redisMGetMaxBatchSize());
                for (List<byte[]> subList : split) {
                    List<byte[]> subMGet = redisTemplate.mget(subList.toArray(new byte[0][0]));
                    mget.addAll(subMGet);
                }
            } else {
                mget = redisTemplate.mget(redisKey(valueRefKeyList.toArray(new byte[0][0])));
            }
            String zmember_valueRef_method = forRead ? zmember_valueRef_4_read : zmember_valueRef_4_write;
            List<Get> getList = new ArrayList<>();
            for (int i = 0; i < valueRefKeyList.size(); i++) {
                byte[] valueRefKey = valueRefKeyList.get(i);
                byte[] value = mget.get(i);
                if (value != null) {
                    map.put(new BytesKey(valueRefKey), value);
                    RedisHBaseMonitor.incrRead(zmember_valueRef_method, ReadOpeType.REDIS_ONLY);
                } else {
                    getList.add(new Get(valueRefKey));
                }
            }
            if (!getList.isEmpty()) {
                boolean pass;
                if (forRead) {
                    pass = FreqUtil.zmemberHbaseGetStandaloneFreqOfRead();
                } else {
                    pass = FreqUtil.zmemberHbaseGetStandaloneFreqOfWrite();
                }
                if (pass) {
                    Map<byte[], byte[]> cacheRebuildMap = new HashMap<>(getList.size());

                    List<List<Get>> split = split(getList, RedisHBaseConfiguration.hbaseReadBatchMaxSize());
                    Result[] results = new Result[getList.size()];
                    int index = 0;
                    for (List<Get> gets : split) {
                        Result[] resultArray = hBaseTemplate.get(RedisHBaseConfiguration.hbaseTableName(), gets);
                        for (Result result : resultArray) {
                            results[index] = result;
                            index++;
                        }
                    }

                    for (Result result : results) {
                        byte[] originalValue = result.getValue(CF_D, COL_DATA);
                        byte[] valueRefKey = result.getRow();
                        if (originalValue != null) {
                            map.put(new BytesKey(valueRefKey), originalValue);
                            cacheRebuildMap.put(valueRefKey, originalValue);
                            RedisHBaseMonitor.incrRead(zmember_valueRef_method, ReadOpeType.HIT_TO_HBASE);
                        } else {
                            RedisHBaseMonitor.incrRead(zmember_valueRef_method, ReadOpeType.HIT_TO_HBASE_AND_MISS);
                        }
                    }
                    if (!cacheRebuildMap.isEmpty()) {
                        try (ICamelliaRedisPipeline pipelined = redisTemplate.pipelined()) {
                            int pipelineSize = 0;
                            for (Map.Entry<byte[], byte[]> entry : cacheRebuildMap.entrySet()) {
                                pipelined.setex(redisKey(entry.getKey()), RedisHBaseConfiguration.zsetValueRefExpireSeconds(), entry.getValue());
                                pipelineSize++;
                                if (pipelineSize > RedisHBaseConfiguration.redisPipelineMaxBatchSize()) {
                                    pipelined.sync();
                                    pipelineSize = 0;
                                }
                            }
                            pipelined.sync();
                        }
                    }
                } else {
                    RedisHBaseMonitor.incrRead(zmember_valueRef_method, ReadOpeType.HIT_TO_HBASE_DEGRADED);
                }
            }
        }
        List<byte[]> ret = new ArrayList<>();
        for (byte[] bytes : set) {
            BytesKey redisKey = new BytesKey(bytes);
            byte[] originalValue = map.get(redisKey);
            if (originalValue != null) {
                ret.add(originalValue);
            } else {
                logger.warn("missing value, key = {}, value = {}",
                        SafeEncoder.encode(key), Bytes.toHex(redisKey.getKey()));
            }
        }
        return SetFromList.of(ret);
    }

    //
    private HBase2RedisRebuildResult rebuildZSet(byte[] key, boolean strictConcurrent, boolean forceRebuild, boolean forRead) {
        boolean cacheNull = RedisHBaseConfiguration.isHBaseCacheNull();
        if (!forceRebuild) {
            if (cacheNull) {
                byte[] nullCacheValue = redisTemplate.get(nullCacheKey(key));
                if (nullCacheValue != null && Bytes.equals(nullCacheValue, NULL_CACHE_YES)) {
                    return HBase2RedisRebuildResult.NULL_CACHE_HIT;
                }
            }
        }
        CamelliaRedisLock redisLock = CamelliaRedisLock.newLock(redisTemplate, lockKey(key), RedisHBaseConfiguration.lockAcquireTimeoutMillis(), RedisHBaseConfiguration.lockExpireMillis());
        if (!redisLock.tryLock()) {
            boolean lock = redisLock.lock();
            if (!lock) {
                logger.warn("rebuildZSet lock fail, forRead = {}, key = {}", forRead, SafeEncoder.encode(key));
                if (strictConcurrent || RedisHBaseConfiguration.errorIfLockFail()) {
                    throw new CamelliaRedisException("rebuildZSet lock fail, please retry");
                }
                RedisHBaseMonitor.incrHBaseDegradedCount(forRead ? "zset_rebuild_lock_fail_4_read" : "zset_rebuild_lock_fail_4_write");
            }
            if (lock) {
                if (!forceRebuild) {
                    if (checkRedisKeyExists(redisTemplate, key)) {
                        return HBase2RedisRebuildResult.REBUILD_OK;
                    }
                    if (cacheNull) {
                        byte[] nullCacheValue = redisTemplate.get(nullCacheKey(key));
                        if (nullCacheValue != null && Bytes.equals(nullCacheValue, NULL_CACHE_YES)) {
                            return HBase2RedisRebuildResult.NULL_CACHE_HIT;
                        }
                    }
                }
            }
        }
        Result result;
        List<Put> putList;
        try {
            if (RedisHBaseConfiguration.zsetHBaseDegradedAsync()) {
                submitCacheRebuildDelayTask(key);
                RedisHBaseMonitor.incrHBaseDegradedCount(forRead ? "zset_rebuild_delay_task_4_read" : "zset_rebuild_delay_task_4_write");
                return HBase2RedisRebuildResult.DEGRADED;
            }
            byte[] freqKey = hbaseGetFreqKey(key);
            boolean pass = FreqUtil.freq(redisTemplate, freqKey, RedisHBaseConfiguration.hbaseGetFreqThreshold(), RedisHBaseConfiguration.hbaseGetFreqMillis());
            if (pass) {
                boolean standaloneFreqPass;
                if (forceRebuild) {
                    standaloneFreqPass = FreqUtil.hbaseGetStandaloneFreqOfRebuild();
                } else {
                    if (forRead) {
                        standaloneFreqPass = FreqUtil.hbaseGetStandaloneFreqOfRead();
                    } else {
                        standaloneFreqPass = FreqUtil.hbaseGetStandaloneFreqOfWrite();
                    }
                }
                if (standaloneFreqPass) {
                    result = checkZSetExists(key);
                } else {
                    logger.warn("zset rebuild hbaseGetStandaloneFreq fail, will submit cache rebuild delay task, forRead = {}, key = {}", forRead, SafeEncoder.encode(key));
                    submitCacheRebuildDelayTask(key);
                    RedisHBaseMonitor.incrHBaseDegradedCount(forRead ? "zset_rebuild_hbase_get_standalone_freq_4_read" : "zset_rebuild_hbase_get_standalone_freq_4_write");
                    FreqUtil.releaseFreqKey(redisTemplate, freqKey);
                    return HBase2RedisRebuildResult.DEGRADED;
                }
            } else {
                logger.warn("zset rebuild freq fail, will submit cache rebuild delay task, forRead = {}, key = {}", forRead, SafeEncoder.encode(key));
                submitCacheRebuildDelayTask(key);
                RedisHBaseMonitor.incrHBaseDegradedCount(forRead ? "zset_rebuild_freq_fail_delay_task_4_read" : "zset_rebuild_freq_fail_delay_task_4_write");
                return HBase2RedisRebuildResult.DEGRADED;
            }
            if (result == null) {
                if (cacheNull) {
                    redisTemplate.set(nullCacheKey(key), NULL_CACHE_YES, NX, EX, RedisHBaseConfiguration.nullCacheExpireSeconds());
                }
                return HBase2RedisRebuildResult.NONE_RESULT;
            }
            NavigableMap<byte[], byte[]> map = result.getFamilyMap(CF_D);
            if (map.isEmpty()) {
                if (cacheNull) {
                    redisTemplate.set(nullCacheKey(key), NULL_CACHE_YES, NX, EX, RedisHBaseConfiguration.nullCacheExpireSeconds());
                }
                return HBase2RedisRebuildResult.NONE_RESULT;
            }
            boolean needClear = map.size() > RedisHBaseConfiguration.zsetMaxMemberSize();
            Map<byte[], Double> scoreMembers = new HashMap<>();
            Map<BytesKey, byte[]> mappingMap = new HashMap<>();
            for (Map.Entry<byte[], byte[]> entry : map.entrySet()) {
                byte[] value = entry.getKey();
                byte[] score = entry.getValue();
                if (isDataQualifier(value)) {
                    byte[] data = data(value);
                    scoreMembers.put(data, Bytes.toDouble(score));
                    if (needClear) {
                        mappingMap.put(new BytesKey(data), value);
                    }
                }
            }
            if (needClear) {
                logger.error("hbase rebuild size too large, size = {}, key = {}", map.size(), SafeEncoder.encode(key));
                try {
                    Map<byte[], Double> scoreMembers1 = scoreMembers;
                    scoreMembers = new HashMap<>();
                    List<Delete> deleteList = new ArrayList<>();
                    List<Map.Entry<byte[], Double>> entries = new ArrayList<>(scoreMembers1.entrySet());
                    entries.sort(Comparator.comparingDouble(Map.Entry::getValue));
                    for (int i = entries.size() - 1; i >= 0; i--) {
                        Map.Entry<byte[], Double> entry = entries.get(i);
                        if (scoreMembers.size() < RedisHBaseConfiguration.zsetMaxMemberSize()) {
                            scoreMembers.put(entry.getKey(), entry.getValue());
                        } else {
                            byte[] qualifier = mappingMap.get(new BytesKey(entry.getKey()));
                            if (qualifier != null) {
                                Delete delete = new Delete(result.getRow());
                                delete.addColumns(CF_D, qualifier);
                                deleteList.add(delete);
                                if (deleteList.size() >= RedisHBaseConfiguration.hbaseWriteBatchMaxSize()) {
                                    List<Delete> finalDeleteList = deleteList;
                                    exec.submit(() -> {
                                        try {
                                            hBaseTemplate.delete(RedisHBaseConfiguration.hbaseTableName(), finalDeleteList);
                                        } catch (Exception e) {
                                            logger.error("clear hbase error, key = {}", SafeEncoder.encode(key), e);
                                        }
                                    });
                                    deleteList = new ArrayList<>();
                                }
                            }
                        }
                    }
                    if (!deleteList.isEmpty()) {
                        List<Delete> finalDeleteList1 = deleteList;
                        exec.submit(() -> {
                            try {
                                hBaseTemplate.delete(RedisHBaseConfiguration.hbaseTableName(), finalDeleteList1);
                            } catch (Exception e) {
                                logger.error("clear hbase error, key = {}", SafeEncoder.encode(key), e);
                            }
                        });
                    }
                } catch (Exception e) {
                    logger.error("clear hbase error, key = {}", SafeEncoder.encode(key), e);
                }
            }

            putList = new ArrayList<>();
            _redis_zadd(key, scoreMembers, putList);
        } finally {
            redisLock.release();
        }
        if (!putList.isEmpty()) {
            if (RedisHBaseConfiguration.hbaseWriteOpeAsyncEnable()) {
                hBaseWriteAsyncExecutor.put(key, putList);
            } else {
                List<List<Put>> split = split(putList, RedisHBaseConfiguration.hbaseWriteBatchMaxSize());
                for (List<Put> puts : split) {
                    hBaseTemplate.put(RedisHBaseConfiguration.hbaseTableName(), puts);
                }
            }
        }
        byte[] expireTimeValue = result.getValue(CF_D, COL_EXPIRE_TIME);
        if (expireTimeValue != null) {
            long expireTime = Bytes.toLong(expireTimeValue);
            int seconds = (int) ((expireTime - System.currentTimeMillis()) / 1000);
            if (seconds > RedisHBaseConfiguration.zsetExpireSeconds()) {
                seconds = RedisHBaseConfiguration.zsetExpireSeconds();
            }
            redisTemplate.expire(redisKey(key), seconds);
        } else {
            redisTemplate.expire(redisKey(key), RedisHBaseConfiguration.zsetExpireSeconds());
        }
        return HBase2RedisRebuildResult.REBUILD_OK;
    }

    //
    private Result checkZSetExists(byte[] key) {
        key = buildRowKey(key);
        Get get = new Get(key);
        String tableName = RedisHBaseConfiguration.hbaseTableName();
        Result result = hBaseTemplate.get(tableName, get);
        if (result == null) return null;
        byte[] value = result.getValue(CF_D, COL_EXPIRE_TIME);
        if (value != null) {
            long expireTime = Bytes.toLong(value);
            if (System.currentTimeMillis() > expireTime) {
                Delete delete = new Delete(key);
                if (RedisHBaseConfiguration.hbaseWALAsyncEnable()) {
                    delete.setDurability(Durability.ASYNC_WAL);
                }
                hBaseTemplate.delete(tableName, delete);
                return null;
            }
        }
        byte[] typeRaw = result.getValue(CF_D, COL_TYPE);
        if (typeRaw == null) {
            return null;
        }
        if (!Bytes.equals(typeRaw, RedisHBaseType.ZSET.raw())) {
            throw new IllegalArgumentException("WRONGTYPE Operation against a key holding the wrong kind of type");
        }
        return result;
    }

    //
    private Long _redis_zadd(byte[] key, Map<byte[], Double> scoreMembers, List<Put> putList) {
        if (scoreMembers == null || scoreMembers.isEmpty()) return 0L;
        Map<byte[], Double> newScoreMembers = new HashMap<>();
        try (ICamelliaRedisPipeline pipelined = redisTemplate.pipelined()) {
            int pipelineSize = 0;
            for (Map.Entry<byte[], Double> entry : scoreMembers.entrySet()) {
                byte[] value = entry.getKey();
                Double score = entry.getValue();
                RedisHBaseMonitor.zsetValueSize(value.length);
                if (value.length > RedisHBaseConfiguration.zsetValueRefThreshold()) {//value有点大
                    byte[] valueRefKey = buildValueRefKey(key, value);
                    newScoreMembers.put(valueRefKey, score);
                    pipelined.setex(redisKey(valueRefKey), RedisHBaseConfiguration.zsetValueRefExpireSeconds(), value);
                    pipelineSize ++;
                    if (pipelineSize > RedisHBaseConfiguration.redisPipelineMaxBatchSize()) {
                        pipelined.sync();
                        pipelineSize = 0;
                    }
                    Put put = new Put(valueRefKey);
                    if (RedisHBaseConfiguration.hbaseWALAsyncEnable()) {
                        put.setDurability(Durability.ASYNC_WAL);
                    }
                    put.addColumn(CF_D, COL_DATA, value);
                    put.addColumn(CF_D, COL_TYPE, RedisHBaseType.INNER.raw());
                    putList.add(put);
                    RedisHBaseMonitor.zsetValueHitThreshold(true);
                } else {//value挺小
                    newScoreMembers.put(value, score);
                    RedisHBaseMonitor.zsetValueHitThreshold(false);
                }
            }
            Response<Long> response = pipelined.zadd(redisKey(key), newScoreMembers);
            pipelined.sync();
            return response.get();
        }
    }

    //
    private KeyStatus checkCacheAndRebuild4Write(byte[] key, String method) {
        if (checkRedisKeyExists(redisTemplate, key)) {
            RedisHBaseMonitor.incrWrite(method, WriteOpeType.REDIS_HIT);
            return KeyStatus.CACHE_OK;
        } else {
            HBase2RedisRebuildResult rebuildResult = rebuildZSet(key, false, false, false);
            switch (rebuildResult) {
                case REBUILD_OK:
                    RedisHBaseMonitor.incrWrite(method, WriteOpeType.REDIS_REBUILD_OK);
                    return KeyStatus.CACHE_OK;
                case NONE_RESULT:
                    RedisHBaseMonitor.incrWrite(method, WriteOpeType.REDIS_REBUILD_NONE_RESULT);
                    return KeyStatus.NULL;
                case NULL_CACHE_HIT:
                    RedisHBaseMonitor.incrWrite(method, WriteOpeType.REDIS_REBUILD_HIT_NULL_CACHE);
                    return KeyStatus.NULL;
                case DEGRADED:
                    RedisHBaseMonitor.incrWrite(method, WriteOpeType.REDIS_REBUILD_DEGRADED);
                    return KeyStatus.NULL;
            }
            throw new IllegalArgumentException("unknown HBase2RedisRebuildResult");
        }
    }

    //
    private KeyStatus checkCacheAndRebuild4Read(byte[] key, String method) {
        if (checkRedisKeyExists(redisTemplate, key)) {
            RedisHBaseMonitor.incrRead(method, ReadOpeType.REDIS_ONLY);
        } else {
            HBase2RedisRebuildResult rebuildResult = rebuildZSet(key, false, false, true);
            if (rebuildFail4Read(rebuildResult, method)) {
                return KeyStatus.NULL;
            }
        }
        return KeyStatus.CACHE_OK;
    }

    //
    private boolean rebuildFail4Read(HBase2RedisRebuildResult rebuildResult, String method) {
        switch (rebuildResult) {
            case REBUILD_OK:
                RedisHBaseMonitor.incrRead(method, ReadOpeType.HIT_TO_HBASE);
                return false;
            case NONE_RESULT:
                RedisHBaseMonitor.incrRead(method, ReadOpeType.HIT_TO_HBASE_AND_MISS);
                return true;
            case NULL_CACHE_HIT:
                RedisHBaseMonitor.incrRead(method, ReadOpeType.HIT_TO_HBASE_NULL_CACHE);
                return true;
            case DEGRADED:
                RedisHBaseMonitor.incrRead(method, ReadOpeType.HIT_TO_HBASE_DEGRADED);
                return false;
        }
        throw new IllegalArgumentException("unknown HBase2RedisRebuildResult");
    }

    private static final String CACHE_REBUILD_TASK = "cache_rebuild_delay_task";
    private final LocalCache localCache = new LocalCache();
    private void submitCacheRebuildDelayTask(byte[] key) {
        long sum = timerCount.sum();
        if (sum > RedisHBaseConfiguration.zsetRedisRebuildTaskMaxCount()) {
            logger.error("cache rebuild task too many, size = {}", sum);
            return;
        }
        String hex = Bytes.toHex(key);
        String tag = "dup";
        LocalCache.ValueWrapper valueWrapper = localCache.get(tag, hex);
        if (valueWrapper != null) {
            return;
        }
        int delaySeconds = RedisHBaseConfiguration.zsetRedisRebuildTaskDelaySeconds();
        boolean success = localCache.putIfAbsent(tag, hex, "", delaySeconds * 3);
        if (!success) {
            return;
        }
        timerCount.increment();
        timer.newTimeout(timeout -> {
            try {
                exec.submit(() -> {
                    try {
                        HBase2RedisRebuildResult result = null;
                        int index = 0;
                        int errorCount = 0;
                        while (result == null || result == HBase2RedisRebuildResult.DEGRADED) {
                            try {
                                result = rebuildZSet(key, true, true, false);
                                if (RedisHBaseConfiguration.zsetRedisRebuildTaskLogEnable()) {
                                    logger.info("cache rebuild delay task exec success, key = {}, index = {}, result = {}", SafeEncoder.encode(key), index + 1, result);
                                }
                                switch (result) {
                                    case REBUILD_OK:
                                        RedisHBaseMonitor.incrWrite(CACHE_REBUILD_TASK, WriteOpeType.REDIS_REBUILD_OK);
                                        break;
                                    case NONE_RESULT:
                                        RedisHBaseMonitor.incrWrite(CACHE_REBUILD_TASK, WriteOpeType.REDIS_REBUILD_NONE_RESULT);
                                        break;
                                    case NULL_CACHE_HIT:
                                        RedisHBaseMonitor.incrWrite(CACHE_REBUILD_TASK, WriteOpeType.REDIS_REBUILD_HIT_NULL_CACHE);
                                        break;
                                    case DEGRADED:
                                        RedisHBaseMonitor.incrWrite(CACHE_REBUILD_TASK, WriteOpeType.REDIS_REBUILD_DEGRADED);
                                        TimeUnit.MILLISECONDS.sleep(RedisHBaseConfiguration.zsetRebuildRetryIntervalMillis());
                                        break;
                                }
                            } catch (CamelliaRedisException e) {
                                logger.warn("cache rebuild delay task exec error, key = {}, index = {}, ex = {}", SafeEncoder.encode(key), index + 1, e.toString());
                                break;
                            } catch (Exception e) {
                                Throwable t = ErrorHandlerUtil.handler(e);
                                logger.error("cache rebuild delay task exec error, key = {}, index = {}, ex = {}", SafeEncoder.encode(key), index + 1, t.toString());
                                errorCount ++;
                            }
                            index ++;
                            if (index > 1 && index == RedisHBaseConfiguration.zsetRebuildErrorMaxRetry() - 1) {
                                FreqUtil.releaseFreqKey(redisTemplate, hbaseGetFreqKey(key));
                            }
                            if (index >= RedisHBaseConfiguration.zsetRebuildMaxRetry()) {
                                logger.error("cache rebuild delay task exec too many, index = {}, key = {}", index, SafeEncoder.encode(key));
                                break;
                            }
                            if (errorCount >= RedisHBaseConfiguration.zsetRebuildErrorMaxRetry()) {
                                logger.error("cache rebuild delay task exec too many error, errorCount = {}, key = {}", errorCount, SafeEncoder.encode(key));
                                if (RedisHBaseConfiguration.zsetRebuildClearIfErrorTooMany()) {
                                    try {
                                        hBaseTemplate.delete(RedisHBaseConfiguration.hbaseTableName(), new Delete(buildRowKey(key)));
                                        logger.error("clear key in hbase, key = {}", SafeEncoder.encode(key));
                                    } catch (Exception e) {
                                        Throwable t = ErrorHandlerUtil.handler(e);
                                        logger.error("clear key in hbase error, key = {}, ex = {}", SafeEncoder.encode(key), t.toString());
                                    }
                                }
                                break;
                            }
                        }
                    } finally {
                        localCache.evict(tag, hex);
                    }
                });
            } catch (Exception e) {
                localCache.evict(tag, hex);
                Throwable t = ErrorHandlerUtil.handler(e);
                logger.error("submit cache rebuild delay task error, key = {}, ex = {}", SafeEncoder.encode(key), t.toString());
            } finally {
                timerCount.decrement();
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }
}
