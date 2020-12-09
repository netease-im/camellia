package com.netease.nim.camellia.spring.redis.eureka.discovery;

import com.netease.nim.camellia.redis.eureka.base.EurekaProxyDiscovery;
import com.netease.nim.camellia.redis.proxy.RedisProxyJedisPool;
import com.netease.nim.camellia.spring.redis.base.RedisProxyRedisConnectionFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPoolConfig;

/**
 *
 * Created by caojiajun on 2020/12/8
 */
@Configuration
@EnableConfigurationProperties({SpringRedisEurekaDiscoveryProperties.class})
public class SpringRedisEurekaDiscoveryConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SpringRedisEurekaDiscoveryConfiguration.class);

    @Autowired(required = false)
    private DiscoveryClient discoveryClient;

    @Bean
    @ConditionalOnMissingBean(RedisProxyJedisPool.class)
    public RedisProxyJedisPool redisProxyJedisPool(SpringRedisEurekaDiscoveryProperties properties) {
        EurekaProxyDiscovery eurekaProxyDiscovery = new EurekaProxyDiscovery(discoveryClient, properties.getApplicationName(), properties.getRefreshIntervalSeconds());
        boolean sidCarFirst = properties.isSidCarFirst();
        SpringRedisEurekaDiscoveryProperties.RedisConf redisConf = properties.getRedisConf();
        GenericObjectPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(redisConf.getMaxIdle());
        poolConfig.setMinIdle(redisConf.getMinIdle());
        poolConfig.setMaxTotal(redisConf.getMaxActive());
        poolConfig.setMaxWaitMillis(redisConf.getMaxWaitMillis());
        Long bid = properties.getBid();
        String bgroup = properties.getBgroup();
        if (bid == null || bid < 0 || bgroup == null) {
            return new RedisProxyJedisPool(eurekaProxyDiscovery, poolConfig, redisConf.getTimeout(), properties.getPassword(), sidCarFirst);
        } else {
            return new RedisProxyJedisPool(bid, bgroup, eurekaProxyDiscovery, poolConfig, redisConf.getTimeout(), properties.getPassword(), sidCarFirst);
        }
    }

    @Bean
    public RedisProxyRedisConnectionFactory redisProxyRedisConnectionFactory(SpringRedisEurekaDiscoveryProperties properties) {
        RedisProxyRedisConnectionFactory redisProxyRedisConnectionFactory = new RedisProxyRedisConnectionFactory(redisProxyJedisPool(properties));
        logger.info("RedisProxyRedisConnectionFactory init success");
        return redisProxyRedisConnectionFactory;
    }
}
