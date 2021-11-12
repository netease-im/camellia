package com.netease.nim.camellia.id.gen.springboot.segment;

import com.netease.nim.camellia.id.gen.common.CamelliaIdGenConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by caojiajun on 2021/9/27
 */
@ConfigurationProperties(prefix = "camellia-id-gen-segment")
public class CamelliaIdGenSegmentProperties {

    private int regionBits = CamelliaIdGenConstants.Segment.regionBits;
    private long regionId;
    private int regionIdShiftingBits = 0;//regionId偏移量，默认不偏移

    private int step = CamelliaIdGenConstants.Segment.step;

    private int tagCount = CamelliaIdGenConstants.Segment.tagCount;

    private int maxRetry = CamelliaIdGenConstants.Segment.maxRetry;

    private long retryIntervalMillis = CamelliaIdGenConstants.Segment.retryIntervalMillis;

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

    public int getRegionIdShiftingBits() {
        return regionIdShiftingBits;
    }

    public void setRegionIdShiftingBits(int regionIdShiftingBits) {
        this.regionIdShiftingBits = regionIdShiftingBits;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public int getTagCount() {
        return tagCount;
    }

    public void setTagCount(int tagCount) {
        this.tagCount = tagCount;
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
}
