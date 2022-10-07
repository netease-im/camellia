package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import com.netease.nim.camellia.redis.proxy.hbase.monitor.RedisHBaseMonitor;
import com.netease.nim.camellia.redis.proxy.hbase.monitor.OperationType;
import com.netease.nim.camellia.redis.proxy.hbase.util.FreqUtils;
import com.netease.nim.camellia.redis.proxy.hbase.util.SetFromList;
import com.netease.nim.camellia.redis.proxy.util.BytesKey;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Response;
import redis.clients.jedis.Tuple;

import java.util.*;

import static com.netease.nim.camellia.redis.proxy.hbase.util.RedisHBaseUtils.*;


/**
 *
 * Created by caojiajun on 2020/2/24.
 */
public class RedisHBaseZSetMixClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisHBaseZSetMixClient.class);

    private final CamelliaRedisTemplate redisTemplate;
    private final CamelliaHBaseTemplate hBaseTemplate;
    private final HBaseAsyncWriteExecutor hBaseAsyncWriteExecutor;

    public RedisHBaseZSetMixClient(CamelliaRedisTemplate redisTemplate,
                                   CamelliaHBaseTemplate hBaseTemplate,
                                   HBaseAsyncWriteExecutor hBaseAsyncWriteExecutor) {
        this.hBaseTemplate = hBaseTemplate;
        this.redisTemplate = redisTemplate;
        this.hBaseAsyncWriteExecutor = hBaseAsyncWriteExecutor;
    }

    /**
     *
     */
    public Long zadd(byte[] key, Map<byte[], Double> scoreMembers) {
        if (scoreMembers == null || scoreMembers.isEmpty()) return 0L;
        List<Put> putList = new ArrayList<>();
        int pipelineSize = 0;
        try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
            Map<byte[], Double> newScoreMembers = new HashMap<>();
            for (Map.Entry<byte[], Double> entry : scoreMembers.entrySet()) {
                byte[] member = entry.getKey();
                if (member.length > zsetMemberRefKeyThreshold()) {
                    byte[] memberRefKey = buildRefKey(key, member);
                    newScoreMembers.put(memberRefKey, entry.getValue());
                    pipeline.setex(redisKey(memberRefKey), zsetMemberRefKeyExpireSeconds(), member);
                    pipelineSize ++;
                    if (pipelineSize >= RedisHBaseConfiguration.redisMaxPipeline()) {
                        pipeline.sync();
                        pipelineSize = 0;
                    }
                    Put put = new Put(memberRefKey);
                    put.addColumn(CF_D, COL_DATA, member);
                    putList.add(put);
                    RedisHBaseMonitor.incrValueSize("zset", member.length, true);
                } else {
                    newScoreMembers.put(member, entry.getValue());
                    RedisHBaseMonitor.incrValueSize("zset", member.length, false);
                }
            }
            Response<Long> response = pipeline.zadd(redisKey(key), newScoreMembers);
            pipeline.sync();
            if (!putList.isEmpty()) {
                List<List<Put>> split = split(putList, RedisHBaseConfiguration.hbaseMaxBatch());
                for (List<Put> puts : split) {
                    if (RedisHBaseConfiguration.hbaseAsyncWriteEnable()) {
                        HBaseAsyncWriteExecutor.HBaseAsyncWriteTask task = new HBaseAsyncWriteExecutor.HBaseAsyncWriteTask();
                        task.setKey(key);
                        task.setPuts(puts);
                        boolean success = this.hBaseAsyncWriteExecutor.submit(task);
                        if (!success) {
                            if (RedisHBaseConfiguration.hbaseDegradedIfAsyncWriteSubmitFail()) {
                                logger.error("hBaseAsyncWriteExecutor submit fail for zadd, degraded hbase write, key = {}", Utils.bytesToString(key));
                                RedisHBaseMonitor.incrDegraded("zadd|async_write_submit_fail");
                            } else {
                                logger.warn("hBaseAsyncWriteExecutor submit fail, write sync for zadd, key = {}", Utils.bytesToString(key));
                                hBaseTemplate.put(hbaseTableName(), puts);
                            }
                        }
                    } else {
                        hBaseTemplate.put(hbaseTableName(), puts);
                    }
                }
                RedisHBaseMonitor.incr("zadd", OperationType.REDIS_HBASE.name());
            } else {
                RedisHBaseMonitor.incr("zadd", OperationType.REDIS_ONLY.name());
            }
            return response.get();
        }
    }

    /**
     *
     */
    public Long zcard(byte[] key) {
        return redisTemplate.zcard(redisKey(key));
    }

    /**
     *
     */
    public Long zcount(byte[] key, byte[] min, byte[] max) {
        return redisTemplate.zcount(redisKey(key), min, max);
    }

    /**
     *
     */
    public Long zrem(byte[] key, byte[]... member) {
        byte[] redisKey = redisKey(key);
        List<Delete> deleteList = new ArrayList<>();
        int pipelineSize = 0;
        List<Response<Long>> responseList = new ArrayList<>();
        try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
            Response<Long> response = pipeline.zrem(redisKey, member);
            responseList.add(response);
            pipelineSize ++;
            for (byte[] bytes : member) {
                if (bytes.length <= ZSET_MEMBER_REF_KEY_THRESHOLD_MIN) continue;
                byte[] memberRefKey = buildRefKey(key, bytes);
                Response<Long> response1 = pipeline.zrem(redisKey, memberRefKey);
                responseList.add(response1);
                pipeline.del(redisKey(memberRefKey));
                pipelineSize += 2;
                if (pipelineSize > RedisHBaseConfiguration.redisMaxPipeline()) {
                    pipeline.sync();
                    pipelineSize = 0;
                }
                Delete delete = new Delete(memberRefKey);
                deleteList.add(delete);
            }
            pipeline.sync();
            if (!deleteList.isEmpty()) {
                List<List<Delete>> split = split(deleteList, RedisHBaseConfiguration.hbaseMaxBatch());
                for (List<Delete> list : split) {
                    if (RedisHBaseConfiguration.hbaseAsyncWriteEnable()) {
                        HBaseAsyncWriteExecutor.HBaseAsyncWriteTask task = new HBaseAsyncWriteExecutor.HBaseAsyncWriteTask();
                        task.setKey(key);
                        task.setDeletes(list);
                        boolean sucess = hBaseAsyncWriteExecutor.submit(task);
                        if (!sucess) {
                            if (RedisHBaseConfiguration.hbaseDegradedIfAsyncWriteSubmitFail()) {
                                logger.error("hBaseAsyncWriteExecutor submit fail for zrem, degraded for hbase write, key = {}", Utils.bytesToString(key));
                                RedisHBaseMonitor.incrDegraded("zrem|async_write_submit_fail");
                            } else {
                                logger.warn("hBaseAsyncWriteExecutor submit fail, write sync for zrem, key = {}", Utils.bytesToString(key));
                                hBaseTemplate.delete(hbaseTableName(), list);
                            }
                        }
                    } else {
                        hBaseTemplate.delete(hbaseTableName(), list);
                    }
                }
                RedisHBaseMonitor.incr("zrem", OperationType.REDIS_HBASE.name());
            } else {
                RedisHBaseMonitor.incr("zrem", OperationType.REDIS_ONLY.name());
            }
            long ret = 0;
            for (Response<Long> longResponse : responseList) {
                ret += longResponse.get();
            }
            return ret;
        }
    }

    /**
     *
     */
    public Long zlexcount(byte[] key, byte[] min, byte[] max) {
        return redisTemplate.zlexcount(redisKey(key), min, max);
    }

    /**
     *
     */
    public Set<byte[]> zrange(byte[] key, long start, long end) {
        Set<byte[]> set = redisTemplate.zrange(redisKey(key), start, end);
        return _parseOriginalSet("zrange", key, set);
    }

    /**
     *
     */
    public Set<byte[]> zrevrange(byte[] key, long start, long end) {
        Set<byte[]> set = redisTemplate.zrevrange(redisKey(key), start, end);
        return _parseOriginalSet("zrevrange", key, set);
    }

    /**
     *
     */
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
        Set<Tuple> set = redisTemplate.zrevrangeByScoreWithScores(redisKey(key), max, min, offset, count);
        return _parseOriginalTuple("zrevrangeByScoreWithScores", key, set);
    }

    /**
     *
     */
    public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
        Set<byte[]> set = redisTemplate.zrangeByLex(redisKey(key), min, max, offset, count);
        return _parseOriginalSet("zrangeByLex", key, set);
    }

    /**
     *
     */
    public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
        Set<byte[]> set = redisTemplate.zrangeByLex(redisKey(key), min, max);
        return _parseOriginalSet("zrangeByLex", key, set);
    }

    /**
     *
     */
    public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
        Set<byte[]> set = redisTemplate.zrevrangeByLex(redisKey(key), max, min);
        return _parseOriginalSet("zrevrangeByLex", key, set);
    }

    /**
     *
     */
    public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
        Set<byte[]> set = redisTemplate.zrevrangeByLex(redisKey(key), max, min, offset, count);
        return _parseOriginalSet("zrevrangeByLex", key, set);
    }

    /**
     *
     */
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
        Set<Tuple> set = redisTemplate.zrevrangeByScoreWithScores(redisKey(key), max, min);
        return _parseOriginalTuple("zrevrangeByScoreWithScores", key, set);
    }

    /**
     *
     */
    public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
        Set<byte[]> set = redisTemplate.zrevrangeByScore(redisKey(key), max, min, offset, count);
        return _parseOriginalSet("zrevrangeByScore", key, set);
    }

    /**
     *
     */
    public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
        Set<byte[]> set = redisTemplate.zrevrangeByScore(redisKey(key), max, min);
        return _parseOriginalSet("zrevrangeByScore", key, set);
    }

    /**
     *
     */
    public Set<Tuple> zrevrangeWithScores(byte[] key, long start, long end) {
        Set<Tuple> set = redisTemplate.zrevrangeWithScores(redisKey(key), start, end);
        return _parseOriginalTuple("zrevrangeWithScores", key, set);
    }

    /**
     *
     */
    public Set<Tuple> zrangeWithScores(byte[] key, long start, long end) {
        Set<Tuple> set = redisTemplate.zrangeWithScores(redisKey(key), start, end);
        return _parseOriginalTuple("zrangeWithScores", key, set);
    }

    /**
     *
     */
    public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
        Set<byte[]> set = redisTemplate.zrangeByScore(redisKey(key), min, max);
        return _parseOriginalSet("zrangeByScore", key, set);
    }

    /**
     *
     */
    public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
        Set<byte[]> set = redisTemplate.zrangeByScore(redisKey(key), min, max, offset, count);
        return _parseOriginalSet("zrangeByScore", key, set);
    }

    /**
     *
     */
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
        Set<Tuple> set = redisTemplate.zrangeByScoreWithScores(redisKey(key), min, max);
        return _parseOriginalTuple("zrangeByScoreWithScores", key, set);
    }

    /**
     *
     */
    public Double zscore(byte[] key, byte[] member)  {
        byte[] redisKey = redisKey(key);
        if (member.length <= ZSET_MEMBER_REF_KEY_THRESHOLD_MIN) {
            return redisTemplate.zscore(redisKey, member);
        }
        if (member.length > zsetMemberRefKeyThreshold()) {
            byte[] memberRefKey = buildRefKey(key, member);
            Double score = redisTemplate.zscore(redisKey, memberRefKey);
            if (score != null) {
                return score;
            }
            return redisTemplate.zscore(redisKey, member);
        } else {
            Double score = redisTemplate.zscore(redisKey, member);
            if (score != null) {
                return score;
            }
            byte[] memberRefKey = buildRefKey(key, member);
            return redisTemplate.zscore(redisKey, memberRefKey);
        }
    }

    /**
     *
     */
    public Double zincrby(byte[] key, double score, byte[] member) {
        byte[] redisKey = redisKey(key);
        if (member.length <= ZSET_MEMBER_REF_KEY_THRESHOLD_MIN) {
            return redisTemplate.zincrby(redisKey, score, member);
        }
        boolean isBigMember;
        byte[] memberRefKey = buildRefKey(key, member);
        if (member.length > zsetMemberRefKeyThreshold()) {
            Double oldScore = redisTemplate.zscore(redisKey, memberRefKey);
            if (oldScore != null) {
                isBigMember = true;
            } else {
                oldScore = redisTemplate.zscore(redisKey, member);
                isBigMember = oldScore == null;
            }
        } else {
            Double oldScore = redisTemplate.zscore(redisKey, member);
            if (oldScore != null) {
                isBigMember = false;
            } else {
                oldScore = redisTemplate.zscore(redisKey, memberRefKey);
                isBigMember = oldScore != null;
            }
        }
        if (isBigMember) {
            return redisTemplate.zincrby(key, score, memberRefKey);
        } else {
            return redisTemplate.zincrby(key, score, member);
        }
    }

    /**
     *
     */
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
        Set<Tuple> set = redisTemplate.zrangeByScoreWithScores(redisKey(key), min, max, offset, count);
        return _parseOriginalTuple("zrangeByScoreWithScores", key, set);
    }

    /**
     *
     */
    public Long zremrangeByRank(byte[] key, long start, long stop) {
        byte[] redisKey = redisKey(key);
        Set<byte[]> set = redisTemplate.zrange(redisKey, start, stop);
        _zrem("zremrangeByRank", key, set);
        return redisTemplate.zremrangeByRank(redisKey, start, stop);
    }

    /**
     *
     */
    public Long zremrangeByScore(byte[] key, byte[] start, byte[] end) {
        byte[] redisKey = redisKey(key);
        Set<byte[]> set = redisTemplate.zrangeByScore(redisKey, start, end);
        _zrem("zremrangeByScore", key, set);
        return redisTemplate.zremrangeByScore(redisKey, start, end);
    }

    /**
     *
     */
    public Long zremrangeByLex(byte[] key, byte[] min, byte[] max) {
        byte[] redisKey = redisKey(key);
        Set<byte[]> set = redisTemplate.zrangeByLex(redisKey, min, max);
        _zrem("zremrangeByLex", key, set);
        return redisTemplate.zremrangeByLex(redisKey, min, max);
    }

    /**
     *
     */
    public Long zrank(byte[] key, byte[] member) {
        byte[] redisKey = redisKey(key);
        if (member.length <= ZSET_MEMBER_REF_KEY_THRESHOLD_MIN) {
            return redisTemplate.zrank(redisKey, member);
        }
        if (member.length < zsetMemberRefKeyThreshold()) {
            Long zrank = redisTemplate.zrank(redisKey, member);
            if (zrank == null) {
                byte[] memberRefKey = buildRefKey(key, member);
                return redisTemplate.zrank(redisKey, memberRefKey);
            }
            return zrank;
        } else {
            byte[] memberRefKey = buildRefKey(key, member);
            Long zrank = redisTemplate.zrank(redisKey, memberRefKey);
            if (zrank == null) {
                return redisTemplate.zrank(redisKey, member);
            }
            return zrank;
        }
    }

    /**
     *
     */
    public Long zrevrank(byte[] key, byte[] member) {
        byte[] redisKey = redisKey(key);
        if (member.length <= ZSET_MEMBER_REF_KEY_THRESHOLD_MIN) {
            return redisTemplate.zrevrank(redisKey, member);
        }
        if (member.length < zsetMemberRefKeyThreshold()) {
            Long zrank = redisTemplate.zrevrank(redisKey, member);
            if (zrank == null) {
                byte[] memberRefKey = buildRefKey(key, member);
                return redisTemplate.zrevrank(redisKey, memberRefKey);
            }
            return zrank;
        } else {
            byte[] memberRefKey = buildRefKey(key, member);
            Long zrank = redisTemplate.zrevrank(redisKey, memberRefKey);
            if (zrank == null) {
                return redisTemplate.zrevrank(redisKey, member);
            }
            return zrank;
        }
    }

    private void _zrem(String method, byte[] key, Set<byte[]> set) {
        if (set == null || set.isEmpty()) {
            RedisHBaseMonitor.incr(method, OperationType.REDIS_ONLY.name());
            return;
        }
        List<Delete> deleteList = new ArrayList<>();
        int pipelineSize = 0;
        try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
            for (byte[] bytes : set) {
                if (isRefKey(key, bytes)) {
                    pipeline.del(redisKey(bytes));
                    pipelineSize ++;
                    if (pipelineSize >= RedisHBaseConfiguration.redisMaxPipeline()) {
                        pipeline.sync();
                        pipelineSize = 0;
                    }
                    Delete delete = new Delete(bytes);
                    deleteList.add(delete);
                }
            }
            pipeline.sync();
        }
        if (!deleteList.isEmpty()) {
            List<List<Delete>> split = split(deleteList, RedisHBaseConfiguration.hbaseMaxBatch());
            for (List<Delete> list : split) {
                if (RedisHBaseConfiguration.hbaseAsyncWriteEnable()) {
                    HBaseAsyncWriteExecutor.HBaseAsyncWriteTask task = new HBaseAsyncWriteExecutor.HBaseAsyncWriteTask();
                    task.setKey(key);
                    task.setDeletes(list);
                    boolean success = hBaseAsyncWriteExecutor.submit(task);
                    if (!success) {
                        if (RedisHBaseConfiguration.hbaseDegradedIfAsyncWriteSubmitFail()) {
                            logger.error("hBaseAsyncWriteExecutor submit fail for {}, degraded for hbase write, key = {}", method, Utils.bytesToString(key));
                            RedisHBaseMonitor.incrDegraded(method + "|async_write_submit_fail");
                        } else {
                            logger.warn("hBaseAsyncWriteExecutor submit fail, write sync for {}, key = {}", method, Utils.bytesToString(key));
                            hBaseTemplate.delete(hbaseTableName(), list);
                        }
                    }
                } else {
                    hBaseTemplate.delete(hbaseTableName(), list);
                }
            }
            RedisHBaseMonitor.incr(method, OperationType.REDIS_HBASE.name());
        } else {
            RedisHBaseMonitor.incr(method, OperationType.REDIS_ONLY.name());
        }
    }

    private Set<byte[]> _parseOriginalSet(String method, byte[] key, Set<byte[]> set) {
        if (set == null || set.isEmpty()) {
            RedisHBaseMonitor.incr(method, OperationType.REDIS_ONLY.name());
            return set;
        }
        int pipelineSize = 0;
        Map<BytesKey, Response<byte[]>> map = new HashMap<>();
        try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
            for (byte[] bytes : set) {
                if (isRefKey(key, bytes)) {
                    Response<byte[]> response = pipeline.get(redisKey(bytes));
                    map.put(new BytesKey(bytes), response);
                    pipelineSize ++;
                    if (pipelineSize >= RedisHBaseConfiguration.redisMaxPipeline()) {
                        pipeline.sync();
                        pipelineSize = 0;
                    }
                }
            }
            pipeline.sync();
        }
        if (map.isEmpty()) {
            RedisHBaseMonitor.incr(method, OperationType.REDIS_ONLY.name());
            return set;
        }
        List<Get> getList = new ArrayList<>();
        List<byte[]> list = new ArrayList<>(set.size());
        for (byte[] bytes : set) {
            if (isRefKey(key, bytes)) {
                Response<byte[]> response = map.get(new BytesKey(bytes));
                byte[] originalValue = response.get();
                if (originalValue != null) {
                    list.add(originalValue);
                } else {
                    getList.add(new Get(bytes));
                    list.add(bytes);
                }
            } else {
                list.add(bytes);
            }
        }
        if (getList.isEmpty()) {
            RedisHBaseMonitor.incr(method, OperationType.REDIS_ONLY.name());
            return SetFromList.of(list);
        }
        Map<BytesKey, byte[]> hbaseMap = new HashMap<>();
        List<List<Get>> split = split(getList, RedisHBaseConfiguration.hbaseMaxBatch());
        for (List<Get> gets : split) {
            if (RedisHBaseConfiguration.hbaseReadDegraded()) {
                logger.warn("get original zset member from hbase degraded, key = {}", Utils.bytesToString(key));
                RedisHBaseMonitor.incrDegraded("hbase_read_batch_degraded");
            } else {
                if (FreqUtils.hbaseReadFreq()) {
                    Result[] results = hBaseTemplate.get(hbaseTableName(), gets);
                    for (Result result : results) {
                        byte[] originalValue = parseOriginalValue(result);
                        if (originalValue != null) {
                            hbaseMap.put(new BytesKey(result.getRow()), originalValue);
                        }
                    }
                } else {
                    RedisHBaseMonitor.incrDegraded("hbase_read_batch_freq_degraded");
                }
            }
        }
        List<byte[]> ret = new ArrayList<>(set.size());
        pipelineSize = 0;
        try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
            for (byte[] bytes : list) {
                if (isRefKey(key, bytes)) {
                    byte[] originalValue = hbaseMap.get(new BytesKey(bytes));
                    if (originalValue != null) {
                        ret.add(originalValue);
                        pipeline.setex(redisKey(bytes), zsetMemberRefKeyExpireSeconds(), originalValue);
                        pipelineSize ++;
                        if (pipelineSize > RedisHBaseConfiguration.redisMaxPipeline()) {
                            pipeline.sync();
                            pipelineSize = 0;
                        }
                    }
                } else {
                    ret.add(bytes);
                }
            }
            pipeline.sync();
        }
        RedisHBaseMonitor.incr(method, OperationType.REDIS_HBASE.name());
        return SetFromList.of(ret);
    }

    private Set<Tuple> _parseOriginalTuple(String method, byte[] key, Set<Tuple> set) {
        if (set == null || set.isEmpty()) {
            RedisHBaseMonitor.incr(method, OperationType.REDIS_ONLY.name());
            return set;
        }
        int pipelineSize = 0;
        Map<BytesKey, Response<byte[]>> map = new HashMap<>();
        try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
            for (Tuple tuple : set) {
                byte[] bytes = tuple.getBinaryElement();
                if (isRefKey(key, tuple.getBinaryElement())) {
                    Response<byte[]> response = pipeline.get(redisKey(bytes));
                    map.put(new BytesKey(bytes), response);
                    pipelineSize ++;
                    if (pipelineSize >= RedisHBaseConfiguration.redisMaxPipeline()) {
                        pipeline.sync();
                        pipelineSize = 0;
                    }
                }
            }
            pipeline.sync();
        }
        if (map.isEmpty()) {
            RedisHBaseMonitor.incr(method, OperationType.REDIS_ONLY.name());
            return set;
        }
        pipelineSize = 0;
        boolean hit2HBase = false;
        try (ICamelliaRedisPipeline pipeline = redisTemplate.pipelined()) {
            List<Tuple> list = new ArrayList<>(set.size());
            for (Tuple tuple : set) {
                byte[] bytes = tuple.getBinaryElement();
                if (isRefKey(key, bytes)) {
                    Response<byte[]> response = map.get(new BytesKey(bytes));
                    byte[] originalValue = response.get();
                    if (originalValue != null) {
                        list.add(new Tuple(originalValue, tuple.getScore()));
                    } else {
                        hit2HBase = true;
                        if (RedisHBaseConfiguration.hbaseReadDegraded()) {
                            logger.warn("get original zset member tuple from hbase degraded, key = {}, member-ref-key = {}",
                                    Utils.bytesToString(key), Bytes.toHex(bytes));
                            RedisHBaseMonitor.incrDegraded("hbase_read_degraded");
                        } else {
                            if (FreqUtils.hbaseReadFreq()) {
                                Result result = hBaseTemplate.get(hbaseTableName(), new Get(bytes));
                                originalValue = parseOriginalValue(result);
                                if (originalValue != null) {
                                    pipeline.setex(redisKey(bytes), zsetMemberRefKeyExpireSeconds(), originalValue);
                                    pipelineSize++;
                                    if (pipelineSize > RedisHBaseConfiguration.redisMaxPipeline()) {
                                        pipeline.sync();
                                        pipelineSize = 0;
                                    }
                                    list.add(new Tuple(originalValue, tuple.getScore()));
                                }
                            } else {
                                RedisHBaseMonitor.incrDegraded("hbase_read_freq_degraded");
                            }
                        }
                    }
                } else {
                    list.add(tuple);
                }
            }
            pipeline.sync();
            if (hit2HBase) {
                RedisHBaseMonitor.incr(method, OperationType.REDIS_HBASE.name());
            } else {
                RedisHBaseMonitor.incr(method, OperationType.REDIS_ONLY.name());
            }
            return SetFromList.of(list);
        }
    }
}
