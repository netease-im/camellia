package com.netease.nim.camellia.redis.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;


/**
 *
 * Created by caojiajun on 2020/3/31.
 */
@ConfigurationProperties(prefix = "camellia-redis")
public class CamelliaRedisProperties {

    private Type type = Type.LOCAL;
    private Local local = new Local();
    private Remote remote;
    private Custom custom;
    private RedisConf redisConf = new RedisConf();

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public Remote getRemote() {
        return remote;
    }

    public void setRemote(Remote remote) {
        this.remote = remote;
    }

    public Custom getCustom() {
        return custom;
    }

    public void setCustom(Custom custom) {
        this.custom = custom;
    }

    public RedisConf getRedisConf() {
        return redisConf;
    }

    public void setRedisConf(RedisConf redisConf) {
        this.redisConf = redisConf;
    }

    public static class Local {
        private Type type = Type.SIMPLE;
        private String resource;
        private String jsonFile;
        private boolean dynamic;
        private long checkIntervalMillis = 5000;

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public String getJsonFile() {
            return jsonFile;
        }

        public void setJsonFile(String jsonFile) {
            this.jsonFile = jsonFile;
        }

        public boolean isDynamic() {
            return dynamic;
        }

        public void setDynamic(boolean dynamic) {
            this.dynamic = dynamic;
        }

        public long getCheckIntervalMillis() {
            return checkIntervalMillis;
        }

        public void setCheckIntervalMillis(long checkIntervalMillis) {
            this.checkIntervalMillis = checkIntervalMillis;
        }

        public static enum Type {
            SIMPLE,
            COMPLEX,
            ;
        }
    }

    public static class Remote {
        /**
         * dashboard地址
         */
        private String url;

        /**
         * bid
         */
        private long bid;

        /**
         * bgroup
         */
        private String bgroup;

        /**
         * 是否开启监控，若开启，则会把数据上报给dashboard
         */
        private boolean monitor = true;

        /**
         * dashboard检查变更的时间间隔
         */
        private long checkIntervalMillis = 5000;

        /**
         * dashboard的请求connect超时
         */
        private int connectTimeoutMillis = 10000;

        /**
         * dashboard的请求read超时
         */
        private int readTimeoutMillis = 60000;
        /**
         * dashboard's headers
         */
        private Map<String, String> headerMap = new HashMap<>();

        public Map<String, String> getHeaderMap() {
            return headerMap;
        }

        public void setHeaderMap(Map<String, String> headerMap) {
            this.headerMap = headerMap;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public long getBid() {
            return bid;
        }

        public void setBid(long bid) {
            this.bid = bid;
        }

        public String getBgroup() {
            return bgroup;
        }

        public void setBgroup(String bgroup) {
            this.bgroup = bgroup;
        }

        public boolean isMonitor() {
            return monitor;
        }

        public void setMonitor(boolean monitor) {
            this.monitor = monitor;
        }

        public long getCheckIntervalMillis() {
            return checkIntervalMillis;
        }

        public void setCheckIntervalMillis(long checkIntervalMillis) {
            this.checkIntervalMillis = checkIntervalMillis;
        }

        public int getConnectTimeoutMillis() {
            return connectTimeoutMillis;
        }

        public void setConnectTimeoutMillis(int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
        }

        public int getReadTimeoutMillis() {
            return readTimeoutMillis;
        }

        public void setReadTimeoutMillis(int readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
        }
    }

    public static class Custom {
        private String resourceTableUpdaterClassName;

        public String getResourceTableUpdaterClassName() {
            return resourceTableUpdaterClassName;
        }

        public void setResourceTableUpdaterClassName(String resourceTableUpdaterClassName) {
            this.resourceTableUpdaterClassName = resourceTableUpdaterClassName;
        }
    }

    public static class RedisConf {

        private Jedis jedis = new Jedis();
        private JedisCluster jedisCluster = new JedisCluster();

        public Jedis getJedis() {
            return jedis;
        }

        public void setJedis(Jedis jedis) {
            this.jedis = jedis;
        }

        public JedisCluster getJedisCluster() {
            return jedisCluster;
        }

        public void setJedisCluster(JedisCluster jedisCluster) {
            this.jedisCluster = jedisCluster;
        }

        public static class Jedis {
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

        public static class JedisCluster {
            private int maxIdle = 8;
            private int minIdle = 0;
            private int maxActive = 8;
            private int maxWaitMillis = 2000;
            private int maxAttempts = 5;
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

            public int getMaxAttempts() {
                return maxAttempts;
            }

            public void setMaxAttempts(int maxAttempts) {
                this.maxAttempts = maxAttempts;
            }

            public int getTimeout() {
                return timeout;
            }

            public void setTimeout(int timeout) {
                this.timeout = timeout;
            }
        }
    }

    public static enum Type {
        LOCAL,
        REMOTE,
        CUSTOM,
        ;
    }
}
