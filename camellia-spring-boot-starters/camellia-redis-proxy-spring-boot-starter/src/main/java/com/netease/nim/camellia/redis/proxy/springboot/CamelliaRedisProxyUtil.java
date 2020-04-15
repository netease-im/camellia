package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.*;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.springboot.conf.TranspondProperties;
import com.netease.nim.camellia.redis.resource.RedisResourceUtil;

import java.net.URL;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2020/4/3.
 */
public class CamelliaRedisProxyUtil {

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
        TranspondProperties.RedisConfProperties.Jedis jedis = properties.getJedis();
        if (jedis != null) {
            redisConfProperties.setJedis(new CamelliaTranspondProperties.RedisConfProperties.Jedis(jedis.getMaxIdle(), jedis.getMinIdle(),
                    jedis.getMaxActive(), jedis.getMaxWait(), jedis.getTimeout()));
        }

        TranspondProperties.RedisConfProperties.JedisCluster jedisCluster = properties.getJedisCluster();
        if (jedisCluster != null) {
            redisConfProperties.setJedisCluster(new CamelliaTranspondProperties.RedisConfProperties.JedisCluster(jedisCluster.getMaxIdle(), jedisCluster.getMinIdle(),
                    jedisCluster.getMaxActive(), jedisCluster.getMaxWait(), jedisCluster.getTimeout(), jedisCluster.getMaxAttempts()));
        }

        TranspondProperties.RedisConfProperties.Netty netty = properties.getNetty();
        if (netty != null) {
            redisConfProperties.setNetty(new CamelliaTranspondProperties.RedisConfProperties.Netty(netty.getRedisClusterMaxAttempts(),
                    netty.getHeartbeatIntervalSeconds(), netty.getHeartbeatTimeoutMillis(), netty.getCommandPipelineFlushThreshold(),
                    netty.getConnectTimeoutMillis(), netty.getFailCountThreshold(), netty.getFailBanMillis()));
        }

        redisConfProperties.setShadingFunc(properties.getShadingFunc());
        redisConfProperties.setPipelinePoolSize(properties.getPipelinePoolSize());
        redisConfProperties.setConcurrentExecPoolSize(properties.getConcurrentExecPoolSize());
        redisConfProperties.setShadingConcurrentExecPoolSize(properties.getShadingConcurrentExecPoolSize());
        redisConfProperties.setMultiWriteConcurrentExecPoolSize(properties.getMultiWriteConcurrentExecPoolSize());
        redisConfProperties.setShadingConcurrentEnable(properties.isShadingConcurrentEnable());
        redisConfProperties.setMultiWriteConcurrentEnable(properties.isMultiWriteConcurrentEnable());
        return redisConfProperties;
    }
}
