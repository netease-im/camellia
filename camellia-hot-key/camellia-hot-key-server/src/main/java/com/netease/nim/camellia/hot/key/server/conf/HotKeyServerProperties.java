package com.netease.nim.camellia.hot.key.server.conf;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.hot.key.server.bean.BeanFactory;
import com.netease.nim.camellia.hot.key.server.bean.DefaultBeanFactory;
import com.netease.nim.camellia.hot.key.server.callback.LoggingHotKeyCallback;
import com.netease.nim.camellia.hot.key.server.callback.LoggingHotKeyTopNCallback;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;

import java.util.UUID;

/**
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyServerProperties {

    private final String id = UUID.randomUUID().toString().replaceAll("-", "");
    private String applicationName;
    private int port = HotKeyConstants.Server.severPort;
    private int nettyBossThread = HotKeyConstants.Server.nettyBossThread;
    private int nettyWorkThread = HotKeyConstants.Server.nettyWorkThread;

    private int bizWorkThread = HotKeyConstants.Server.bizWorkThread;
    private int bizQueueCapacity = HotKeyConstants.Server.bizWorkQueueCapacity;

    private boolean tcpNoDelay = HotKeyConstants.Server.tcpNoDelay;
    private int soBacklog = HotKeyConstants.Server.soBacklog;
    private int soSndbuf = HotKeyConstants.Server.soSndbuf;
    private int soRcvbuf = HotKeyConstants.Server.soRcvbuf;
    private boolean soKeepalive = HotKeyConstants.Server.soKeepalive;
    private int writeBufferWaterMarkLow = HotKeyConstants.Server.writeBufferWaterMarkLow;
    private int writeBufferWaterMarkHigh = HotKeyConstants.Server.writeBufferWaterMarkHigh;

    private int maxNamespace = HotKeyConstants.Server.maxNamespace;

    private BeanFactory beanFactory = DefaultBeanFactory.INSTANCE;
    private int callbackExecutorSize = HotKeyConstants.Server.callbackExecutorSize;

    private String hotKeyConfigServiceClassName = FileBasedHotKeyConfigService.class.getName();
    private int hotKeyCacheCounterCapacity = HotKeyConstants.Server.hotKeyCacheCounterCapacity;
    private int hotKeyCacheCapacity = HotKeyConstants.Server.hotKeyCacheCapacity;
    private int hotKeyCallbackIntervalSeconds = HotKeyConstants.Server.hotKeyCallbackIntervalSeconds;
    private String hotKeyCallbackClassName = LoggingHotKeyCallback.class.getName();

    private int topnCount = HotKeyConstants.Server.topnCount;
    private int topnCheckMillis = HotKeyConstants.Server.topnCheckMillis;
    private int topnCacheCounterCapacity = HotKeyConstants.Server.topnCacheCounterCapacity;
    private int topnScheduleSeconds = HotKeyConstants.Server.topnScheduleSeconds;
    private String topNCallbackClassName = LoggingHotKeyTopNCallback.class.getName();
    private String topnRedisKeyPrefix = HotKeyConstants.Server.topnRedisKeyPrefix;
    private int topnRedisExpireSeconds = HotKeyConstants.Server.topnRedisExpireSeconds;

    private CamelliaRedisTemplate redisTemplate;

    public String getId() {
        return id;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getNettyBossThread() {
        return nettyBossThread;
    }

    public void setNettyBossThread(int nettyBossThread) {
        this.nettyBossThread = nettyBossThread;
    }

    public int getNettyWorkThread() {
        return nettyWorkThread;
    }

    public void setNettyWorkThread(int nettyWorkThread) {
        this.nettyWorkThread = nettyWorkThread;
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

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
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

    public boolean isSoKeepalive() {
        return soKeepalive;
    }

    public void setSoKeepalive(boolean soKeepalive) {
        this.soKeepalive = soKeepalive;
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

    public String getHotKeyConfigServiceClassName() {
        return hotKeyConfigServiceClassName;
    }

    public void setHotKeyConfigServiceClassName(String hotKeyConfigServiceClassName) {
        this.hotKeyConfigServiceClassName = hotKeyConfigServiceClassName;
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
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

    public CamelliaRedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(CamelliaRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String getTopNCallbackClassName() {
        return topNCallbackClassName;
    }

    public void setTopNCallbackClassName(String topNCallbackClassName) {
        this.topNCallbackClassName = topNCallbackClassName;
    }
}
