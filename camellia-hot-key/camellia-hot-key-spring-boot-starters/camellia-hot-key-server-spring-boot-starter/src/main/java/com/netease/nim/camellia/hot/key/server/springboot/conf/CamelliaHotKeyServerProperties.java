package com.netease.nim.camellia.hot.key.server.springboot.conf;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.hot.key.server.callback.LoggingHotKeyCacheStatsCallback;
import com.netease.nim.camellia.hot.key.server.callback.LoggingHotKeyCallback;
import com.netease.nim.camellia.hot.key.server.callback.LoggingHotKeyTopNCallback;
import com.netease.nim.camellia.hot.key.server.callback.LoggingMonitorCallback;
import com.netease.nim.camellia.hot.key.server.conf.FileBasedHotKeyConfigService;
import com.netease.nim.camellia.hot.key.server.conf.WorkQueueType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2023/5/10
 */
@ConfigurationProperties(prefix = "camellia-hot-key-server")
public class CamelliaHotKeyServerProperties {

    private int consolePort = HotKeyConstants.Server.consolePort;

    private NettyProperties netty = new NettyProperties();

    private int bizWorkThread = -1;
    private int bizQueueCapacity = HotKeyConstants.Server.bizWorkQueueCapacity;
    private WorkQueueType workQueueType = WorkQueueType.ConcurrentLinkedQueue;

    private int maxNamespace = HotKeyConstants.Server.maxNamespace;

    private String hotKeyConfigServiceClassName = FileBasedHotKeyConfigService.class.getName();
    private int hotKeyCacheCounterCapacity = HotKeyConstants.Server.hotKeyCacheCounterCapacity;
    private int hotKeyCacheCapacity = HotKeyConstants.Server.hotKeyCacheCapacity;
    private int hotKeyCallbackIntervalSeconds = HotKeyConstants.Server.hotKeyCallbackIntervalSeconds;
    private String hotKeyCallbackClassName = LoggingHotKeyCallback.class.getName();

    private int topnCount = HotKeyConstants.Server.topnCount;
    private int topnCacheCounterCapacity = HotKeyConstants.Server.topnCacheCounterCapacity;
    private int topnCollectSeconds = HotKeyConstants.Server.topnCollectSeconds;
    private int topnTinyCollectSeconds = HotKeyConstants.Server.topnTinyCollectSeconds;
    private String topnRedisKeyPrefix = HotKeyConstants.Server.topnRedisKeyPrefix;
    private int topnRedisExpireSeconds = HotKeyConstants.Server.topnRedisExpireSeconds;
    private String topnCallbackClassName = LoggingHotKeyTopNCallback.class.getName();

    private int callbackExecutorSize = HotKeyConstants.Server.callbackExecutorSize;

    private String hotKeyCacheStatsCallbackClassName = LoggingHotKeyCacheStatsCallback.class.getName();

    private String monitorCallbackClassName = LoggingMonitorCallback.class.getName();

    private int monitorIntervalSeconds = HotKeyConstants.Server.monitorIntervalSeconds;
    private int monitorHotKeyMaxCount = HotKeyConstants.Server.monitorHotKeyMaxCount;

    private Map<String, String> config = new HashMap<>();

    public int getConsolePort() {
        return consolePort;
    }

    public void setConsolePort(int consolePort) {
        this.consolePort = consolePort;
    }

    public NettyProperties getNetty() {
        return netty;
    }

    public void setNetty(NettyProperties netty) {
        this.netty = netty;
    }

    public int getBizWorkThread() {
        return bizWorkThread;
    }

    public void setBizWorkThread(int bizWorkThread) {
        this.bizWorkThread = bizWorkThread;
    }

    public int getBizQueueCapacity() {
        return bizQueueCapacity;
    }

    public void setBizQueueCapacity(int bizQueueCapacity) {
        this.bizQueueCapacity = bizQueueCapacity;
    }

    public WorkQueueType getWorkQueueType() {
        return workQueueType;
    }

    public void setWorkQueueType(WorkQueueType workQueueType) {
        this.workQueueType = workQueueType;
    }

    public int getMaxNamespace() {
        return maxNamespace;
    }

    public void setMaxNamespace(int maxNamespace) {
        this.maxNamespace = maxNamespace;
    }

    public String getHotKeyConfigServiceClassName() {
        return hotKeyConfigServiceClassName;
    }

    public void setHotKeyConfigServiceClassName(String hotKeyConfigServiceClassName) {
        this.hotKeyConfigServiceClassName = hotKeyConfigServiceClassName;
    }

    public int getHotKeyCacheCounterCapacity() {
        return hotKeyCacheCounterCapacity;
    }

    public void setHotKeyCacheCounterCapacity(int hotKeyCacheCounterCapacity) {
        this.hotKeyCacheCounterCapacity = hotKeyCacheCounterCapacity;
    }

    public int getHotKeyCacheCapacity() {
        return hotKeyCacheCapacity;
    }

    public void setHotKeyCacheCapacity(int hotKeyCacheCapacity) {
        this.hotKeyCacheCapacity = hotKeyCacheCapacity;
    }

    public int getHotKeyCallbackIntervalSeconds() {
        return hotKeyCallbackIntervalSeconds;
    }

    public void setHotKeyCallbackIntervalSeconds(int hotKeyCallbackIntervalSeconds) {
        this.hotKeyCallbackIntervalSeconds = hotKeyCallbackIntervalSeconds;
    }

    public String getHotKeyCallbackClassName() {
        return hotKeyCallbackClassName;
    }

    public void setHotKeyCallbackClassName(String hotKeyCallbackClassName) {
        this.hotKeyCallbackClassName = hotKeyCallbackClassName;
    }

    public int getTopnCount() {
        return topnCount;
    }

    public void setTopnCount(int topnCount) {
        this.topnCount = topnCount;
    }

    public int getTopnCacheCounterCapacity() {
        return topnCacheCounterCapacity;
    }

    public void setTopnCacheCounterCapacity(int topnCacheCounterCapacity) {
        this.topnCacheCounterCapacity = topnCacheCounterCapacity;
    }

    public int getTopnCollectSeconds() {
        return topnCollectSeconds;
    }

    public void setTopnCollectSeconds(int topnCollectSeconds) {
        this.topnCollectSeconds = topnCollectSeconds;
    }

    public int getTopnTinyCollectSeconds() {
        return topnTinyCollectSeconds;
    }

    public void setTopnTinyCollectSeconds(int topnTinyCollectSeconds) {
        this.topnTinyCollectSeconds = topnTinyCollectSeconds;
    }

    public String getTopnRedisKeyPrefix() {
        return topnRedisKeyPrefix;
    }

    public void setTopnRedisKeyPrefix(String topnRedisKeyPrefix) {
        this.topnRedisKeyPrefix = topnRedisKeyPrefix;
    }

    public int getTopnRedisExpireSeconds() {
        return topnRedisExpireSeconds;
    }

    public void setTopnRedisExpireSeconds(int topnRedisExpireSeconds) {
        this.topnRedisExpireSeconds = topnRedisExpireSeconds;
    }

    public String getTopnCallbackClassName() {
        return topnCallbackClassName;
    }

    public void setTopnCallbackClassName(String topnCallbackClassName) {
        this.topnCallbackClassName = topnCallbackClassName;
    }

    public int getCallbackExecutorSize() {
        return callbackExecutorSize;
    }

    public void setCallbackExecutorSize(int callbackExecutorSize) {
        this.callbackExecutorSize = callbackExecutorSize;
    }

    public int getMonitorIntervalSeconds() {
        return monitorIntervalSeconds;
    }

    public void setMonitorIntervalSeconds(int monitorIntervalSeconds) {
        this.monitorIntervalSeconds = monitorIntervalSeconds;
    }

    public int getMonitorHotKeyMaxCount() {
        return monitorHotKeyMaxCount;
    }

    public void setMonitorHotKeyMaxCount(int monitorHotKeyMaxCount) {
        this.monitorHotKeyMaxCount = monitorHotKeyMaxCount;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public String getHotKeyCacheStatsCallbackClassName() {
        return hotKeyCacheStatsCallbackClassName;
    }

    public void setHotKeyCacheStatsCallbackClassName(String hotKeyCacheStatsCallbackClassName) {
        this.hotKeyCacheStatsCallbackClassName = hotKeyCacheStatsCallbackClassName;
    }

    public String getMonitorCallbackClassName() {
        return monitorCallbackClassName;
    }

    public void setMonitorCallbackClassName(String monitorCallbackClassName) {
        this.monitorCallbackClassName = monitorCallbackClassName;
    }
}
