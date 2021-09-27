package com.netease.nim.camellia.id.gen.segment;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.id.gen.common.CamelliaIdGenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2021/9/27
 */
public abstract class AbstractCamelliaSegmentIdGen implements ICamelliaSegmentIdGen {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCamelliaSegmentIdGen.class);
    protected int step;

    protected int maxRetry;
    protected long retryIntervalMillis;

    protected int cacheMaxCapacity;

    private final Object lock = new Object();

    protected ConcurrentLinkedHashMap<String, LinkedBlockingQueue<Long>> cacheMap;
    protected ConcurrentLinkedHashMap<String, AtomicBoolean> lockMap;

    protected ExecutorService asyncLoadThreadPool;

    public void setStep(int step) {
        this.step = step;
    }

    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    public void setRetryIntervalMillis(long retryIntervalMillis) {
        this.retryIntervalMillis = retryIntervalMillis;
    }

    public void setCacheMaxCapacity(int cacheMaxCapacity) {
        this.cacheMaxCapacity = cacheMaxCapacity;
    }

    public void setCacheMap(ConcurrentLinkedHashMap<String, LinkedBlockingQueue<Long>> cacheMap) {
        this.cacheMap = cacheMap;
    }

    public void setLockMap(ConcurrentLinkedHashMap<String, AtomicBoolean> lockMap) {
        this.lockMap = lockMap;
    }

    public void setAsyncLoadThreadPool(ExecutorService asyncLoadThreadPool) {
        this.asyncLoadThreadPool = asyncLoadThreadPool;
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
                checkAndLoadCache(getCacheQueue(tag), tag, count);
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
                            checkAndLoadCache(getCacheQueue(tag), tag, count);
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

    private void checkAndLoadCache(LinkedBlockingQueue<Long> cache, String tag, int count) {
        int size = cache.size();
        int maxLoading = cacheMaxCapacity - size;
        int loadCount = Math.min(maxLoading, count);
        loadCache(cache, tag, loadCount);
    }

    protected abstract void loadCache(LinkedBlockingQueue<Long> cache, String tag, int loadCount);

    private LinkedBlockingQueue<Long> getCacheQueue(String tag) {
        LinkedBlockingQueue<Long> queue = this.cacheMap.get(tag);
        if (queue == null) {
            synchronized (this.lock) {
                queue = this.cacheMap.get(tag);
                if (queue == null) {
                    queue = new LinkedBlockingQueue<>(cacheMaxCapacity + 1000);
                    this.cacheMap.put(tag, queue);
                }
            }
        }
        return queue;
    }

    private AtomicBoolean getLock(String tag) {
        AtomicBoolean lock = this.lockMap.get(tag);
        if (lock == null) {
            synchronized (this.lock) {
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
