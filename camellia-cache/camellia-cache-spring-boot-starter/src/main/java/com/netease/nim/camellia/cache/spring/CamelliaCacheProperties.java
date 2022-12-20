package com.netease.nim.camellia.cache.spring;

import com.netease.nim.camellia.cache.core.CamelliaCacheEnv;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "camellia-cache")
public class CamelliaCacheProperties {

    private int multiOpBatchSize = CamelliaCacheEnv.multiOpBatchSize;
    private long syncLoadExpireMillis = CamelliaCacheEnv.syncLoadExpireMillis;
    private int syncLoadMaxRetry = CamelliaCacheEnv.syncLoadMaxRetry;
    private long syncLoadSleepMillis = CamelliaCacheEnv.syncLoadSleepMillis;
    private boolean compressEnable = false;
    private int compressThreshold = 1024;
    private int maxCacheValue = CamelliaCacheEnv.maxCacheValue;//缓存value的最大值
    private Local local = new Local();

    public static class Local {
        private int initialCapacity = 10000;
        private int maxCapacity = 100000;

        public int getInitialCapacity() {
            return initialCapacity;
        }

        public void setInitialCapacity(int initialCapacity) {
            this.initialCapacity = initialCapacity;
        }

        public int getMaxCapacity() {
            return maxCapacity;
        }

        public void setMaxCapacity(int maxCapacity) {
            this.maxCapacity = maxCapacity;
        }
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public int getMultiOpBatchSize() {
        return multiOpBatchSize;
    }

    public void setMultiOpBatchSize(int multiOpBatchSize) {
        this.multiOpBatchSize = multiOpBatchSize;
    }

    public long getSyncLoadExpireMillis() {
        return syncLoadExpireMillis;
    }

    public void setSyncLoadExpireMillis(long syncLoadExpireMillis) {
        this.syncLoadExpireMillis = syncLoadExpireMillis;
    }

    public int getSyncLoadMaxRetry() {
        return syncLoadMaxRetry;
    }

    public void setSyncLoadMaxRetry(int syncLoadMaxRetry) {
        this.syncLoadMaxRetry = syncLoadMaxRetry;
    }

    public long getSyncLoadSleepMillis() {
        return syncLoadSleepMillis;
    }

    public void setSyncLoadSleepMillis(long syncLoadSleepMillis) {
        this.syncLoadSleepMillis = syncLoadSleepMillis;
    }

    public boolean isCompressEnable() {
        return compressEnable;
    }

    public void setCompressEnable(boolean compressEnable) {
        this.compressEnable = compressEnable;
    }

    public int getCompressThreshold() {
        return compressThreshold;
    }

    public void setCompressThreshold(int compressThreshold) {
        this.compressThreshold = compressThreshold;
    }

    public int getMaxCacheValue() {
        return maxCacheValue;
    }

    public void setMaxCacheValue(int maxCacheValue) {
        this.maxCacheValue = maxCacheValue;
    }
}
