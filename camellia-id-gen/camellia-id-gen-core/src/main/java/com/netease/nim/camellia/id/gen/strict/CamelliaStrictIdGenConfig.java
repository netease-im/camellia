package com.netease.nim.camellia.id.gen.strict;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.id.gen.common.CamelliaIdGenConstants;
import com.netease.nim.camellia.id.gen.common.IDLoader;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 基于数据库和redis的严格递增的id生成器
 * Created by caojiajun on 2021/9/24
 */
public class CamelliaStrictIdGenConfig {

    public static final ThreadPoolExecutor defaultAsyncLoadThreadPool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2,
            Runtime.getRuntime().availableProcessors() * 2, 0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10240), new CamelliaThreadFactory("camellia-strict-id-gen", true));

    private CamelliaRedisTemplate template;
    private ExecutorService asyncLoadThreadPool = defaultAsyncLoadThreadPool;
    private IDLoader idLoader;

    //redis的key前缀
    private String cacheKeyPrefix = CamelliaIdGenConstants.Strict.cacheKeyPrefix;
    //并发操作时的锁超时时间
    private long lockExpireMillis = CamelliaIdGenConstants.Strict.lockExpireMillis;
    //id在redis中的缓存时长
    private int cacheExpireSeconds = CamelliaIdGenConstants.Strict.cacheExpireSeconds;
    //最大重试次数
    private int maxRetry = CamelliaIdGenConstants.Strict.maxRetry;
    //并发情况下重试间隔
    private long retryIntervalMillis = CamelliaIdGenConstants.Strict.retryIntervalMillis;
    //装填redis时的默认步长
    private int defaultStep = CamelliaIdGenConstants.Strict.defaultStep;
    //装填redis时的最大步长
    private int maxStep = CamelliaIdGenConstants.Strict.maxStep;
    //id多久被用完后触发步长调整的阈值，低于该值则扩大步长，大于该值的2倍，则缩小步长
    private int cacheHoldSeconds = CamelliaIdGenConstants.Strict.cacheHoldSeconds;

    //单元id所占的位数
    //默认为0，表示不需要单元id
    //如果为4，则表示最多支持16个单元id，会基于数据库生成的id在右边补上4bit的单元id
    private int regionBits = CamelliaIdGenConstants.Strict.regionBits;
    //regionId，位数不超过regionBits
    private long regionId;

    public CamelliaRedisTemplate getTemplate() {
        return template;
    }

    public void setTemplate(CamelliaRedisTemplate template) {
        this.template = template;
    }

    public ExecutorService getAsyncLoadThreadPool() {
        return asyncLoadThreadPool;
    }

    public void setAsyncLoadThreadPool(ExecutorService asyncLoadThreadPool) {
        this.asyncLoadThreadPool = asyncLoadThreadPool;
    }

    public IDLoader getIdLoader() {
        return idLoader;
    }

    public void setIdLoader(IDLoader idLoader) {
        this.idLoader = idLoader;
    }

    public String getCacheKeyPrefix() {
        return cacheKeyPrefix;
    }

    public void setCacheKeyPrefix(String cacheKeyPrefix) {
        this.cacheKeyPrefix = cacheKeyPrefix;
    }

    public long getLockExpireMillis() {
        return lockExpireMillis;
    }

    public void setLockExpireMillis(long lockExpireMillis) {
        this.lockExpireMillis = lockExpireMillis;
    }

    public int getCacheExpireSeconds() {
        return cacheExpireSeconds;
    }

    public void setCacheExpireSeconds(int cacheExpireSeconds) {
        this.cacheExpireSeconds = cacheExpireSeconds;
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

    public int getDefaultStep() {
        return defaultStep;
    }

    public void setDefaultStep(int defaultStep) {
        this.defaultStep = defaultStep;
    }

    public int getMaxStep() {
        return maxStep;
    }

    public void setMaxStep(int maxStep) {
        this.maxStep = maxStep;
    }

    public int getCacheHoldSeconds() {
        return cacheHoldSeconds;
    }

    public void setCacheHoldSeconds(int cacheHoldSeconds) {
        this.cacheHoldSeconds = cacheHoldSeconds;
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
}
