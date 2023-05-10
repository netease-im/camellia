package com.netease.nim.camellia.hot.key.server.springboot.conf;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.hot.key.server.callback.LoggingHotKeyCallback;
import com.netease.nim.camellia.hot.key.server.callback.LoggingHotKeyTopNCallback;
import org.springframework.boot.context.properties.ConfigurationProperties;

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

    private int maxNamespace = HotKeyConstants.Server.maxNamespace;

    private String hotKeyConfigServiceClassName;
    private int hotKeyCacheCounterCapacity = HotKeyConstants.Server.hotKeyCacheCounterCapacity;
    private int hotKeyCacheCapacity = HotKeyConstants.Server.hotKeyCacheCapacity;
    private int hotKeyCallbackIntervalSeconds = HotKeyConstants.Server.hotKeyCallbackIntervalSeconds;
    private String hotKeyCallbackClassName = LoggingHotKeyCallback.class.getName();

    private int topnCount = HotKeyConstants.Server.topnCount;
    private int topnCheckMillis = HotKeyConstants.Server.topnCheckMillis;
    private int topnCacheCounterCapacity = HotKeyConstants.Server.topnCacheCounterCapacity;
    private int topnScheduleSeconds = HotKeyConstants.Server.topnScheduleSeconds;
    private String topnRedisKeyPrefix = HotKeyConstants.Server.topnRedisKeyPrefix;
    private int topnRedisExpireSeconds = HotKeyConstants.Server.topnRedisExpireSeconds;
    private String topnCallbackClassName = LoggingHotKeyTopNCallback.class.getName();

    private int callbackExecutorSize = HotKeyConstants.Server.callbackExecutorSize;

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

    public int getMaxNamespace() {
        return maxNamespace;
    }

    public void setMaxNamespace(int maxNamespace) {
        this.maxNamespace = maxNamespace;
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

    public String getHotKeyConfigServiceClassName() {
        return hotKeyConfigServiceClassName;
    }

    public void setHotKeyConfigServiceClassName(String hotKeyConfigServiceClassName) {
        this.hotKeyConfigServiceClassName = hotKeyConfigServiceClassName;
    }

    public int getCallbackExecutorSize() {
        return callbackExecutorSize;
    }

    public void setCallbackExecutorSize(int callbackExecutorSize) {
        this.callbackExecutorSize = callbackExecutorSize;
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

    public String getTopnCallbackClassName() {
        return topnCallbackClassName;
    }

    public void setTopnCallbackClassName(String topnCallbackClassName) {
        this.topnCallbackClassName = topnCallbackClassName;
    }

    public int getTopnCount() {
        return topnCount;
    }

    public void setTopnCount(int topnCount) {
        this.topnCount = topnCount;
    }

    public int getTopnCheckMillis() {
        return topnCheckMillis;
    }

    public void setTopnCheckMillis(int topnCheckMillis) {
        this.topnCheckMillis = topnCheckMillis;
    }

    public int getTopnCacheCounterCapacity() {
        return topnCacheCounterCapacity;
    }

    public void setTopnCacheCounterCapacity(int topnCacheCounterCapacity) {
        this.topnCacheCounterCapacity = topnCacheCounterCapacity;
    }

    public int getTopnScheduleSeconds() {
        return topnScheduleSeconds;
    }

    public void setTopnScheduleSeconds(int topnScheduleSeconds) {
        this.topnScheduleSeconds = topnScheduleSeconds;
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
}
