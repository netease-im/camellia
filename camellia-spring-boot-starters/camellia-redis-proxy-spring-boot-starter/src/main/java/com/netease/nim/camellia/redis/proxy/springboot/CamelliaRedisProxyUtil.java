package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.*;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.async.route.ProxyRouteConfUpdater;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.springboot.conf.CamelliaRedisProxyProperties;
import com.netease.nim.camellia.redis.proxy.springboot.conf.NettyProperties;
import com.netease.nim.camellia.redis.proxy.springboot.conf.TranspondProperties;
import com.netease.nim.camellia.redis.resource.RedisResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 *
 * Created by caojiajun on 2020/4/3.
 */
public class CamelliaRedisProxyUtil {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisProxyUtil.class);

    public static CamelliaServerProperties parse(CamelliaRedisProxyProperties properties, CamelliaRedisProxyConfigurerSupport support, String applicationName, int port) {
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
        serverProperties.setMonitorEnable(properties.isMonitorEnable());
        serverProperties.setMonitorIntervalSeconds(properties.getMonitorIntervalSeconds());
        serverProperties.setMonitorCallback(support.getMonitorCallback());
        serverProperties.setMonitorCallbackClassName(properties.getMonitorCallbackClassName());
        serverProperties.setCommandSpendTimeMonitorEnable(properties.isCommandSpendTimeMonitorEnable());
        serverProperties.setSlowCommandThresholdMillisTime(properties.getSlowCommandThresholdMillisTime());
        serverProperties.setCommandInterceptorClassName(properties.getCommandInterceptorClassName());
        serverProperties.setCommandInterceptor(support.getCommandInterceptor());
        serverProperties.setSlowCommandCallbackClassName(properties.getSlowCommandCallbackClassName());
        serverProperties.setSlowCommandMonitorCallback(support.getSlowCommandMonitorCallback());
        serverProperties.setClientAuthProviderClassName(properties.getClientAuthProviderClassName());
        serverProperties.setClientAuthProvider(support.getClientAuthProvider());
        NettyProperties netty = properties.getNetty();
        serverProperties.setBossThread(netty.getBossThread());
        if (netty.getWorkThread() > 0) {
            serverProperties.setWorkThread(netty.getWorkThread());
        } else {
            serverProperties.setWorkThread(SysUtils.getCpuNum());
        }
        serverProperties.setCommandDecodeMaxBatchSize(netty.getCommandDecodeMaxBatchSize());
        serverProperties.setCommandDecodeBufferInitializerSize(netty.getCommandDecodeBufferInitializerSize());
        serverProperties.setSoBacklog(netty.getSoBacklog());
        serverProperties.setSoRcvbuf(netty.getSoRcvbuf());
        serverProperties.setSoSndbuf(netty.getSoSndbuf());
        serverProperties.setWriteBufferWaterMarkLow(netty.getWriteBufferWaterMarkLow());
        serverProperties.setWriteBufferWaterMarkHigh(netty.getWriteBufferWaterMarkHigh());

        CamelliaRedisProxyProperties.HotKeyMonitorConfig hotKeyMonitorConfig = properties.getHotKeyMonitorConfig();
        CamelliaServerProperties.HotKeyMonitorConfig config = new CamelliaServerProperties.HotKeyMonitorConfig();
        config.setCheckCacheMaxCapacity(hotKeyMonitorConfig.getCheckCacheMaxCapacity());
        config.setCheckMillis(hotKeyMonitorConfig.getCheckMillis());
        config.setCheckThreshold(hotKeyMonitorConfig.getCheckThreshold());
        config.setHotKeyMonitorCallbackClassName(hotKeyMonitorConfig.getHotKeyMonitorCallbackClassName());
        config.setHotKeyMonitorCallback(support.getHotKeyMonitorCallback());
        config.setMaxHotKeyCount(hotKeyMonitorConfig.getMaxHotKeyCount());
        serverProperties.setHotKeyMonitorConfig(config);
        serverProperties.setHotKeyMonitorEnable(properties.isHotKeyMonitorEnable());

        CamelliaRedisProxyProperties.HotKeyCacheConfig hotKeyCacheConfig = properties.getHotKeyCacheConfig();
        CamelliaServerProperties.HotKeyCacheConfig cacheConfig = new CamelliaServerProperties.HotKeyCacheConfig();
        cacheConfig.setCounterCheckThreshold(hotKeyCacheConfig.getCounterCheckThreshold());
        cacheConfig.setCounterCheckMillis(hotKeyCacheConfig.getCounterCheckMillis());
        cacheConfig.setCounterMaxCapacity(hotKeyCacheConfig.getCounterMaxCapacity());
        cacheConfig.setCacheExpireMillis(hotKeyCacheConfig.getCacheExpireMillis());
        cacheConfig.setCacheKeyCheckerClassName(hotKeyCacheConfig.getHotKeyCacheKeyCheckerClassName());
        cacheConfig.setHotKeyCacheKeyChecker(support.getHotKeyCacheKeyChecker());
        cacheConfig.setCacheMaxCapacity(hotKeyCacheConfig.getCacheMaxCapacity());
        cacheConfig.setHotKeyCacheStatsCallbackClassName(hotKeyCacheConfig.getHotKeyCacheStatsCallbackClassName());
        cacheConfig.setHotKeyCacheStatsCallback(support.getHotKeyCacheStatsCallback());
        cacheConfig.setHotKeyCacheStatsCallbackIntervalSeconds(hotKeyCacheConfig.getHotKeyCacheStatsCallbackIntervalSeconds());
        cacheConfig.setNeedCacheNull(hotKeyCacheConfig.isNeedCacheNull());
        serverProperties.setHotKeyCacheConfig(cacheConfig);
        serverProperties.setHotKeyCacheEnable(properties.isHotKeyCacheEnable());

        CamelliaRedisProxyProperties.BigKeyMonitorConfig bigKeyMonitorConfig = properties.getBigKeyMonitorConfig();
        CamelliaServerProperties.BigKeyMonitorConfig config1 = new CamelliaServerProperties.BigKeyMonitorConfig();
        config1.setBigKeyMonitorCallbackClassName(bigKeyMonitorConfig.getBigKeyMonitorCallbackClassName());
        config1.setBigKeyMonitorCallback(support.getBigKeyMonitorCallback());
        config1.setHashSizeThreshold(bigKeyMonitorConfig.getHashSizeThreshold());
        config1.setListSizeThreshold(bigKeyMonitorConfig.getListSizeThreshold());
        config1.setZsetSizeThreshold(bigKeyMonitorConfig.getZsetSizeThreshold());
        config1.setSetSizeThreshold(bigKeyMonitorConfig.getSetSizeThreshold());
        config1.setStringSizeThreshold(bigKeyMonitorConfig.getStringSizeThreshold());
        serverProperties.setBigKeyMonitorConfig(config1);
        serverProperties.setBigKeyMonitorEnable(properties.isBigKeyMonitorEnable());

        CamelliaRedisProxyProperties.ConverterConfig converterConfig = properties.getConverterConfig();
        serverProperties.setConverterEnable(properties.isConverterEnable());
        CamelliaServerProperties.ConverterConfig config2 = new CamelliaServerProperties.ConverterConfig();

        config2.setKeyConverterClassName(converterConfig.getKeyConverterClassName());
        config2.setKeyConverter(support.getKeyConverter());
        config2.setStringConverterClassName(converterConfig.getStringConverterClassName());
        config2.setStringConverter(support.getStringConverter());
        config2.setHashConverterClassName(converterConfig.getHashConverterClassName());
        config2.setHashConverter(support.getHashConverter());
        config2.setListConverterClassName(converterConfig.getListConverterClassName());
        config2.setListConverter(support.getListConverter());
        config2.setSetConverterClassName(converterConfig.getSetConverterClassName());
        config2.setSetConverter(support.getSetConverter());
        config2.setZsetConverterClassName(converterConfig.getZsetConverterClassName());
        config2.setzSetConverter(support.getzSetConverter());

        serverProperties.setConverterConfig(config2);

        serverProperties.setProxyDynamicConfHookClassName(properties.getProxyDynamicConfHookClassName());
        serverProperties.setProxyDynamicConfHook(support.getProxyDynamicConfHook());
        serverProperties.setMonitorDataMaskPassword(properties.isMonitorDataMaskPassword());
        return serverProperties;
    }

    public static CamelliaTranspondProperties.Type parseType(TranspondProperties properties) {
        CamelliaTranspondProperties.Type type = null;
        switch (properties.getType()) {
            case AUTO:
                type = CamelliaTranspondProperties.Type.AUTO;
                break;
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
        return remoteProperties;
    }

    public static CamelliaTranspondProperties.CustomProperties parse(TranspondProperties.CustomProperties properties, CamelliaRedisProxyConfigurerSupport support) {
        if (properties == null) return null;
        CamelliaTranspondProperties.CustomProperties customProperties = new CamelliaTranspondProperties.CustomProperties();
        customProperties.setBid(properties.getBid());
        customProperties.setBgroup(properties.getBgroup());
        customProperties.setDynamic(properties.isDynamic());
        customProperties.setReloadIntervalMillis(properties.getReloadIntervalMillis());
        ProxyRouteConfUpdater proxyRouteConfUpdater = support.getProxyRouteConfUpdater();
        if (proxyRouteConfUpdater != null) {
            logger.info("ProxyRouteConfUpdater init success, class = {}", proxyRouteConfUpdater.getClass().getName());
        } else {
            String className = properties.getProxyRouteConfUpdaterClassName();
            if (className == null) {
                throw new IllegalArgumentException("proxyRouteConfUpdaterClassName missing");
            }
            try {
                Class<?> clazz;
                try {
                    clazz = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
                }
                proxyRouteConfUpdater = (ProxyRouteConfUpdater) clazz.newInstance();
                logger.info("ProxyRouteConfUpdater init success, class = {}", className);
            } catch (Exception e) {
                logger.error("ProxyRouteConfUpdater init error, class = {}", className, e);
                throw new CamelliaRedisException(e);
            }
        }
        customProperties.setProxyRouteConfUpdater(proxyRouteConfUpdater);
        return customProperties;
    }

    public static CamelliaTranspondProperties.RedisConfProperties parse(TranspondProperties.RedisConfProperties properties, CamelliaRedisProxyConfigurerSupport support) {
        if (properties == null) return null;
        CamelliaTranspondProperties.RedisConfProperties redisConfProperties = new CamelliaTranspondProperties.RedisConfProperties();
        redisConfProperties.setShadingFunc(properties.getShadingFunc());
        redisConfProperties.setShadingFuncInstance(support.getShadingFunc());
        redisConfProperties.setConnectTimeoutMillis(properties.getConnectTimeoutMillis());
        redisConfProperties.setFailBanMillis(properties.getFailBanMillis());
        redisConfProperties.setFailCountThreshold(properties.getFailCountThreshold());
        redisConfProperties.setHeartbeatIntervalSeconds(properties.getHeartbeatIntervalSeconds());
        redisConfProperties.setHeartbeatTimeoutMillis(properties.getHeartbeatTimeoutMillis());
        redisConfProperties.setRedisClusterMaxAttempts(properties.getRedisClusterMaxAttempts());
        redisConfProperties.setDefaultTranspondWorkThread(properties.getDefaultTranspondWorkThread());
        redisConfProperties.setMultiWriteMode(properties.getMultiWriteMode());
        redisConfProperties.setPreheat(properties.isPreheat());
        redisConfProperties.setCloseIdleConnection(properties.isCloseIdleConnection());
        redisConfProperties.setCheckIdleConnectionThresholdSeconds(properties.getCheckIdleConnectionThresholdSeconds());
        redisConfProperties.setCloseIdleConnectionDelaySeconds(properties.getCloseIdleConnectionDelaySeconds());
        return redisConfProperties;
    }
}
