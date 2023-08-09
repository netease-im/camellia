package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.redis.proxy.netty.NettyTransportMode;
import com.netease.nim.camellia.redis.proxy.plugin.DefaultBeanFactory;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyBeanFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/11/11.
 */
public class CamelliaServerProperties {
    private int port = Constants.Server.severPort;
    private int tlsPort = -1;
    private int cport = -1;
    private String applicationName;
    private String password;
    private String proxyFrontendTlsProviderClassName = Constants.Server.proxyFrontendTlsProviderClassName;
    private boolean monitorEnable = Constants.Server.monitorEnable;
    private int monitorIntervalSeconds = Constants.Server.monitorIntervalSeconds;
    private ProxyBeanFactory proxyBeanFactory = DefaultBeanFactory.INSTANCE;
    private String monitorCallbackClassName = Constants.Server.monitorCallbackClassName;
    private String clientAuthProviderClassName = Constants.Server.clientAuthByConfigProvider;
    private String upstreamClientTemplateFactoryClassName = Constants.Server.upstreamClientTemplateFactoryClassName;

    private String proxyDynamicConfLoaderClassName = Constants.Server.proxyDynamicConfLoaderClassName;

    private List<String> plugins = new ArrayList<>();

    private Map<String, String> config = new HashMap<>();

    private boolean clusterModeEnable = Constants.Server.clusterModeEnable;
    private String clusterModeProviderClassName = Constants.Server.clusterModeProviderClassName;
    private String queueFactoryClassName = Constants.Server.queueFactoryClassName;

    private NettyTransportMode nettyTransportMode = Constants.Server.nettyTransportMode;
    private int bossThread = 1;
    private int workThread = Constants.Server.workThread;
    private boolean tcpNoDelay = Constants.Server.tcpNoDelay;
    private int soBacklog = Constants.Server.soBacklog;
    private int soSndbuf = Constants.Server.soSndbuf;
    private int soRcvbuf = Constants.Server.soRcvbuf;
    private boolean soKeepalive = Constants.Server.soKeepalive;
    public boolean tcpQuickAck =  Constants.Server.tcpQuickAck;
    private int readerIdleTimeSeconds = Constants.Server.readerIdleTimeSeconds;
    private int writerIdleTimeSeconds = Constants.Server.writerIdleTimeSeconds;
    private int allIdleTimeSeconds = Constants.Server.allIdleTimeSeconds;
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

    public int getTlsPort() {
        return tlsPort;
    }

    public void setTlsPort(int tlsPort) {
        this.tlsPort = tlsPort;
    }

    public int getCport() {
        return cport;
    }

    public void setCport(int cport) {
        this.cport = cport;
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

    public String getProxyFrontendTlsProviderClassName() {
        return proxyFrontendTlsProviderClassName;
    }

    public void setProxyFrontendTlsProviderClassName(String proxyFrontendTlsProviderClassName) {
        this.proxyFrontendTlsProviderClassName = proxyFrontendTlsProviderClassName;
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

    public ProxyBeanFactory getProxyBeanFactory() {
        return proxyBeanFactory;
    }

    public void setProxyBeanFactory(ProxyBeanFactory proxyBeanFactory) {
        if (proxyBeanFactory != null) {
            this.proxyBeanFactory = proxyBeanFactory;
        }
    }

    public String getMonitorCallbackClassName() {
        return monitorCallbackClassName;
    }

    public void setMonitorCallbackClassName(String monitorCallbackClassName) {
        this.monitorCallbackClassName = monitorCallbackClassName;
    }

    public String getClientAuthProviderClassName() {
        return clientAuthProviderClassName;
    }

    public void setClientAuthProviderClassName(String clientAuthProviderClassName) {
        this.clientAuthProviderClassName = clientAuthProviderClassName;
    }

    public String getUpstreamClientTemplateFactoryClassName() {
        return upstreamClientTemplateFactoryClassName;
    }

    public void setUpstreamClientTemplateFactoryClassName(String upstreamClientTemplateFactoryClassName) {
        this.upstreamClientTemplateFactoryClassName = upstreamClientTemplateFactoryClassName;
    }

    public String getProxyDynamicConfLoaderClassName() {
        return proxyDynamicConfLoaderClassName;
    }

    public void setProxyDynamicConfLoaderClassName(String proxyDynamicConfLoaderClassName) {
        this.proxyDynamicConfLoaderClassName = proxyDynamicConfLoaderClassName;
    }

    public List<String> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<String> plugins) {
        this.plugins = plugins;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public boolean isClusterModeEnable() {
        return clusterModeEnable;
    }

    public void setClusterModeEnable(boolean clusterModeEnable) {
        this.clusterModeEnable = clusterModeEnable;
    }

    public String getClusterModeProviderClassName() {
        return clusterModeProviderClassName;
    }

    public void setClusterModeProviderClassName(String clusterModeProviderClassName) {
        this.clusterModeProviderClassName = clusterModeProviderClassName;
    }

    public String getQueueFactoryClassName() {
        return queueFactoryClassName;
    }

    public void setQueueFactoryClassName(String queueFactoryClassName) {
        this.queueFactoryClassName = queueFactoryClassName;
    }

    public NettyTransportMode getNettyTransportMode() {
        return nettyTransportMode;
    }

    public void setNettyTransportMode(NettyTransportMode nettyTransportMode) {
        this.nettyTransportMode = nettyTransportMode;
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

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public boolean isTcpQuickAck() {
        return tcpQuickAck;
    }

    public void setTcpQuickAck(boolean tcpQuickAck) {
        this.tcpQuickAck = tcpQuickAck;
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

    public int getReaderIdleTimeSeconds() {
        return readerIdleTimeSeconds;
    }

    public void setReaderIdleTimeSeconds(int readerIdleTimeSeconds) {
        this.readerIdleTimeSeconds = readerIdleTimeSeconds;
    }

    public int getWriterIdleTimeSeconds() {
        return writerIdleTimeSeconds;
    }

    public void setWriterIdleTimeSeconds(int writerIdleTimeSeconds) {
        this.writerIdleTimeSeconds = writerIdleTimeSeconds;
    }

    public int getAllIdleTimeSeconds() {
        return allIdleTimeSeconds;
    }

    public void setAllIdleTimeSeconds(int allIdleTimeSeconds) {
        this.allIdleTimeSeconds = allIdleTimeSeconds;
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
}
