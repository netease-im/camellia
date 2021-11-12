package com.netease.nim.camellia.id.gen.springboot.strict;

import com.netease.nim.camellia.id.gen.common.CamelliaIdGenConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by caojiajun on 2021/9/27
 */
@ConfigurationProperties(prefix = "camellia-id-gen-strict")
public class CamelliaIdGenStrictProperties {

    private String cacheKeyPrefix = CamelliaIdGenConstants.Strict.cacheKeyPrefix;
    private long lockExpireMillis = CamelliaIdGenConstants.Strict.lockExpireMillis;
    private int cacheExpireSeconds = CamelliaIdGenConstants.Strict.cacheExpireSeconds;
    private int maxRetry = CamelliaIdGenConstants.Strict.maxRetry;
    private long retryIntervalMillis = CamelliaIdGenConstants.Strict.retryIntervalMillis;
    private int defaultStep = CamelliaIdGenConstants.Strict.defaultStep;
    private int maxStep = CamelliaIdGenConstants.Strict.maxStep;
    private int cacheHoldSeconds = CamelliaIdGenConstants.Strict.cacheHoldSeconds;
    private int regionBits = CamelliaIdGenConstants.Strict.regionBits;
    private int regionId;
    private int regionIdShiftingBits = 0;//regionId偏移量，默认不偏移

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

    public int getRegionId() {
        return regionId;
    }

    public void setRegionId(int regionId) {
        this.regionId = regionId;
    }

    public int getRegionIdShiftingBits() {
        return regionIdShiftingBits;
    }

    public void setRegionIdShiftingBits(int regionIdShiftingBits) {
        this.regionIdShiftingBits = regionIdShiftingBits;
    }
}
