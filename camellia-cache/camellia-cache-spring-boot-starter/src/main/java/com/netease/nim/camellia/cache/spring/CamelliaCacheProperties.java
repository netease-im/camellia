package com.netease.nim.camellia.cache.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "camellia-cache")
public class CamelliaCacheProperties {

    private String cachePrefix = null;
    private int multiOpBatchSize = 500;
    private long syncLoadExpireMillis = 1000;
    private int syncLoadMaxRetry = 1;
    private long syncLoadSleepMillis = 100;
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

    public String getCachePrefix() {
        return cachePrefix;
    }

    public void setCachePrefix(String cachePrefix) {
        this.cachePrefix = cachePrefix;
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
}
