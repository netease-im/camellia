package com.netease.nim.camellia.redis.proxy;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.JedisPoolConfig;

/**
 *
 * Created by caojiajun on 2021/4/13
 */
public class ProxyJedisPoolConfig {
    private boolean sideCarFirst = false;
    private String regionResolveConf = "";//例子 10.189.0.0/20:region1,10.189.208.0/21:region2
    private String defaultRegion = "default";
    private boolean jedisPoolLazyInit = true;
    private int jedisPoolInitialSize = 16;

    private GenericObjectPoolConfig jedisPoolConfig = new JedisPoolConfig();

    private int timeout = 2000;

    public boolean isSideCarFirst() {
        return sideCarFirst;
    }

    public void setSideCarFirst(boolean sideCarFirst) {
        this.sideCarFirst = sideCarFirst;
    }

    public String getRegionResolveConf() {
        return regionResolveConf;
    }

    public void setRegionResolveConf(String regionResolveConf) {
        this.regionResolveConf = regionResolveConf;
    }

    public String getDefaultRegion() {
        return defaultRegion;
    }

    public void setDefaultRegion(String defaultRegion) {
        this.defaultRegion = defaultRegion;
    }

    public boolean isJedisPoolLazyInit() {
        return jedisPoolLazyInit;
    }

    public void setJedisPoolLazyInit(boolean jedisPoolLazyInit) {
        this.jedisPoolLazyInit = jedisPoolLazyInit;
    }

    public int getJedisPoolInitialSize() {
        return jedisPoolInitialSize;
    }

    public void setJedisPoolInitialSize(int jedisPoolInitialSize) {
        this.jedisPoolInitialSize = jedisPoolInitialSize;
    }

    public GenericObjectPoolConfig getJedisPoolConfig() {
        return jedisPoolConfig;
    }

    public void setJedisPoolConfig(GenericObjectPoolConfig jedisPoolConfig) {
        this.jedisPoolConfig = jedisPoolConfig;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        JSONObject poolConfig = new JSONObject();
        poolConfig.put("minIdle", jedisPoolConfig.getMinIdle());
        poolConfig.put("maxIdle", jedisPoolConfig.getMaxIdle());
        poolConfig.put("maxTotal", jedisPoolConfig.getMaxTotal());
        poolConfig.put("maxWaitMillis", jedisPoolConfig.getMaxWaitMillis());
        jsonObject.put("poolConfig", poolConfig);
        jsonObject.put("sideCarFirst", sideCarFirst);
        jsonObject.put("regionResolveConf", regionResolveConf);
        jsonObject.put("jedisPoolLazyInit", jedisPoolLazyInit);
        jsonObject.put("jedisPoolInitialSize", jedisPoolInitialSize);
        jsonObject.put("timeout", timeout);
        jsonObject.put("defaultRegion", defaultRegion);
        return jsonObject.toJSONString();
    }
}
