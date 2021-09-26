package com.netease.nim.camellia.id.gen.segment;


import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.id.gen.common.CamelliaIdGenException;
import com.netease.nim.camellia.id.gen.common.IDLoader;
import com.netease.nim.camellia.id.gen.common.IDRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2021/9/24
 */
public class CamelliaSegmentIdGen implements ICamelliaSegmentIdGen {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaSegmentIdGen.class);

    private final IDLoader idLoader;
    private final int regionBits;
    private final long regionId;
    private final int step;

    private final int maxRetry;
    private final long retryIntervalMillis;

    private final int cacheMaxCapacity;

    private final ConcurrentLinkedHashMap<String, LinkedBlockingQueue<Long>> cacheMap;
    private final ConcurrentLinkedHashMap<String, AtomicBoolean> lockMap;

    private final ExecutorService asyncLoadThreadPool;

    public CamelliaSegmentIdGen(CamelliaSegmentIdGenConfig config) {
        this.idLoader = config.getIdLoader();
        this.regionBits = config.getRegionBits();
        this.regionId = config.getRegionId();
        this.step = config.getStep();
        this.cacheMaxCapacity = step * 10;
        this.maxRetry = config.getMaxRetry();
        this.retryIntervalMillis = config.getRetryIntervalMillis();

        this.cacheMap = new ConcurrentLinkedHashMap.Builder<String, LinkedBlockingQueue<Long>>()
                .initialCapacity(config.getTagCount()).maximumWeightedCapacity(config.getTagCount()).build();
        this.lockMap = new ConcurrentLinkedHashMap.Builder<String, AtomicBoolean>()
                .initialCapacity(config.getTagCount() * 2).maximumWeightedCapacity(config.getTagCount() * 2).build();

        if (this.regionBits < 0) {
            throw new CamelliaIdGenException("regionBits should >= 0");
        }
        long maxRegionId = (1L << config.getRegionBits()) - 1;
        if (this.regionId > maxRegionId) {
            throw new CamelliaIdGenException("regionId too long");
        }
        this.asyncLoadThreadPool = config.getAsyncLoadThreadPool();

        logger.info("CamelliaSegmentIdGen init success, regionId = {}, regionBits = {}, step = {}, maxRetry = {}, retryIntervalMillis = {}",
                regionId, regionBits, step, maxRetry, retryIntervalMillis);
    }

    @Override
    public List<Long> genIds(String tag, int count) {
        try {
            //每次获取的个数不能超过step的5倍
            if (count > step * 5) {
                throw new CamelliaIdGenException("count exceed step*5");
            }
            LinkedBlockingQueue<Long> cache = getCacheQueue(tag);
            List<Long> ids = new ArrayList<>(count);
            int maxRetry = this.maxRetry;
            while (maxRetry-- > 0) {
                int needCount = count - ids.size();
                //发现缓存里已经有了
                if (cache.size() >= needCount) {
                    //尝试从缓存里获取到id
                    cache.drainTo(ids, needCount);
                    if (ids.size() >= count) {
                        //获取到了则返回
                        return ids;
                    }
                } else {
                    //发现缓存里不够，则尝试load一把
                    boolean success = tryLoad(tag, Math.max(count, step));
                    if (!success) {
                        //有并发load，则等待一会看看
                        try {
                            TimeUnit.MILLISECONDS.sleep(retryIntervalMillis);
                        } catch (InterruptedException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }
            throw new CamelliaIdGenException("exceed max retry");
        } finally {
            //检查缓存是否不足了，如不足了则提前load一把
            checkAndLoad(tag, Math.max(count, step/2), Math.max(count*2, step));
        }
    }

    @Override
    public long genId(String tag) {
        try {
            LinkedBlockingQueue<Long> cache = getCacheQueue(tag);
            int maxRetry = this.maxRetry;
            while (maxRetry-- > 0) {
                //尝试从缓存里取一把
                Long id = cache.poll();
                if (id != null) {
                    //取到了直接返回
                    return id;
                } else {
                    //取不到则尝试load一把
                    boolean success = tryLoad(tag, step);
                    if (!success) {
                        //有并发load，则等待一会看看
                        try {
                            TimeUnit.MILLISECONDS.sleep(retryIntervalMillis);
                        } catch (InterruptedException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }
            throw new CamelliaIdGenException("exceed max retry");
        } finally {
            //检查缓存是否不足了，如不足了则提前load一把
            checkAndLoad(tag, step/2, step);
        }
    }

    private boolean tryLoad(String tag, int count) {
        //尝试load一把
        //同时最多只有一个load任务
        if (getLock(tag).compareAndSet(false, true)) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("try load ids from idLoader, tag = {}, count = {}", tag, count);
                }
                loadCache(tag, count);
            } finally {
                getLock(tag).compareAndSet(true, false);
            }
            return true;
        }
        return false;
    }

    private void checkAndLoad(String tag, int threshold, int count) {
        LinkedBlockingQueue<Long> cache = getCacheQueue(tag);
        int size = cache.size();
        if (size < threshold) {//如果缓存低于step的一半，则认为快用完了，异步load一把
            //同时最多只有一个load任务
            if (getLock(tag).compareAndSet(false, true)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("cache.size[{}] < threshold[{}], try load from db", size, threshold);
                }
                try {
                    asyncLoadThreadPool.submit(() -> {
                        try {
                            loadCache(tag, count);
                        } finally {
                            getLock(tag).compareAndSet(true, false);
                        }
                    });
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }
    private void loadCache(String tag, int count) {
        try {
            LinkedBlockingQueue<Long> cache = getCacheQueue(tag);
            int size = cache.size();
            int maxLoading = cacheMaxCapacity - size;
            int loadCount = Math.min(maxLoading, count);
            IDRange load = idLoader.load(tag, loadCount);
            for (long i=load.getStart(); i<=load.getEnd(); i++) {
                long id = (i << regionBits) | regionId;
                cache.offer(id);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("load ids from idLoader success, tag = {}, start = {}, end = {}", tag, load.getStart(), load.getEnd());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new CamelliaIdGenException("load ids from idLoader error", e);
        }
    }

    public LinkedBlockingQueue<Long> getCacheQueue(String tag) {
        LinkedBlockingQueue<Long> queue = this.cacheMap.get(tag);
        if (queue == null) {
            synchronized (this.cacheMap) {
                queue = this.cacheMap.get(tag);
                if (queue == null) {
                    queue = new LinkedBlockingQueue<>(cacheMaxCapacity + 1000);
                    this.cacheMap.put(tag, queue);
                }
            }
        }
        return queue;
    }

    public AtomicBoolean getLock(String tag) {
        AtomicBoolean lock = this.lockMap.get(tag);
        if (lock == null) {
            synchronized (this.lockMap) {
                lock = this.lockMap.get(tag);
                if (lock == null) {
                    lock = new AtomicBoolean();
                    this.lockMap.put(tag, lock);
                }
            }
        }
        return lock;
    }
}
