package com.netease.nim.camellia.id.gen.springboot.segment;

import com.netease.nim.camellia.id.gen.common.CamelliaIdGenConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2021/9/27
 */
@ConfigurationProperties(prefix = "camellia-id-gen-segment")
public class CamelliaIdGenSegmentProperties {

    private int monitorIntervalSeconds = CamelliaIdGenConstants.monitorIntervalSeconds;
    private int regionBits = CamelliaIdGenConstants.Segment.regionBits;
    private long regionId;
    private int regionIdShiftingBits = 0;//regionId偏移量，默认不偏移

    private int step = CamelliaIdGenConstants.Segment.step;

    private int tagCount = CamelliaIdGenConstants.Segment.tagCount;

    private int maxRetry = CamelliaIdGenConstants.Segment.maxRetry;

    private long retryIntervalMillis = CamelliaIdGenConstants.Segment.retryIntervalMillis;

    private IdSyncInMultiRegionsConf idSyncInMultiRegionsConf = new IdSyncInMultiRegionsConf();

    public static class IdSyncInMultiRegionsConf {
        private boolean enable;
        private long checkIntervalSeconds = 24*3600L;
        private long idUpdateThreshold = 10*10000L;
        private long apiTimeoutMillis = 10*1000L;
        private List<String> multiRegionUrls = new ArrayList<>();
        private List<String> whiteListTags = new ArrayList<>();
        private List<String> blackListTags = new ArrayList<>();

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public long getCheckIntervalSeconds() {
            return checkIntervalSeconds;
        }

        public void setCheckIntervalSeconds(long checkIntervalSeconds) {
            this.checkIntervalSeconds = checkIntervalSeconds;
        }

        public long getIdUpdateThreshold() {
            return idUpdateThreshold;
        }

        public void setIdUpdateThreshold(long idUpdateThreshold) {
            this.idUpdateThreshold = idUpdateThreshold;
        }

        public long getApiTimeoutMillis() {
            return apiTimeoutMillis;
        }

        public void setApiTimeoutMillis(long apiTimeoutMillis) {
            this.apiTimeoutMillis = apiTimeoutMillis;
        }

        public List<String> getMultiRegionUrls() {
            return multiRegionUrls;
        }

        public void setMultiRegionUrls(List<String> multiRegionUrls) {
            this.multiRegionUrls = multiRegionUrls;
        }

        public List<String> getWhiteListTags() {
            return whiteListTags;
        }

        public void setWhiteListTags(List<String> whiteListTags) {
            this.whiteListTags = whiteListTags;
        }

        public List<String> getBlackListTags() {
            return blackListTags;
        }

        public void setBlackListTags(List<String> blackListTags) {
            this.blackListTags = blackListTags;
        }
    }

    public int getMonitorIntervalSeconds() {
        return monitorIntervalSeconds;
    }

    public void setMonitorIntervalSeconds(int monitorIntervalSeconds) {
        this.monitorIntervalSeconds = monitorIntervalSeconds;
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

    public IdSyncInMultiRegionsConf getIdSyncInMultiRegionsConf() {
        return idSyncInMultiRegionsConf;
    }

    public void setIdSyncInMultiRegionsConf(IdSyncInMultiRegionsConf idSyncInMultiRegionsConf) {
        this.idSyncInMultiRegionsConf = idSyncInMultiRegionsConf;
    }
}
