package com.netease.nim.camellia.spring.redis.zk.discovery;

import com.netease.nim.camellia.redis.proxy.discovery.zk.ZkConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * Created by caojiajun on 2020/12/3
 */
@ConfigurationProperties(prefix = "camellia-spring-redis-zk-discovery")
public class SpringRedisZkDiscoveryProperties {

    private boolean enable = true;
    private String applicationName;
    private Long bid;
    private String bgroup;
    private String password;
    private ZkConf zkConf = new ZkConf();
    private RedisConf redisConf = new RedisConf();

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public Long getBid() {
        return bid;
    }

    public void setBid(Long bid) {
        this.bid = bid;
    }

    public String getBgroup() {
        return bgroup;
    }

    public void setBgroup(String bgroup) {
        this.bgroup = bgroup;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ZkConf getZkConf() {
        return zkConf;
    }

    public void setZkConf(ZkConf zkConf) {
        this.zkConf = zkConf;
    }

    public RedisConf getRedisConf() {
        return redisConf;
    }

    public void setRedisConf(RedisConf redisConf) {
        this.redisConf = redisConf;
    }

    public static class ZkConf {

        private String basePath = ZkConstants.basePath;

        /**
         * zk的地址，例子：127.0.0.1:2181,127.0.0.2.2181
         */
        private String zkUrl;

        /**
         * zk的一些连接配置
         */
        private int sessionTimeoutMs = ZkConstants.sessionTimeoutMs;
        private int connectionTimeoutMs = ZkConstants.connectionTimeoutMs;
        private int baseSleepTimeMs = ZkConstants.baseSleepTimeMs;
        private int maxRetries = ZkConstants.maxRetries;
        private int reloadIntervalSeconds = ZkConstants.reloadIntervalSeconds;
        private boolean sideCarFirst = ZkConstants.sideCarFirst;
        private String regionResolveConf = "";//例子 10.189.0.0/20:region1,10.189.208.0/21:region2
        private String defaultRegion = "default";
        private boolean jedisPoolLazyInit = true;
        private int jedisPoolInitialSize = 16;

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }

        public String getZkUrl() {
            return zkUrl;
        }

        public void setZkUrl(String zkUrl) {
            this.zkUrl = zkUrl;
        }

        public int getSessionTimeoutMs() {
            return sessionTimeoutMs;
        }

        public void setSessionTimeoutMs(int sessionTimeoutMs) {
            this.sessionTimeoutMs = sessionTimeoutMs;
        }

        public int getConnectionTimeoutMs() {
            return connectionTimeoutMs;
        }

        public void setConnectionTimeoutMs(int connectionTimeoutMs) {
            this.connectionTimeoutMs = connectionTimeoutMs;
        }

        public int getBaseSleepTimeMs() {
            return baseSleepTimeMs;
        }

        public void setBaseSleepTimeMs(int baseSleepTimeMs) {
            this.baseSleepTimeMs = baseSleepTimeMs;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public int getReloadIntervalSeconds() {
            return reloadIntervalSeconds;
        }

        public void setReloadIntervalSeconds(int reloadIntervalSeconds) {
            this.reloadIntervalSeconds = reloadIntervalSeconds;
        }

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
    }

    public static class RedisConf {
        private int maxIdle = 8;
        private int minIdle = 0;
        private int maxActive = 8;
        private int maxWaitMillis = 2000;
        private int timeout = 2000;

        public int getMaxIdle() {
            return maxIdle;
        }

        public void setMaxIdle(int maxIdle) {
            this.maxIdle = maxIdle;
        }

        public int getMinIdle() {
            return minIdle;
        }

        public void setMinIdle(int minIdle) {
            this.minIdle = minIdle;
        }

        public int getMaxActive() {
            return maxActive;
        }

        public void setMaxActive(int maxActive) {
            this.maxActive = maxActive;
        }

        public int getMaxWaitMillis() {
            return maxWaitMillis;
        }

        public void setMaxWaitMillis(int maxWaitMillis) {
            this.maxWaitMillis = maxWaitMillis;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
                this.timeout = timeout;
            }
    }
}
