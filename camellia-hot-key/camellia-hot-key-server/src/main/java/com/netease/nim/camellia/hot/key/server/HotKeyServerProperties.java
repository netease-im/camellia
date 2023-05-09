package com.netease.nim.camellia.hot.key.server;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;

/**
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyServerProperties {

    private int port = HotKeyConstants.Server.severPort;
    private int nettyBossThread = HotKeyConstants.Server.nettyBossThread;
    private int nettyWorkThread = HotKeyConstants.Server.nettyWorkThread;
    private int bizWorkThread = HotKeyConstants.Server.bizWorkThread;
    private int bizQueueCapacity = HotKeyConstants.Server.bizWorkQueueCapacity;

    private int maxNamespace = HotKeyConstants.Server.maxNamespace;
    private int cacheCapacityPerNamespace = HotKeyConstants.Server.cacheCapacityPerNamespace;

    private boolean tcpNoDelay = HotKeyConstants.Server.tcpNoDelay;
    private int soBacklog = HotKeyConstants.Server.soBacklog;
    private int soSndbuf = HotKeyConstants.Server.soSndbuf;
    private int soRcvbuf = HotKeyConstants.Server.soRcvbuf;
    private boolean soKeepalive = HotKeyConstants.Server.soKeepalive;
    private int writeBufferWaterMarkLow = HotKeyConstants.Server.writeBufferWaterMarkLow;
    private int writeBufferWaterMarkHigh = HotKeyConstants.Server.writeBufferWaterMarkHigh;

    private String hotKeyConfigServiceClassName;
    private BeanFactory beanFactory = DefaultBeanFactory.INSTANCE;

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

    public int getCacheCapacityPerNamespace() {
        return cacheCapacityPerNamespace;
    }

    public void setCacheCapacityPerNamespace(int cacheCapacityPerNamespace) {
        this.cacheCapacityPerNamespace = cacheCapacityPerNamespace;
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

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public String getHotKeyConfigServiceClassName() {
        return hotKeyConfigServiceClassName;
    }

    public void setHotKeyConfigServiceClassName(String hotKeyConfigServiceClassName) {
        this.hotKeyConfigServiceClassName = hotKeyConfigServiceClassName;
    }
}
