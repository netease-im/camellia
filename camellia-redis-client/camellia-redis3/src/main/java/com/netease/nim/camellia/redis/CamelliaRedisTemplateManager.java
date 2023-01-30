package com.netease.nim.camellia.redis;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiUtil;
import com.netease.nim.camellia.redis.jedis.JedisPoolFactory;
import com.netease.nim.camellia.redis.jediscluster.JedisClusterFactory;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2022/2/16
 */
public class CamelliaRedisTemplateManager {

    private static final boolean defaultMonitorEnable = false;
    private static final long defaultCheckMillis = 5000;

    private final CamelliaRedisEnv redisEnv;
    private final CamelliaApi service;
    private final boolean monitorEnable;
    private final long checkIntervalMillis;

    private final ConcurrentHashMap<String, CamelliaRedisTemplate> redisTemplateMap = new ConcurrentHashMap<>();

    public CamelliaRedisTemplateManager(CamelliaRedisEnv redisEnv, CamelliaApi service, boolean monitorEnable, long checkIntervalMillis) {
        this.redisEnv = redisEnv;
        this.service = service;
        this.monitorEnable = monitorEnable;
        this.checkIntervalMillis = checkIntervalMillis;
    }

    public CamelliaRedisTemplateManager(CamelliaApi service, JedisPoolConfig poolConfig, int timeout, boolean monitorEnable, long checkIntervalMillis) {
        this.redisEnv = new CamelliaRedisEnv.Builder()
                .jedisPoolFactory(new JedisPoolFactory.DefaultJedisPoolFactory(poolConfig, timeout))
                .jedisClusterFactory(new JedisClusterFactory.DefaultJedisClusterFactory(poolConfig, timeout, timeout, 5))
                .build();
        this.service = service;
        this.monitorEnable = monitorEnable;
        this.checkIntervalMillis = checkIntervalMillis;
    }

    public CamelliaRedisTemplateManager(CamelliaApi service, int minIdle, int maxIdle, int maxTotal, int maxWaitMillis,
                                        int timeout, boolean monitorEnable, long checkIntervalMillis) {
        this.service = service;
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxWaitMillis(maxWaitMillis);
        this.redisEnv = new CamelliaRedisEnv.Builder()
                .jedisPoolFactory(new JedisPoolFactory.DefaultJedisPoolFactory(poolConfig, timeout))
                .jedisClusterFactory(new JedisClusterFactory.DefaultJedisClusterFactory(poolConfig, timeout, timeout, 5))
                .build();
        this.monitorEnable = monitorEnable;
        this.checkIntervalMillis = checkIntervalMillis;
    }

    public CamelliaRedisTemplateManager(String url, int poolSize, int timeout) {
        this(CamelliaApiUtil.init(url), 0, poolSize, poolSize, timeout, timeout, defaultMonitorEnable, defaultCheckMillis);
    }

    public CamelliaRedisTemplateManager(String url, int poolSize, int timeout, Map<String, String> headerMap) {
        this(CamelliaApiUtil.init(url, headerMap), 0, poolSize, poolSize, timeout, timeout, defaultMonitorEnable, defaultCheckMillis);
    }

    public CamelliaRedisTemplateManager(CamelliaApi service, int poolSize, int timeout) {
        this(service, 0, poolSize, poolSize, timeout, timeout, defaultMonitorEnable, defaultCheckMillis);
    }

    public CamelliaRedisTemplateManager(String url, int minIdle, int maxIdle, int maxTotal, int maxWaitMillis, int timeout) {
        this(CamelliaApiUtil.init(url), minIdle, maxIdle, maxTotal, maxWaitMillis, timeout, defaultMonitorEnable, defaultCheckMillis);
    }

    public CamelliaRedisTemplateManager(String url, int minIdle, int maxIdle, int maxTotal, int maxWaitMillis, int timeout, Map<String, String> headerMap) {
        this(CamelliaApiUtil.init(url, headerMap), minIdle, maxIdle, maxTotal, maxWaitMillis, timeout, defaultMonitorEnable, defaultCheckMillis);
    }

    public CamelliaRedisTemplateManager(CamelliaApi service, int minIdle, int maxIdle, int maxTotal, int maxWaitMillis, int timeout) {
        this(service, minIdle, maxIdle, maxTotal, maxWaitMillis, timeout, defaultMonitorEnable, defaultCheckMillis);
    }

    public CamelliaRedisTemplateManager(String url, int minIdle, int maxIdle, int maxTotal, int maxWaitMillis,
                                        int timeout, boolean monitorEnable, long checkIntervalMillis) {
        this(CamelliaApiUtil.init(url), minIdle, maxIdle, maxTotal, maxWaitMillis, timeout, monitorEnable, checkIntervalMillis);
    }

    public CamelliaRedisTemplateManager(String url, int minIdle, int maxIdle, int maxTotal, int maxWaitMillis,
                                        int timeout, boolean monitorEnable, long checkIntervalMillis, Map<String, String> headerMap) {
        this(CamelliaApiUtil.init(url, headerMap), minIdle, maxIdle, maxTotal, maxWaitMillis, timeout, monitorEnable, checkIntervalMillis);
    }

    public CamelliaRedisTemplateManager(String url, JedisPoolConfig poolConfig, int timeout) {
        this(CamelliaApiUtil.init(url), poolConfig, timeout, defaultMonitorEnable, defaultCheckMillis);
    }

    public CamelliaRedisTemplateManager(String url, JedisPoolConfig poolConfig, int timeout, Map<String, String> headerMap) {
        this(CamelliaApiUtil.init(url, headerMap), poolConfig, timeout, defaultMonitorEnable, defaultCheckMillis);
    }

    public CamelliaRedisTemplateManager(CamelliaApi service, JedisPoolConfig poolConfig, int timeout) {
        this(service, poolConfig, timeout, defaultMonitorEnable, defaultCheckMillis);
    }

    public CamelliaRedisTemplateManager(String url, JedisPoolConfig poolConfig, int timeout, boolean monitorEnable,
                                        long checkIntervalMillis) {
        this(CamelliaApiUtil.init(url), poolConfig, timeout, monitorEnable, checkIntervalMillis);
    }

    public CamelliaRedisTemplateManager(String url, JedisPoolConfig poolConfig, int timeout, boolean monitorEnable,
                                        long checkIntervalMillis, Map<String, String> headerMap) {
        this(CamelliaApiUtil.init(url, headerMap), poolConfig, timeout, monitorEnable, checkIntervalMillis);
    }

    public CamelliaRedisTemplateManager(CamelliaRedisEnv redisEnv, String url, boolean monitorEnable,
                                        long checkIntervalMillis) {
        this(redisEnv, CamelliaApiUtil.init(url), monitorEnable, checkIntervalMillis);
    }

    public CamelliaRedisTemplateManager(CamelliaRedisEnv redisEnv, String url, boolean monitorEnable,
                                        long checkIntervalMillis, Map<String, String> headerMap) {
        this(redisEnv, CamelliaApiUtil.init(url, headerMap), monitorEnable, checkIntervalMillis);
    }

    public CamelliaRedisTemplateManager(CamelliaRedisEnv redisEnv, CamelliaApi service) {
        this(redisEnv, service, defaultMonitorEnable, defaultCheckMillis);
    }

    public CamelliaRedisTemplateManager(CamelliaRedisEnv redisEnv, String url) {
        this(redisEnv, CamelliaApiUtil.init(url), defaultMonitorEnable, defaultCheckMillis);
    }

    public CamelliaRedisTemplateManager(CamelliaRedisEnv redisEnv, String url, Map<String, String> headerMap) {
        this(redisEnv, CamelliaApiUtil.init(url, headerMap), defaultMonitorEnable, defaultCheckMillis);
    }

    public CamelliaRedisTemplateManager(CamelliaApi service) {
        this(CamelliaRedisEnv.defaultRedisEnv(), service, defaultMonitorEnable, defaultCheckMillis);
    }

    public CamelliaRedisTemplateManager(String url) {
        this(CamelliaRedisEnv.defaultRedisEnv(), CamelliaApiUtil.init(url), defaultMonitorEnable, defaultCheckMillis);
    }

    public CamelliaRedisTemplateManager(String url, Map<String, String> headerMap) {
        this(CamelliaRedisEnv.defaultRedisEnv(), CamelliaApiUtil.init(url, headerMap), defaultMonitorEnable, defaultCheckMillis);
    }

    public CamelliaRedisTemplate getRedisTemplate(long bid, String bgroup) {
        String key = bid + "|" + bgroup;
        CamelliaRedisTemplate template = redisTemplateMap.get(key);
        if (template == null) {
            synchronized (redisTemplateMap) {
                template = redisTemplateMap.get(key);
                if (template == null) {
                    template = new CamelliaRedisTemplate(redisEnv, service, bid, bgroup, monitorEnable, checkIntervalMillis);
                    redisTemplateMap.put(key, template);
                }
            }
        }
        return template;
    }

    public void reloadResourceTable() {
        for (CamelliaRedisTemplate template : redisTemplateMap.values()) {
            template.reloadResourceTable();
        }
    }

}
