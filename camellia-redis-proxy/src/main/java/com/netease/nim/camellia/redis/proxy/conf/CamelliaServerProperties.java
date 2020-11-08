package com.netease.nim.camellia.redis.proxy.conf;



/**
 *
 * Created by caojiajun on 2019/11/11.
 */
public class CamelliaServerProperties {
    private int port = Constants.Server.severPort;
    private String password;
    private boolean monitorEnable = Constants.Server.monitorEnable;
    private int monitorIntervalSeconds = Constants.Server.monitorIntervalSeconds;
    private String monitorCallbackClassName = Constants.Server.monitorCallbackClassName;
    private boolean commandSpendTimeMonitorEnable = Constants.Server.commandSpendTimeMonitorEnable;
    private long slowCommandThresholdMillisTime = Constants.Server.slowCommandThresholdMillisTime;
    private String slowCommandCallbackClassName = Constants.Server.slowCommandCallbackClassName;
    private String commandInterceptorClassName;
    private boolean hotKeyMonitorEnable = Constants.Server.hotKeyMonitorEnable;
    private HotKeyMonitorConfig hotKeyMonitorConfig;
    private boolean hotKeyCacheEnable = Constants.Server.hotKeyCacheEnable;
    private HotKeyCacheConfig hotKeyCacheConfig;

    private int bossThread = 1;
    private int workThread = Constants.Server.workThread;
    private int soBacklog = Constants.Server.soBacklog;
    private int soSndbuf = Constants.Server.soSndbuf;
    private int soRcvbuf = Constants.Server.soRcvbuf;
    private int writeBufferWaterMarkLow = Constants.Server.writeBufferWaterMarkLow;
    private int writeBufferWaterMarkHigh = Constants.Server.writeBufferWaterMarkHigh;
    private int commandDecodeMaxBatchSize = Constants.Server.commandDecodeMaxBatchSize;


    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isMonitorEnable() {
        return monitorEnable;
    }

    public void setMonitorEnable(boolean monitorEnable) {
        this.monitorEnable = monitorEnable;
    }

    public boolean isCommandSpendTimeMonitorEnable() {
        return commandSpendTimeMonitorEnable;
    }

    public void setCommandSpendTimeMonitorEnable(boolean commandSpendTimeMonitorEnable) {
        this.commandSpendTimeMonitorEnable = commandSpendTimeMonitorEnable;
    }

    public long getSlowCommandThresholdMillisTime() {
        return slowCommandThresholdMillisTime;
    }

    public void setSlowCommandThresholdMillisTime(long slowCommandThresholdMillisTime) {
        this.slowCommandThresholdMillisTime = slowCommandThresholdMillisTime;
    }

    public String getMonitorCallbackClassName() {
        return monitorCallbackClassName;
    }

    public void setMonitorCallbackClassName(String monitorCallbackClassName) {
        this.monitorCallbackClassName = monitorCallbackClassName;
    }

    public int getMonitorIntervalSeconds() {
        return monitorIntervalSeconds;
    }

    public void setMonitorIntervalSeconds(int monitorIntervalSeconds) {
        this.monitorIntervalSeconds = monitorIntervalSeconds;
    }

    public int getBossThread() {
        return bossThread;
    }

    public void setBossThread(int bossThread) {
        this.bossThread = bossThread;
    }

    public int getWorkThread() {
        return workThread;
    }

    public void setWorkThread(int workThread) {
        this.workThread = workThread;
    }

    public int getSoBacklog() {
        return soBacklog;
    }

    public void setSoBacklog(int soBacklog) {
        this.soBacklog = soBacklog;
    }

    public int getSoSndbuf() {
        return soSndbuf;
    }

    public void setSoSndbuf(int soSndbuf) {
        this.soSndbuf = soSndbuf;
    }

    public int getSoRcvbuf() {
        return soRcvbuf;
    }

    public void setSoRcvbuf(int soRcvbuf) {
        this.soRcvbuf = soRcvbuf;
    }

    public int getWriteBufferWaterMarkLow() {
        return writeBufferWaterMarkLow;
    }

    public void setWriteBufferWaterMarkLow(int writeBufferWaterMarkLow) {
        this.writeBufferWaterMarkLow = writeBufferWaterMarkLow;
    }

    public int getWriteBufferWaterMarkHigh() {
        return writeBufferWaterMarkHigh;
    }

    public void setWriteBufferWaterMarkHigh(int writeBufferWaterMarkHigh) {
        this.writeBufferWaterMarkHigh = writeBufferWaterMarkHigh;
    }

    public int getCommandDecodeMaxBatchSize() {
        return commandDecodeMaxBatchSize;
    }

    public void setCommandDecodeMaxBatchSize(int commandDecodeMaxBatchSize) {
        this.commandDecodeMaxBatchSize = commandDecodeMaxBatchSize;
    }

    public String getCommandInterceptorClassName() {
        return commandInterceptorClassName;
    }

    public void setCommandInterceptorClassName(String commandInterceptorClassName) {
        this.commandInterceptorClassName = commandInterceptorClassName;
    }

    public String getSlowCommandCallbackClassName() {
        return slowCommandCallbackClassName;
    }

    public void setSlowCommandCallbackClassName(String slowCommandCallbackClassName) {
        this.slowCommandCallbackClassName = slowCommandCallbackClassName;
    }

    public HotKeyMonitorConfig getHotKeyMonitorConfig() {
        return hotKeyMonitorConfig;
    }

    public void setHotKeyMonitorConfig(HotKeyMonitorConfig hotKeyMonitorConfig) {
        this.hotKeyMonitorConfig = hotKeyMonitorConfig;
    }

    public boolean isHotKeyMonitorEnable() {
        return hotKeyMonitorEnable;
    }

    public void setHotKeyMonitorEnable(boolean hotKeyMonitorEnable) {
        this.hotKeyMonitorEnable = hotKeyMonitorEnable;
    }

    public boolean isHotKeyCacheEnable() {
        return hotKeyCacheEnable;
    }

    public void setHotKeyCacheEnable(boolean hotKeyCacheEnable) {
        this.hotKeyCacheEnable = hotKeyCacheEnable;
    }

    public HotKeyCacheConfig getHotKeyCacheConfig() {
        return hotKeyCacheConfig;
    }

    public void setHotKeyCacheConfig(HotKeyCacheConfig hotKeyCacheConfig) {
        this.hotKeyCacheConfig = hotKeyCacheConfig;
    }

    public static class HotKeyMonitorConfig {
        private long checkMillis = Constants.Server.hotKeyMonitorCheckMillis;
        private long checkCacheMaxCapacity = Constants.Server.hotKeyMonitorCheckCacheMaxCapacity;
        private long checkThreshold = Constants.Server.hotKeyMonitorCheckThreshold;
        private int maxHotKeyCount = Constants.Server.hotKeyMonitorMaxHotKeyCount;
        private String hotKeyMonitorCallbackClassName = Constants.Server.hotKeyMonitorCallbackClassName;

        public long getCheckMillis() {
            return checkMillis;
        }

        public void setCheckMillis(long checkMillis) {
            this.checkMillis = checkMillis;
        }

        public long getCheckCacheMaxCapacity() {
            return checkCacheMaxCapacity;
        }

        public void setCheckCacheMaxCapacity(long checkCacheMaxCapacity) {
            this.checkCacheMaxCapacity = checkCacheMaxCapacity;
        }

        public long getCheckThreshold() {
            return checkThreshold;
        }

        public void setCheckThreshold(long checkThreshold) {
            this.checkThreshold = checkThreshold;
        }

        public int getMaxHotKeyCount() {
            return maxHotKeyCount;
        }

        public void setMaxHotKeyCount(int maxHotKeyCount) {
            this.maxHotKeyCount = maxHotKeyCount;
        }

        public String getHotKeyMonitorCallbackClassName() {
            return hotKeyMonitorCallbackClassName;
        }

        public void setHotKeyMonitorCallbackClassName(String hotKeyMonitorCallbackClassName) {
            this.hotKeyMonitorCallbackClassName = hotKeyMonitorCallbackClassName;
        }
    }

    public static class HotKeyCacheConfig {
        private long hotKeyCacheExpireMillis = Constants.Server.hotKeyCacheExpireMillis;
        private long hotKeyCacheMaxCapacity = Constants.Server.hotKeyCacheMaxCapacity;

        private long hotKeyCacheCounterCheckMillis = Constants.Server.hotKeyCacheCounterCheckMillis;
        private long hotKeyCacheCounterMaxCapacity = Constants.Server.hotKeyCacheCounterMaxCapacity;
        private long hotKeyCacheCounterCheckThreshold = Constants.Server.hotKeyCacheCounterCheckThreshold;

        private String hotKeyCacheKeyCheckerClassName = Constants.Server.hotKeyCacheKeyCheckerClassName;

        private long hotKeyCacheStatsCallbackIntervalSeconds = Constants.Server.hotKeyCacheStatsCallbackIntervalSeconds;
        private String hotKeyCacheStatsCallbackClassName = Constants.Server.hotKeyCacheStatsCallbackClassName;

        public long getHotKeyCacheExpireMillis() {
            return hotKeyCacheExpireMillis;
        }

        public void setHotKeyCacheExpireMillis(long hotKeyCacheExpireMillis) {
            this.hotKeyCacheExpireMillis = hotKeyCacheExpireMillis;
        }

        public long getHotKeyCacheMaxCapacity() {
            return hotKeyCacheMaxCapacity;
        }

        public void setHotKeyCacheMaxCapacity(long hotKeyCacheMaxCapacity) {
            this.hotKeyCacheMaxCapacity = hotKeyCacheMaxCapacity;
        }

        public long getHotKeyCacheCounterCheckMillis() {
            return hotKeyCacheCounterCheckMillis;
        }

        public void setHotKeyCacheCounterCheckMillis(long hotKeyCacheCounterCheckMillis) {
            this.hotKeyCacheCounterCheckMillis = hotKeyCacheCounterCheckMillis;
        }

        public long getHotKeyCacheCounterMaxCapacity() {
            return hotKeyCacheCounterMaxCapacity;
        }

        public void setHotKeyCacheCounterMaxCapacity(long hotKeyCacheCounterMaxCapacity) {
            this.hotKeyCacheCounterMaxCapacity = hotKeyCacheCounterMaxCapacity;
        }

        public long getHotKeyCacheCounterCheckThreshold() {
            return hotKeyCacheCounterCheckThreshold;
        }

        public void setHotKeyCacheCounterCheckThreshold(long hotKeyCacheCounterCheckThreshold) {
            this.hotKeyCacheCounterCheckThreshold = hotKeyCacheCounterCheckThreshold;
        }

        public String getHotKeyCacheKeyCheckerClassName() {
            return hotKeyCacheKeyCheckerClassName;
        }

        public void setHotKeyCacheKeyCheckerClassName(String hotKeyCacheKeyCheckerClassName) {
            this.hotKeyCacheKeyCheckerClassName = hotKeyCacheKeyCheckerClassName;
        }

        public long getHotKeyCacheStatsCallbackIntervalSeconds() {
            return hotKeyCacheStatsCallbackIntervalSeconds;
        }

        public void setHotKeyCacheStatsCallbackIntervalSeconds(long hotKeyCacheStatsCallbackIntervalSeconds) {
            this.hotKeyCacheStatsCallbackIntervalSeconds = hotKeyCacheStatsCallbackIntervalSeconds;
        }

        public String getHotKeyCacheStatsCallbackClassName() {
            return hotKeyCacheStatsCallbackClassName;
        }

        public void setHotKeyCacheStatsCallbackClassName(String hotKeyCacheStatsCallbackClassName) {
            this.hotKeyCacheStatsCallbackClassName = hotKeyCacheStatsCallbackClassName;
        }
    }
}
