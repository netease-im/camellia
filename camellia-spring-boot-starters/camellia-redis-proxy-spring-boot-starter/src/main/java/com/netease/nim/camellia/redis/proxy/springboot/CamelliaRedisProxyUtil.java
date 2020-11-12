package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.*;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.conf.QueueType;
import com.netease.nim.camellia.redis.proxy.springboot.conf.CamelliaRedisProxyProperties;
import com.netease.nim.camellia.redis.proxy.springboot.conf.NettyProperties;
import com.netease.nim.camellia.redis.proxy.springboot.conf.TranspondProperties;
import com.netease.nim.camellia.redis.resource.RedisResourceUtil;

import java.net.URL;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2020/4/3.
 */
public class CamelliaRedisProxyUtil {

    public static CamelliaServerProperties parse(CamelliaRedisProxyProperties properties, int port) {
        CamelliaServerProperties serverProperties = new CamelliaServerProperties();
        serverProperties.setPort(port);
        serverProperties.setPassword(properties.getPassword());
        serverProperties.setMonitorEnable(properties.isMonitorEnable());
        serverProperties.setMonitorIntervalSeconds(properties.getMonitorIntervalSeconds());
        serverProperties.setMonitorCallbackClassName(properties.getMonitorCallbackClassName());
        serverProperties.setCommandSpendTimeMonitorEnable(properties.isCommandSpendTimeMonitorEnable());
        serverProperties.setSlowCommandThresholdMillisTime(properties.getSlowCommandThresholdMillisTime());
        serverProperties.setCommandInterceptorClassName(properties.getCommandInterceptorClassName());
        serverProperties.setSlowCommandCallbackClassName(properties.getSlowCommandCallbackClassName());
        NettyProperties netty = properties.getNetty();
        serverProperties.setBossThread(netty.getBossThread());
        if (netty.getWorkThread() > 0) {
            serverProperties.setWorkThread(netty.getWorkThread());
        } else {
            serverProperties.setWorkThread(Constants.Server.workThread);
        }
        serverProperties.setCommandDecodeMaxBatchSize(netty.getCommandDecodeMaxBatchSize());
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
        cacheConfig.setCacheMaxCapacity(hotKeyCacheConfig.getCacheMaxCapacity());
        cacheConfig.setHotKeyCacheStatsCallbackClassName(hotKeyCacheConfig.getHotKeyCacheStatsCallbackClassName());
        cacheConfig.setHotKeyCacheStatsCallbackIntervalSeconds(hotKeyCacheConfig.getHotKeyCacheStatsCallbackIntervalSeconds());
        cacheConfig.setNeedCacheNull(hotKeyCacheConfig.isNeedCacheNull());
        serverProperties.setHotKeyCacheConfig(cacheConfig);
        serverProperties.setHotKeyCacheEnable(properties.isHotKeyCacheEnable());

        CamelliaRedisProxyProperties.BigKeyMonitorConfig bigKeyMonitorConfig = properties.getBigKeyMonitorConfig();
        CamelliaServerProperties.BigKeyMonitorConfig config1 = new CamelliaServerProperties.BigKeyMonitorConfig();
        config1.setBigKeyMonitorCallbackClassName(bigKeyMonitorConfig.getBigKeyMonitorCallbackClassName());
        config1.setHashSizeThreshold(bigKeyMonitorConfig.getHashSizeThreshold());
        config1.setListSizeThreshold(bigKeyMonitorConfig.getListSizeThreshold());
        config1.setZsetSizeThreshold(bigKeyMonitorConfig.getZsetSizeThreshold());
        config1.setSetSizeThreshold(bigKeyMonitorConfig.getSetSizeThreshold());
        config1.setStringSizeThreshold(bigKeyMonitorConfig.getStringSizeThreshold());
        serverProperties.setBigKeyMonitorConfig(config1);
        serverProperties.setBigKeyMonitorEnable(properties.isBigKeyMonitorEnable());
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
        }
        return type;
    }

    public static CamelliaTranspondProperties.LocalProperties parse(TranspondProperties.LocalProperties properties) {
        if (properties == null) return null;
        CamelliaTranspondProperties.LocalProperties localProperties = new CamelliaTranspondProperties.LocalProperties();
        ResourceTable resourceTable;

        if (properties.getType() == TranspondProperties.LocalProperties.Type.SIMPLE) {
            String resource = properties.getResource();
            if (resource == null) return localProperties;
            resourceTable = ResourceTableUtil.simpleTable(new Resource(resource));
        } else if (properties.getType() == TranspondProperties.LocalProperties.Type.COMPLEX) {
            String jsonFile = properties.getJsonFile();
            if (jsonFile == null) {
                throw new IllegalArgumentException("missing jsonFile");
            }
            URL resource = Thread.currentThread().getContextClassLoader().getResource(jsonFile);
            if (resource == null) {
                throw new IllegalArgumentException("not found " + jsonFile);
            }
            String path = resource.getPath();
            String fileContent = FileUtil.readFile(path);
            if (fileContent == null) {
                throw new IllegalArgumentException(jsonFile + " read fail");
            }
            resourceTable = ReadableResourceTableUtil.parseTable(fileContent);
        } else {
            throw new IllegalArgumentException("not support type");
        }
        boolean check = CheckUtil.checkResourceTable(resourceTable);
        if (!check) {
            throw new IllegalArgumentException("resourceTable check fail");
        }
        Set<Resource> allResources = ResourceUtil.getAllResources(resourceTable);
        for (Resource resource : allResources) {
            RedisResourceUtil.parseResourceByUrl(resource);
        }
        localProperties.setResourceTable(resourceTable);
        return localProperties;
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

    public static CamelliaTranspondProperties.RedisConfProperties parse(TranspondProperties.RedisConfProperties properties) {
        if (properties == null) return null;
        CamelliaTranspondProperties.RedisConfProperties redisConfProperties = new CamelliaTranspondProperties.RedisConfProperties();
        redisConfProperties.setShadingFunc(properties.getShadingFunc());
        redisConfProperties.setCommandPipelineFlushThreshold(properties.getCommandPipelineFlushThreshold());
        redisConfProperties.setConnectTimeoutMillis(properties.getConnectTimeoutMillis());
        redisConfProperties.setFailBanMillis(properties.getFailBanMillis());
        redisConfProperties.setFailCountThreshold(properties.getFailCountThreshold());
        redisConfProperties.setHeartbeatIntervalSeconds(properties.getHeartbeatIntervalSeconds());
        redisConfProperties.setHeartbeatTimeoutMillis(properties.getHeartbeatTimeoutMillis());
        redisConfProperties.setRedisClusterMaxAttempts(properties.getRedisClusterMaxAttempts());
        QueueType queueType = properties.getQueueType();
        if (queueType != null) {
            redisConfProperties.setQueueType(queueType);
        }
        TranspondProperties.RedisConfProperties.DisruptorConf disruptorConf = properties.getDisruptorConf();
        if (disruptorConf != null) {
            CamelliaTranspondProperties.RedisConfProperties.DisruptorConf disruptorConf1 = new CamelliaTranspondProperties.RedisConfProperties.DisruptorConf();
            disruptorConf1.setWaitStrategyClassName(disruptorConf.getWaitStrategyClassName());
            redisConfProperties.setDisruptorConf(disruptorConf1);
        }
        redisConfProperties.setDefaultTranspondWorkThread(properties.getDefaultTranspondWorkThread());
        redisConfProperties.setMultiWriteMode(properties.getMultiWriteMode());
        return redisConfProperties;
    }
}
