package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.*;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyBeanFactory;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.springboot.conf.CamelliaRedisProxyProperties;
import com.netease.nim.camellia.redis.proxy.springboot.conf.NettyProperties;
import com.netease.nim.camellia.redis.proxy.springboot.conf.TranspondProperties;
import com.netease.nim.camellia.redis.base.resource.RedisResourceUtil;
import com.netease.nim.camellia.tools.utils.FileUtil;
import com.netease.nim.camellia.tools.utils.SysUtils;

import java.net.URL;

/**
 *
 * Created by caojiajun on 2020/4/3.
 */
public class CamelliaRedisProxyUtil {

    public static CamelliaServerProperties parse(CamelliaRedisProxyProperties properties, ProxyBeanFactory proxyBeanFactory, String applicationName, int port) {
        CamelliaServerProperties serverProperties = new CamelliaServerProperties();
        if (properties.getPort() == Constants.Server.serverPortRandSig) {
            serverProperties.setPort(Constants.Server.serverPortRandSig);
        } else if (properties.getPort() <= 0) {
            serverProperties.setPort(port);
        } else {
            serverProperties.setPort(properties.getPort());
        }
        if (properties.getApplicationName() != null && properties.getApplicationName().trim().length() == 0) {
            serverProperties.setApplicationName(applicationName);
        } else {
            serverProperties.setApplicationName(properties.getApplicationName());
        }
        serverProperties.setPassword(properties.getPassword());
        serverProperties.setTlsEnable(properties.isTlsEnable());
        serverProperties.setProxyFrontendTlsProviderClassName(properties.getProxyFrontendTlsProviderClassName());
        serverProperties.setMonitorEnable(properties.isMonitorEnable());
        serverProperties.setMonitorIntervalSeconds(properties.getMonitorIntervalSeconds());
        serverProperties.setMonitorCallbackClassName(properties.getMonitorCallbackClassName());
        serverProperties.setClientAuthProviderClassName(properties.getClientAuthProviderClassName());
        serverProperties.setUpstreamClientTemplateFactoryClassName(properties.getUpstreamClientTemplateFactoryClassName());
        serverProperties.setProxyDynamicConfLoaderClassName(properties.getProxyDynamicConfLoaderClassName());
        serverProperties.setProxyBeanFactory(proxyBeanFactory);
        serverProperties.setPlugins(properties.getPlugins());
        serverProperties.setConfig(properties.getConfig());
        serverProperties.setQueueFactoryClassName(properties.getQueueFactoryClassName());
        serverProperties.setNettyTransportMode(properties.getNettyTransportMode());
        NettyProperties netty = properties.getNetty();
        serverProperties.setBossThread(netty.getBossThread());
        if (netty.getWorkThread() > 0) {
            serverProperties.setWorkThread(netty.getWorkThread());
        } else {
            serverProperties.setWorkThread(SysUtils.getCpuNum());
        }
        serverProperties.setClusterModeEnable(properties.isClusterModeEnable());
        serverProperties.setClusterModeProviderClassName(properties.getClusterModeProviderClassName());
        serverProperties.setCport(properties.getCport());
        serverProperties.setCommandDecodeMaxBatchSize(netty.getCommandDecodeMaxBatchSize());
        serverProperties.setCommandDecodeBufferInitializerSize(netty.getCommandDecodeBufferInitializerSize());
        serverProperties.setTcpNoDelay(netty.isTcpNoDelay());
        serverProperties.setSoBacklog(netty.getSoBacklog());
        serverProperties.setSoRcvbuf(netty.getSoRcvbuf());
        serverProperties.setSoSndbuf(netty.getSoSndbuf());
        serverProperties.setSoKeepalive(netty.isSoKeepalive());
        serverProperties.setReaderIdleTimeSeconds(netty.getReaderIdleTimeSeconds());
        serverProperties.setWriterIdleTimeSeconds(netty.getWriterIdleTimeSeconds());
        serverProperties.setAllIdleTimeSeconds(netty.getAllIdleTimeSeconds());
        serverProperties.setWriteBufferWaterMarkLow(netty.getWriteBufferWaterMarkLow());
        serverProperties.setWriteBufferWaterMarkHigh(netty.getWriteBufferWaterMarkHigh());
        serverProperties.setTcpQuickAck(netty.isTcpQuickAck());
        return serverProperties;
    }

    public static CamelliaTranspondProperties.Type parseType(TranspondProperties properties) {
        CamelliaTranspondProperties.Type type = null;
        switch (properties.getType()) {
            case LOCAL:
                type = CamelliaTranspondProperties.Type.LOCAL;
                break;
            case REMOTE:
                type = CamelliaTranspondProperties.Type.REMOTE;
                break;
            case CUSTOM:
                type = CamelliaTranspondProperties.Type.CUSTOM;
                break;
        }
        return type;
    }

    public static CamelliaTranspondProperties.LocalProperties parse(TranspondProperties.LocalProperties properties) {
        if (properties == null) return null;
        CamelliaTranspondProperties.LocalProperties localProperties = new CamelliaTranspondProperties.LocalProperties();
        ResourceTable resourceTable;
        if (properties.getType() == TranspondProperties.LocalProperties.Type.SIMPLE) {
            String resource = properties.getResource();
            if (resource == null) {
                return localProperties;
            }
            resourceTable = ResourceTableUtil.simpleTable(new Resource(resource));
            RedisResourceUtil.checkResourceTable(resourceTable);
            localProperties.setResourceTable(resourceTable);
            return localProperties;
        } else if (properties.getType() == TranspondProperties.LocalProperties.Type.COMPLEX) {
            String jsonFile = properties.getJsonFile();
            if (jsonFile == null) {
                throw new IllegalArgumentException("missing 'jsonFile' of local");
            }
            String filePath = null;
            String fileContent;
            //先去classpath下找
            URL resource = Thread.currentThread().getContextClassLoader().getResource(jsonFile);
            if (resource != null) {
                filePath = resource.getPath();
                fileContent = FileUtil.readFileByPath(filePath);//尝试用文件路径的方式去加载
                if (fileContent == null) {
                    filePath = null;
                    fileContent = FileUtil.readFileByNameInStream(jsonFile);//尝试用流的方式去加载
                }
            } else {
                fileContent = FileUtil.readFileByNameInStream(jsonFile);//尝试用文件路径的方式去加载
            }
            if (fileContent == null) {
                filePath = jsonFile;
                fileContent = FileUtil.readFileByPath(filePath);//尝试当做绝对路径去加载
            }
            if (fileContent == null) {
                throw new IllegalArgumentException(jsonFile + " read fail");
            }
            fileContent = fileContent.trim();
            resourceTable = ReadableResourceTableUtil.parseTable(fileContent);
            RedisResourceUtil.checkResourceTable(resourceTable);
            boolean dynamic = properties.isDynamic();
            if (dynamic && filePath != null) {
                localProperties.setResourceTableFilePath(filePath);
                long checkIntervalMillis = properties.getCheckIntervalMillis();
                if (checkIntervalMillis <= 0) {
                    throw new IllegalArgumentException("local.checkIntervalMillis <= 0");
                }
                localProperties.setCheckIntervalMillis(checkIntervalMillis);
            } else {
                localProperties.setResourceTable(resourceTable);
            }
            return localProperties;
        } else {
            throw new IllegalArgumentException("not support type");
        }
    }

    public static CamelliaTranspondProperties.RemoteProperties parse(TranspondProperties.RemoteProperties properties) {
        if (properties == null) return null;
        CamelliaTranspondProperties.RemoteProperties remoteProperties = new CamelliaTranspondProperties.RemoteProperties();
        remoteProperties.setBid(properties.getBid());
        remoteProperties.setBgroup(properties.getBgroup());
        remoteProperties.setDynamic(properties.isDynamic());
        remoteProperties.setUrl(properties.getUrl());
        remoteProperties.setMonitorEnable(properties.isMonitor());
        remoteProperties.setCheckIntervalMillis(properties.getCheckIntervalMillis());
        remoteProperties.setConnectTimeoutMillis(properties.getConnectTimeoutMillis());
        remoteProperties.setReadTimeoutMillis(properties.getReadTimeoutMillis());
        remoteProperties.setHeaderMap(properties.getHeaderMap());
        return remoteProperties;
    }

    public static CamelliaTranspondProperties.CustomProperties parse(TranspondProperties.CustomProperties properties) {
        if (properties == null) return null;
        CamelliaTranspondProperties.CustomProperties customProperties = new CamelliaTranspondProperties.CustomProperties();
        customProperties.setBid(properties.getBid());
        customProperties.setBgroup(properties.getBgroup());
        customProperties.setDynamic(properties.isDynamic());
        customProperties.setReloadIntervalMillis(properties.getReloadIntervalMillis());
        customProperties.setProxyRouteConfUpdaterClassName(properties.getProxyRouteConfUpdaterClassName());
        return customProperties;
    }

    public static CamelliaTranspondProperties.RedisConfProperties parse(TranspondProperties.RedisConfProperties properties) {
        if (properties == null) return null;
        CamelliaTranspondProperties.RedisConfProperties redisConfProperties = new CamelliaTranspondProperties.RedisConfProperties();
        redisConfProperties.setShardingFunc(properties.getShardingFunc());
        redisConfProperties.setConnectTimeoutMillis(properties.getConnectTimeoutMillis());
        redisConfProperties.setFailBanMillis(properties.getFailBanMillis());
        redisConfProperties.setFailCountThreshold(properties.getFailCountThreshold());
        redisConfProperties.setHeartbeatIntervalSeconds(properties.getHeartbeatIntervalSeconds());
        redisConfProperties.setHeartbeatTimeoutMillis(properties.getHeartbeatTimeoutMillis());
        redisConfProperties.setRedisClusterMaxAttempts(properties.getRedisClusterMaxAttempts());
        redisConfProperties.setDefaultTranspondWorkThread(properties.getDefaultTranspondWorkThread());
        redisConfProperties.setPreheat(properties.isPreheat());
        redisConfProperties.setCloseIdleConnection(properties.isCloseIdleConnection());
        redisConfProperties.setCheckIdleConnectionThresholdSeconds(properties.getCheckIdleConnectionThresholdSeconds());
        redisConfProperties.setCloseIdleConnectionDelaySeconds(properties.getCloseIdleConnectionDelaySeconds());
        redisConfProperties.setProxyDiscoveryFactoryClassName(properties.getProxyDiscoveryFactoryClassName());
        return redisConfProperties;
    }

    public static CamelliaTranspondProperties.NettyProperties parse(TranspondProperties.NettyProperties properties) {
        if (properties == null) return null;
        CamelliaTranspondProperties.NettyProperties nettyProperties = new CamelliaTranspondProperties.NettyProperties();
        nettyProperties.setSoKeepalive(properties.isSoKeepalive());
        nettyProperties.setTcpNoDelay(properties.isTcpNoDelay());
        nettyProperties.setTcpQuickAck(properties.isTcpQuickAck());
        nettyProperties.setSoRcvbuf(properties.getSoRcvbuf());
        nettyProperties.setSoSndbuf(properties.getSoSndbuf());
        nettyProperties.setWriteBufferWaterMarkLow(properties.getWriteBufferWaterMarkLow());
        nettyProperties.setWriteBufferWaterMarkHigh(properties.getWriteBufferWaterMarkHigh());
        return nettyProperties;
    }
}
