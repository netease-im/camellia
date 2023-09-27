package com.netease.nim.camellia.id.gen.strict;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.id.gen.common.CamelliaIdGenException;
import com.netease.nim.camellia.id.gen.common.IDLoader;
import com.netease.nim.camellia.id.gen.common.IDRange;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import com.netease.nim.camellia.redis.toolkit.lock.CamelliaRedisLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 基于数据库和redis的严格递增的id生成器
 * <p>
 * 数据库记录每个tag当前分配到的id
 * 每个发号器节点会从数据库中取一段id后塞到redis的list中（不同节点会通过分布式锁保证id不会乱序）
 * 每个发号器节点先从redis中取id，如果取不到则穿透到数据库进行load
 * redis中的id即将耗尽时会提前从db中load最新一批的id
 * 发号器节点会统计每个批次分配完毕消耗的时间来动态调整批次大小
 * <p>
 * Created by caojiajun on 2021/9/24
 */
public class CamelliaStrictIdGen implements ICamelliaStrictIdGen {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaStrictIdGen.class);

    private final String cacheKeyPrefix;
    private final CamelliaRedisTemplate template;
    private final ExecutorService asyncLoadThreadPool;
    private final IDLoader idLoader;
    private final int regionBits;
    private final long regionId;
    private final int regionIdShiftingBits;

    private final long lockExpireMillis;
    private final int cacheExpireSeconds;

    private final int maxRetry;
    private final long retryIntervalMillis;

    private final int defaultStep;
    private final int maxStep;
    private final int cacheHoldSeconds;

    private final ConcurrentLinkedHashMap<String, Long> lastExpireTimeMap = new ConcurrentLinkedHashMap.Builder<String, Long>()
            .initialCapacity(1000).maximumWeightedCapacity(10000).build();

    public CamelliaStrictIdGen(CamelliaStrictIdGenConfig config) {
        this.template = config.getTemplate();
        this.asyncLoadThreadPool = config.getAsyncLoadThreadPool();
        this.idLoader = config.getIdLoader();
        if (template == null) {
            throw new CamelliaIdGenException("redis template not found");
        }
        if (idLoader == null) {
            throw new CamelliaIdGenException("idLoader not found");
        }
        logger.info("CamelliaStrictIdGen, idLoader = {}", idLoader.getClass().getName());
        this.cacheKeyPrefix = config.getCacheKeyPrefix();
        this.lockExpireMillis = config.getLockExpireMillis();
        this.cacheExpireSeconds = config.getCacheExpireSeconds();
        this.maxRetry = config.getMaxRetry();
        this.defaultStep = config.getDefaultStep();
        this.maxStep = config.getMaxStep();
        this.cacheHoldSeconds = config.getCacheHoldSeconds();
        this.regionBits = config.getRegionBits();
        this.regionId = config.getRegionId();
        this.regionIdShiftingBits = config.getRegionIdShiftingBits();
        this.retryIntervalMillis = config.getRetryIntervalMillis();

        if (this.regionBits < 0) {
            throw new CamelliaIdGenException("regionBits should >= 0");
        }
        long maxRegionId = (1L << config.getRegionBits()) - 1;
        if (this.regionId > maxRegionId) {
            throw new CamelliaIdGenException("regionId too long");
        }
        logger.info("CamelliaStrictIdGen init success, regionId = {}, regionBits = {}, regionIdShiftingBits = {}, defaultStep = {}, maxStep = {}, maxRetry = {}, retryIntervalMillis = {}",
                regionId, regionBits, regionIdShiftingBits, defaultStep, maxStep, maxRetry, retryIntervalMillis);
    }

    public IDLoader getIdLoader() {
        return idLoader;
    }

    @Override
    public long genId(String tag) {
        try {
            String cacheKey = cacheKey(tag);
            //从redis里取一下，如果取到了，则返回
            String seq = template.rpop(cacheKey);
            if (seq != null) {
                try {
                    return Long.parseLong(seq);
                } finally {
                    try {
                        //此时提交一个异步任务，检查是否需要更新redis队列
                        asyncLoadThreadPool.submit(() -> checkAndLoad(tag));
                    } catch (Exception e) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("try checkAndLoad fail", e);
                        }
                    }
                }
            }
            //redis里没有取到，则更新redis队列
            tryLoadIds(tag);
            //更新完redis队列，从redis队列里取元素
            int retry = maxRetry;
            while (retry-- > 0) {
                for (int i = 0; i < 10; i++) {
                    String rpop = template.rpop(cacheKey);
                    if (rpop == null) {
                        //取不到，自旋一下
                        TimeUnit.MILLISECONDS.sleep(retryIntervalMillis);
                        continue;
                    }
                    //取到了直接返回
                    return Long.parseLong(rpop);
                }
                //连续取不到，则判断一下是否redis队列被消耗完了
                tryLoadIds(tag);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new CamelliaIdGenException(e);
        }
        //超过最大重试次数，则上抛异常
        throw new CamelliaIdGenException("exceed maxRetry=" + maxRetry);
    }

    @Override
    public long peekId(String tag) {
        try {
            String cacheKey = cacheKey(tag);
            int retry = maxRetry;
            while (retry-- > 0) {
                String lastId = template.lindex(cacheKey, -1);
                if (lastId != null) {
                    return Long.parseLong(lastId);
                }
                String cachePeekKey = cachePeekKey(tag);
                String value = template.get(cachePeekKey);
                if (value != null) {
                    return Long.parseLong(value);
                }
                Long selectId = idLoader.selectId(tag);
                if (selectId != null) {
                    template.setex(cachePeekKey, cacheExpireSeconds, String.valueOf(selectId + 1));
                    return selectId + 1;
                }
                boolean success = tryLoadIds(tag);
                if (!success) {
                    TimeUnit.MILLISECONDS.sleep(retryIntervalMillis);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new CamelliaIdGenException(e);
        }
        throw new CamelliaIdGenException("exceed maxRetry=" + maxRetry);
    }

    @Override
    public long decodeRegionId(long id) {
        if (regionBits == 0) {
            return -1;
        } else if (regionBits > 0 && regionIdShiftingBits == 0) {
            return ((1L << regionBits) - 1) & id;
        } else {
            return ((((1L << regionBits) - 1) << regionIdShiftingBits) & id) >> regionIdShiftingBits;
        }
    }

    //尝试load一下id，会有分布式的锁来控制并发
    private boolean tryLoadIds(String tag) {
        String cacheKey = cacheKey(tag);
        String lockKey = lockKey(cacheKey);
        String cacheHoldKey = cacheHoldKey(cacheKey);
        String currentStepKey = currentStepKey(cacheKey);

        CamelliaRedisLock lock = CamelliaRedisLock.newLock(template, lockKey, lockExpireMillis, lockExpireMillis);
        boolean tryLock = lock.tryLock();
        if (tryLock) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("acquire lock success, tag = {}, cacheKey = {}, lockKey = {}, will load ids from idLoader", tag, cacheKey, lockKey);
                }
                String lastSyncTimeStr;//上次load的时间
                String currentStepStr;//当前的step
                try (ICamelliaRedisPipeline pipelined = template.pipelined()) {
                    Response<String> response1 = pipelined.get(cacheHoldKey);
                    Response<String> response2 = pipelined.get(currentStepKey);
                    pipelined.sync();
                    lastSyncTimeStr = response1.get();
                    currentStepStr = response2.get();
                }

                int newStep = defaultStep;
                if (lastSyncTimeStr != null && currentStepStr != null) {
                    long lastSyncTime = Long.parseLong(lastSyncTimeStr);
                    int currentStep = Integer.parseInt(currentStepStr);
                    long now = System.currentTimeMillis();
                    //动态调整步长
                    if (now - lastSyncTime < cacheHoldSeconds * 1000L) {//如果发现距离上次同步的时间的间隔小于阈值，则扩大step
                        newStep = Math.min(currentStep * 2, maxStep);
                        if (logger.isDebugEnabled()) {
                            logger.debug("step try expand, tag = {}, cacheKey = {}, old/new={}/{}", tag, cacheKey, currentStep, newStep);
                        }
                    } else if (now - lastSyncTime > cacheHoldSeconds * 1000L * 2) {//如果发现距离上次同步的时间的间隔大于阈值的2倍，则缩小step
                        newStep = Math.max(currentStep / 2, defaultStep);
                        if (logger.isDebugEnabled()) {
                            logger.debug("step try shrink, tag = {}, cacheKey = {}, old/new={}/{}", tag, cacheKey, currentStep, newStep);
                        }
                    }
                }
                //使用调整后的step去load一把
                IDRange range = idLoader.load(tag, newStep);
                List<String> ids = new ArrayList<>();
                for (long i = range.getStart(); i<= range.getEnd(); i++) {
                    long id;
                    if (regionBits == 0) {
                        id = i;
                    } else if (regionBits > 0 && regionIdShiftingBits == 0) {
                        id = (i << regionBits) | regionId;
                    } else {
                        id = ((i >> regionIdShiftingBits) << (regionIdShiftingBits + regionBits)) | (regionId << regionIdShiftingBits) | (i & ((1L << regionIdShiftingBits) - 1));
                    }
                    ids.add(String.valueOf(id));
                }
                String cachePeekKey = cachePeekKey(tag);
                //把新获取到的id导入到redis的队列里，并记录load时间以及load的step
                try (ICamelliaRedisPipeline pipelined = template.pipelined()) {
                    pipelined.lpush(cacheKey, ids.toArray(new String[0]));
                    pipelined.expire(cacheKey, cacheExpireSeconds);
                    pipelined.setex(cacheHoldKey, cacheExpireSeconds, String.valueOf(System.currentTimeMillis()));
                    pipelined.setex(currentStepKey, cacheExpireSeconds, String.valueOf(newStep));
                    pipelined.del(cachePeekKey);
                    pipelined.sync();
                    if (logger.isDebugEnabled()) {
                        logger.debug("load ids from idLoader success, cacheKey = {}, start = {}, end = {}", cacheKey, range.getStart(), range.getEnd());
                    }
                }
                return true;
            } finally {
                lock.release();
            }
        }
        return false;
    }

    //检查缓存里的id是否即将耗尽，如果是，则尝试load一把提前补上
    private void checkAndLoad(String tag) {
        try {
            String cacheKey = cacheKey(tag);
            Long size = template.llen(cacheKey);
            if (size == null || size <= 0) {//redis里空了，则需要触发一下load
                if (logger.isDebugEnabled()) {
                    logger.debug("tag = {}, cacheKey = {}, cache.size=0, try load ids from idLoader", tag, cacheKey);
                }
                tryLoadIds(tag);
            } else {
                String currentStepStr = template.get(currentStepKey(cacheKey));
                int currentStep = currentStepStr != null ? Integer.parseInt(currentStepStr) : defaultStep;
                if (size < currentStep / 2) {//如果redis里不足一半了，则需要触发一下load
                    if (logger.isDebugEnabled()) {
                        logger.debug("tag = {}, cacheKey = {}, cache.size/currentStep={}/{}, try load ids from idLoader", tag, cacheKey, size, currentStep);
                    }
                    tryLoadIds(tag);
                } else {
                    Long lastExpireTime = lastExpireTimeMap.get(tag);
                    if (lastExpireTime != null && ((System.currentTimeMillis() - lastExpireTime) / 1000) < (cacheExpireSeconds / 10)) {
                        return;
                    }
                    //否则，delay一下相关redis key
                    try (ICamelliaRedisPipeline pipelined = template.pipelined()) {
                        pipelined.expire(cacheKey, cacheExpireSeconds);
                        pipelined.expire(cacheHoldKey(cacheKey), cacheExpireSeconds);
                        pipelined.expire(currentStepKey(cacheKey), cacheExpireSeconds);
                        pipelined.sync();
                        lastExpireTimeMap.put(tag, System.currentTimeMillis());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("checkAndLoad error", e);
        }
    }

    private String cacheKey(String tag) {
        return cacheKeyPrefix + "|" + tag;
    }

    private String cachePeekKey(String tag) {
        return cacheKeyPrefix + "|peek|" + tag;
    }

    private String lockKey(String cacheKey) {
        return cacheKey + "~lock";
    }

    private String cacheHoldKey(String cacheKey) {
        return cacheKey + "~cacheHold";
    }

    private String currentStepKey(String cacheKey) {
        return cacheKey + "~currentStep";
    }
}
