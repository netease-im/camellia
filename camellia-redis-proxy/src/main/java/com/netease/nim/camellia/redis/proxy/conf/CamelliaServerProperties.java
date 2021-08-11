package com.netease.nim.camellia.redis.proxy.conf;



/**
 *
 * Created by caojiajun on 2019/11/11.
 */
public class CamelliaServerProperties {
    private int port = Constants.Server.severPort;
    private String applicationName;
    private String password;
    private boolean monitorEnable = Constants.Server.monitorEnable;
    private int monitorIntervalSeconds = Constants.Server.monitorIntervalSeconds;
    private String monitorCallbackClassName = Constants.Server.monitorCallbackClassName;
    private boolean commandSpendTimeMonitorEnable = Constants.Server.commandSpendTimeMonitorEnable;
    private long slowCommandThresholdMillisTime = Constants.Server.slowCommandThresholdMillisTime;
    private String slowCommandCallbackClassName = Constants.Server.slowCommandMonitorCallbackClassName;
    private String commandInterceptorClassName;
    private boolean hotKeyMonitorEnable = Constants.Server.hotKeyMonitorEnable;
    private HotKeyMonitorConfig hotKeyMonitorConfig;
    private boolean hotKeyCacheEnable = Constants.Server.hotKeyCacheEnable;
    private HotKeyCacheConfig hotKeyCacheConfig;
    private boolean bigKeyMonitorEnable = Constants.Server.bigKeyMonitorEnable;
    private BigKeyMonitorConfig bigKeyMonitorConfig;
    private boolean converterEnable = Constants.Server.converterEnable;
    private ConverterConfig converterConfig;
    private String proxyDynamicConfHookClassName;
    private boolean monitorDataMaskPassword = Constants.Server.monitorDataMaskPassword;


    private int bossThread = 1;
    private int workThread = Constants.Server.workThread;
    private int soBacklog = Constants.Server.soBacklog;
    private int soSndbuf = Constants.Server.soSndbuf;
    private int soRcvbuf = Constants.Server.soRcvbuf;
    private int writeBufferWaterMarkLow = Constants.Server.writeBufferWaterMarkLow;
    private int writeBufferWaterMarkHigh = Constants.Server.writeBufferWaterMarkHigh;
    private int commandDecodeMaxBatchSize = Constants.Server.commandDecodeMaxBatchSize;
    private int commandDecodeBufferInitializerSize = Constants.Server.commandDecodeBufferInitializerSize;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
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

    public int getCommandDecodeBufferInitializerSize() {
        return commandDecodeBufferInitializerSize;
    }

    public void setCommandDecodeBufferInitializerSize(int commandDecodeBufferInitializerSize) {
        this.commandDecodeBufferInitializerSize = commandDecodeBufferInitializerSize;
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

    public boolean isBigKeyMonitorEnable() {
        return bigKeyMonitorEnable;
    }

    public void setBigKeyMonitorEnable(boolean bigKeyMonitorEnable) {
        this.bigKeyMonitorEnable = bigKeyMonitorEnable;
    }

    public BigKeyMonitorConfig getBigKeyMonitorConfig() {
        return bigKeyMonitorConfig;
    }

    public void setBigKeyMonitorConfig(BigKeyMonitorConfig bigKeyMonitorConfig) {
        this.bigKeyMonitorConfig = bigKeyMonitorConfig;
    }

    public boolean isConverterEnable() {
        return converterEnable;
    }

    public void setConverterEnable(boolean converterEnable) {
        this.converterEnable = converterEnable;
    }

    public ConverterConfig getConverterConfig() {
        return converterConfig;
    }

    public void setConverterConfig(ConverterConfig converterConfig) {
        this.converterConfig = converterConfig;
    }

    public String getProxyDynamicConfHookClassName() {
        return proxyDynamicConfHookClassName;
    }

    public void setProxyDynamicConfHookClassName(String proxyDynamicConfHookClassName) {
        this.proxyDynamicConfHookClassName = proxyDynamicConfHookClassName;
    }

    public boolean isMonitorDataMaskPassword() {
        return monitorDataMaskPassword;
    }

    public void setMonitorDataMaskPassword(boolean monitorDataMaskPassword) {
        this.monitorDataMaskPassword = monitorDataMaskPassword;
    }

    public static class HotKeyMonitorConfig {
        private long checkMillis = Constants.Server.hotKeyMonitorCheckMillis;
        private int checkCacheMaxCapacity = Constants.Server.hotKeyMonitorCheckCacheMaxCapacity;
        private long checkThreshold = Constants.Server.hotKeyMonitorCheckThreshold;
        private int maxHotKeyCount = Constants.Server.hotKeyMonitorMaxHotKeyCount;
        private String hotKeyMonitorCallbackClassName = Constants.Server.hotKeyMonitorCallbackClassName;

        public long getCheckMillis() {
            return checkMillis;
        }

        public void setCheckMillis(long checkMillis) {
            this.checkMillis = checkMillis;
        }

        public int getCheckCacheMaxCapacity() {
            return checkCacheMaxCapacity;
        }

        public void setCheckCacheMaxCapacity(int checkCacheMaxCapacity) {
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
        private long cacheExpireMillis = Constants.Server.hotKeyCacheExpireMillis;
        private int cacheMaxCapacity = Constants.Server.hotKeyCacheMaxCapacity;

        private long counterCheckMillis = Constants.Server.hotKeyCacheCounterCheckMillis;
        private int counterMaxCapacity = Constants.Server.hotKeyCacheCounterMaxCapacity;
        private long counterCheckThreshold = Constants.Server.hotKeyCacheCounterCheckThreshold;
        private boolean needCacheNull = Constants.Server.hotKeyCacheNeedCacheNull;

        private String cacheKeyCheckerClassName = Constants.Server.hotKeyCacheKeyCheckerClassName;

        private long hotKeyCacheStatsCallbackIntervalSeconds = Constants.Server.hotKeyCacheStatsCallbackIntervalSeconds;
        private String hotKeyCacheStatsCallbackClassName = Constants.Server.hotKeyCacheStatsCallbackClassName;

        public long getCacheExpireMillis() {
            return cacheExpireMillis;
        }

        public void setCacheExpireMillis(long cacheExpireMillis) {
            this.cacheExpireMillis = cacheExpireMillis;
        }

        public int getCacheMaxCapacity() {
            return cacheMaxCapacity;
        }

        public void setCacheMaxCapacity(int cacheMaxCapacity) {
            this.cacheMaxCapacity = cacheMaxCapacity;
        }

        public long getCounterCheckMillis() {
            return counterCheckMillis;
        }

        public void setCounterCheckMillis(long counterCheckMillis) {
            this.counterCheckMillis = counterCheckMillis;
        }

        public int getCounterMaxCapacity() {
            return counterMaxCapacity;
        }

        public void setCounterMaxCapacity(int counterMaxCapacity) {
            this.counterMaxCapacity = counterMaxCapacity;
        }

        public long getCounterCheckThreshold() {
            return counterCheckThreshold;
        }

        public void setCounterCheckThreshold(long counterCheckThreshold) {
            this.counterCheckThreshold = counterCheckThreshold;
        }

        public String getCacheKeyCheckerClassName() {
            return cacheKeyCheckerClassName;
        }

        public void setCacheKeyCheckerClassName(String cacheKeyCheckerClassName) {
            this.cacheKeyCheckerClassName = cacheKeyCheckerClassName;
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

        public boolean isNeedCacheNull() {
            return needCacheNull;
        }

        public void setNeedCacheNull(boolean needCacheNull) {
            this.needCacheNull = needCacheNull;
        }
    }

    public static class BigKeyMonitorConfig {
        private int stringSizeThreshold = Constants.Server.bigKeyStringSizeThreshold;
        private int listSizeThreshold = Constants.Server.bigKeyListSizeThreshold;
        private int zsetSizeThreshold = Constants.Server.bigKeyZsetSizeThreshold;
        private int hashSizeThreshold = Constants.Server.bigKeyHashSizeThreshold;
        private int setSizeThreshold = Constants.Server.bigKeySetSizeThreshold;
        private String bigKeyMonitorCallbackClassName = Constants.Server.bigKeyMonitorCallbackClassName;

        public int getStringSizeThreshold() {
            return stringSizeThreshold;
        }

        public void setStringSizeThreshold(int stringSizeThreshold) {
            this.stringSizeThreshold = stringSizeThreshold;
        }

        public int getListSizeThreshold() {
            return listSizeThreshold;
        }

        public void setListSizeThreshold(int listSizeThreshold) {
            this.listSizeThreshold = listSizeThreshold;
        }

        public int getZsetSizeThreshold() {
            return zsetSizeThreshold;
        }

        public void setZsetSizeThreshold(int zsetSizeThreshold) {
            this.zsetSizeThreshold = zsetSizeThreshold;
        }

        public int getHashSizeThreshold() {
            return hashSizeThreshold;
        }

        public void setHashSizeThreshold(int hashSizeThreshold) {
            this.hashSizeThreshold = hashSizeThreshold;
        }

        public int getSetSizeThreshold() {
            return setSizeThreshold;
        }

        public void setSetSizeThreshold(int setSizeThreshold) {
            this.setSizeThreshold = setSizeThreshold;
        }

        public String getBigKeyMonitorCallbackClassName() {
            return bigKeyMonitorCallbackClassName;
        }

        public void setBigKeyMonitorCallbackClassName(String bigKeyMonitorCallbackClassName) {
            this.bigKeyMonitorCallbackClassName = bigKeyMonitorCallbackClassName;
        }
    }

    public static class ConverterConfig {
        private String stringConverterClassName;
        private String setConverterClassName;
        private String listConverterClassName;
        private String hashConverterClassName;
        private String zsetConverterClassName;

        public String getStringConverterClassName() {
            return stringConverterClassName;
        }

        public void setStringConverterClassName(String stringConverterClassName) {
            this.stringConverterClassName = stringConverterClassName;
        }

        public String getSetConverterClassName() {
            return setConverterClassName;
        }

        public void setSetConverterClassName(String setConverterClassName) {
            this.setConverterClassName = setConverterClassName;
        }

        public String getListConverterClassName() {
            return listConverterClassName;
        }

        public void setListConverterClassName(String listConverterClassName) {
            this.listConverterClassName = listConverterClassName;
        }

        public String getHashConverterClassName() {
            return hashConverterClassName;
        }

        public void setHashConverterClassName(String hashConverterClassName) {
            this.hashConverterClassName = hashConverterClassName;
        }

        public String getZsetConverterClassName() {
            return zsetConverterClassName;
        }

        public void setZsetConverterClassName(String zsetConverterClassName) {
            this.zsetConverterClassName = zsetConverterClassName;
        }
    }
}
