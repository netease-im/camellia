package com.netease.nim.camellia.id.gen.segment;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.id.gen.common.IDLoader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2021/9/24
 */
public class CamelliaSegmentIdGenConfig {

    public static final ThreadPoolExecutor defaultAsyncLoadThreadPool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2,
            Runtime.getRuntime().availableProcessors() * 2, 0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10240), new CamelliaThreadFactory("camellia-segment-id-gen"));

    private IDLoader idLoader;

    //单元id所占的位数
    //默认为0，表示不需要单元id
    //如果为4，则表示最多支持16个单元id，会基于数据库生成的id在右边补上4bit的单元id
    private int regionBits = 0;

    private long regionId;//regionId，位数不超过regionBits

    private int step = 1000;

    private int tagCount = 1000;//包含的tag数

    //maxRetry*retryIntervalMillis至少要比idLoader执行一次耗时更长
    private int maxRetry = 100;//并发情况下重试的最大次数
    private long retryIntervalMillis = 10;//并发情况下重试间隔

    private ExecutorService asyncLoadThreadPool = defaultAsyncLoadThreadPool;

    public IDLoader getIdLoader() {
        return idLoader;
    }

    public void setIdLoader(IDLoader idLoader) {
        this.idLoader = idLoader;
    }

    public int getRegionBits() {
        return regionBits;
    }

    public void setRegionBits(int regionBits) {
        this.regionBits = regionBits;
    }

    public long getRegionId() {
        return regionId;
    }

    public void setRegionId(long regionId) {
        this.regionId = regionId;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public ExecutorService getAsyncLoadThreadPool() {
        return asyncLoadThreadPool;
    }

    public void setAsyncLoadThreadPool(ExecutorService asyncLoadThreadPool) {
        this.asyncLoadThreadPool = asyncLoadThreadPool;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    public long getRetryIntervalMillis() {
        return retryIntervalMillis;
    }

    public void setRetryIntervalMillis(long retryIntervalMillis) {
        this.retryIntervalMillis = retryIntervalMillis;
    }

    public int getTagCount() {
        return tagCount;
    }

    public void setTagCount(int tagCount) {
        this.tagCount = tagCount;
    }
}
