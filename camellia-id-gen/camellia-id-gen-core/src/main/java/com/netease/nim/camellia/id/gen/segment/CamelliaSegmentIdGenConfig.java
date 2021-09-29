package com.netease.nim.camellia.id.gen.segment;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.id.gen.common.CamelliaIdGenConstants;
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
            new LinkedBlockingQueue<>(10240), new CamelliaThreadFactory("camellia-segment-id-gen", true));

    private IDLoader idLoader;

    private int regionBits = CamelliaIdGenConstants.Segment.regionBits;

    private long regionId;

    private int step = CamelliaIdGenConstants.Segment.step;

    private int tagCount = CamelliaIdGenConstants.Segment.tagCount;

    private int maxRetry = CamelliaIdGenConstants.Segment.maxRetry;

    private long retryIntervalMillis = CamelliaIdGenConstants.Segment.retryIntervalMillis;

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
