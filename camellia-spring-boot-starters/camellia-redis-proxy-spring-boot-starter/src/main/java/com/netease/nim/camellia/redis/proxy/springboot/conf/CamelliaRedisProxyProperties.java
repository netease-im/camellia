package com.netease.nim.camellia.redis.proxy.springboot.conf;

import com.netease.nim.camellia.redis.proxy.conf.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * Created by caojiajun on 2019/11/13.
 */
@ConfigurationProperties(prefix = "camellia-redis-proxy")
public class CamelliaRedisProxyProperties {

    /**
     * proxy的密码
     */
    private String password;

    /**
     * proxy的console port，会起一个console http server，可以自己实现一些自定义指令
     */
    private int consolePort = Constants.Server.consolePort;

    /**
     *
     */
    private String customCommandInvokerClassName;

    /**
     * 是否开启监控，会记录每个命令的调用次数，see @RedisMonitor
     */
    private boolean monitorEnable = Constants.Server.monitorEnable;

    /**
     * 监控时间间隔，每个间隔生成一份监控数据，会打印到日志里，也可以用户自己选择输出到其他地方，see @RedisMonitor
     */
    private int monitorIntervalSeconds = Constants.Server.monitorIntervalSeconds;

    /**
     * 监控数据的回调
     */
    private String monitorCallbackClassName = Constants.Server.monitorCallbackClassName;

    /**
     * 是否开启监控命令执行时间的监控，需要同时开启monitorEnable和commandSpendTimeMonitorEnable才能生效，see @RedisMonitor
     */
    private boolean commandSpendTimeMonitorEnable = Constants.Server.commandSpendTimeMonitorEnable;

    /**
     * 开启监控命令执行时间的监控的前提下，慢查询的阈值，单位ms
     */
    private long slowCommandThresholdMillisTime = Constants.Server.slowCommandThresholdMillisTime;

    /**
     * 慢查询的回调方法
     */
    private String slowCommandCallbackClassName = Constants.Server.slowCommandMonitorCallbackClassName;

    /**
     * 命令拦截器，see @CommandInterceptor
     * 只有async模式才有效
     */
    private String commandInterceptorClassName;

    /**
     * hot-key监控开关
     */
    private boolean hotKeyMonitorEnable = Constants.Server.hotKeyMonitorEnable;

    /**
     * hot-key监控配置
     */
    private HotKeyMonitorConfig hotKeyMonitorConfig = new HotKeyMonitorConfig();

    /**
     * hot-key缓存开关
     */
    private boolean hotKeyCacheEnable = Constants.Server.hotKeyCacheEnable;

    /**
     * hot-key缓存配置
     */
    private HotKeyCacheConfig hotKeyCacheConfig = new HotKeyCacheConfig();

    /**
     * big-key监控开关
     */
    private boolean bigKeyMonitorEnable = Constants.Server.bigKeyMonitorEnable;

    /**
     * big-key监控配置
     */
    private BigKeyMonitorConfig bigKeyMonitorConfig = new BigKeyMonitorConfig();

    /**
     * netty相关参数
     */
    private NettyProperties netty = new NettyProperties();

    /**
     * 转发配置
     */
    private TranspondProperties transpond;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getConsolePort() {
        return consolePort;
    }

    public void setConsolePort(int consolePort) {
        this.consolePort = consolePort;
    }

    public String getMonitorCallbackClassName() {
        return monitorCallbackClassName;
    }

    public void setMonitorCallbackClassName(String monitorCallbackClassName) {
        this.monitorCallbackClassName = monitorCallbackClassName;
    }

    public String getCustomCommandInvokerClassName() {
        return customCommandInvokerClassName;
    }

    public void setCustomCommandInvokerClassName(String customCommandInvokerClassName) {
        this.customCommandInvokerClassName = customCommandInvokerClassName;
    }

    public boolean isMonitorEnable() {
        return monitorEnable;
    }

    public void setMonitorEnable(boolean monitorEnable) {
        this.monitorEnable = monitorEnable;
    }

    public int getMonitorIntervalSeconds() {
        return monitorIntervalSeconds;
    }

    public void setMonitorIntervalSeconds(int monitorIntervalSeconds) {
        this.monitorIntervalSeconds = monitorIntervalSeconds;
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

    public String getCommandInterceptorClassName() {
        return commandInterceptorClassName;
    }

    public void setCommandInterceptorClassName(String commandInterceptorClassName) {
        this.commandInterceptorClassName = commandInterceptorClassName;
    }

    public NettyProperties getNetty() {
        return netty;
    }

    public void setNetty(NettyProperties netty) {
        this.netty = netty;
    }

    public TranspondProperties getTranspond() {
        return transpond;
    }

    public void setTranspond(TranspondProperties transpond) {
        this.transpond = transpond;
    }

    public String getSlowCommandCallbackClassName() {
        return slowCommandCallbackClassName;
    }

    public void setSlowCommandCallbackClassName(String slowCommandCallbackClassName) {
        this.slowCommandCallbackClassName = slowCommandCallbackClassName;
    }

    public boolean isHotKeyMonitorEnable() {
        return hotKeyMonitorEnable;
    }

    public void setHotKeyMonitorEnable(boolean hotKeyMonitorEnable) {
        this.hotKeyMonitorEnable = hotKeyMonitorEnable;
    }

    public HotKeyMonitorConfig getHotKeyMonitorConfig() {
        return hotKeyMonitorConfig;
    }

    public void setHotKeyMonitorConfig(HotKeyMonitorConfig hotKeyMonitorConfig) {
        this.hotKeyMonitorConfig = hotKeyMonitorConfig;
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

        private String hotKeyCacheKeyCheckerClassName = Constants.Server.hotKeyCacheKeyCheckerClassName;

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
}
