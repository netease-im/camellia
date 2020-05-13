package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.env.ShadingFunc;
import com.netease.nim.camellia.core.util.ShadingFuncUtil;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.jedis.JedisPoolFactory;
import com.netease.nim.camellia.redis.jediscluster.JedisClusterFactory;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPoolConfig;

public class JedisRedisEnvUtil {

    private static final Logger logger = LoggerFactory.getLogger(JedisRedisEnvUtil.class);

    public static CamelliaRedisEnv initEnv(CamelliaTranspondProperties.RedisConfProperties redisConf) {
        CamelliaTranspondProperties.RedisConfProperties.Jedis jedisConf = redisConf.getJedis();
        JedisPoolConfig poolConfig1 = new JedisPoolConfig();
        poolConfig1.setMaxTotal(jedisConf.getMaxActive());
        poolConfig1.setMinIdle(jedisConf.getMinIdle());
        poolConfig1.setMaxIdle(jedisConf.getMaxIdle());
        poolConfig1.setMaxWaitMillis(jedisConf.getMaxWait().toMillis());
        int timeout1 = (int) jedisConf.getTimeout().toMillis();
        JedisPoolFactory jedisPoolFactory = new JedisPoolFactory.DefaultJedisPoolFactory(poolConfig1, timeout1);
        logger.info("JedisConf, minIdle = {}, maxIdle = {}, maxActive = {}, maxWaitMillis = {}, timeoutMillis = {}",
                jedisConf.getMinIdle(), jedisConf.getMaxIdle(), jedisConf.getMaxActive(), jedisConf.getMaxWait().toMillis(), jedisConf.getTimeout().toMillis());

        CamelliaTranspondProperties.RedisConfProperties.JedisCluster jedisClusterConf = redisConf.getJedisCluster();
        JedisPoolConfig poolConfig2 = new JedisPoolConfig();
        poolConfig2.setMaxTotal(jedisClusterConf.getMaxActive());
        poolConfig2.setMinIdle(jedisClusterConf.getMinIdle());
        poolConfig2.setMaxIdle(jedisClusterConf.getMaxIdle());
        poolConfig2.setMaxWaitMillis(jedisClusterConf.getMaxWait().toMillis());
        int timeout2 = (int) jedisClusterConf.getTimeout().toMillis();
        JedisClusterFactory jedisClusterFactory = new JedisClusterFactory.DefaultJedisClusterFactory(poolConfig2, timeout2, timeout2, jedisClusterConf.getMaxAttempts());
        logger.info("JedisClusterConf, minIdle = {}, maxIdle = {}, maxActive = {}, maxWaitMillis = {}, timeoutMillis = {}, maxAttempts = {}",
                jedisClusterConf.getMinIdle(), jedisClusterConf.getMaxIdle(), jedisClusterConf.getMaxActive(), jedisClusterConf.getMaxWait().toMillis(),
                jedisClusterConf.getTimeout().toMillis(), jedisClusterConf.getMaxAttempts());

        ProxyEnv.Builder builder = new ProxyEnv.Builder()
                .multiWriteConcurrentExecPoolSize(redisConf.getMultiWriteConcurrentExecPoolSize())
                .shadingConcurrentExecPoolSize(redisConf.getShadingConcurrentExecPoolSize())
                .multiWriteConcurrentEnable(redisConf.isMultiWriteConcurrentEnable())
                .shadingConcurrentEnable(redisConf.isShadingConcurrentEnable());

        String className = redisConf.getShadingFunc();
        if (className != null) {
            ShadingFunc shadingFunc = ShadingFuncUtil.forName(className);
            builder.shadingFunc(shadingFunc);
            logger.info("ShadingFunc, className = {}", className);
        }

        return new CamelliaRedisEnv.Builder()
                .jedisPoolFactory(jedisPoolFactory)
                .jedisClusterFactory(jedisClusterFactory)
                .proxyEnv(builder.build())
                .pipelinePoolSize(redisConf.getPipelinePoolSize())
                .concurrentExecPoolSize(redisConf.getConcurrentExecPoolSize())
                .build();
    }
}
