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
    private String slowCommandCallbackClassName = Constants.Server.slowCommandCallbackClassName;

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

    public static class HotKeyMonitorConfig {

        private long checkPeriodMillis = Constants.Server.hotKeyCheckPeriodMillis;
        private long checkCacheMaxCapacity = Constants.Server.hotKeyCheckCacheMaxCapacity;
        private long checkThreshold = Constants.Server.hotKeyCheckThreshold;
        private int maxHotKeyCount = Constants.Server.hotKeyMaxHotKeyCount;
        private String hotKeyCallbackClassName = Constants.Server.hotKeyCallbackClassName;

        public long getCheckPeriodMillis() {
            return checkPeriodMillis;
        }

        public void setCheckPeriodMillis(long checkPeriodMillis) {
            this.checkPeriodMillis = checkPeriodMillis;
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

        public String getHotKeyCallbackClassName() {
            return hotKeyCallbackClassName;
        }

        public void setHotKeyCallbackClassName(String hotKeyCallbackClassName) {
            this.hotKeyCallbackClassName = hotKeyCallbackClassName;
        }
    }
}
