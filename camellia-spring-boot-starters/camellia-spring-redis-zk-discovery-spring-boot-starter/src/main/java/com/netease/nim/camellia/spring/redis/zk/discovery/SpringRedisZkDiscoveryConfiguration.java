package com.netease.nim.camellia.spring.redis.zk.discovery;

import com.netease.nim.camellia.redis.proxy.RedisProxyJedisPool;
import com.netease.nim.camellia.redis.zk.discovery.ZkClientFactory;
import com.netease.nim.camellia.redis.zk.discovery.ZkProxyDiscovery;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * Created by caojiajun on 2020/12/2
 */
@Configuration
@EnableConfigurationProperties({SpringRedisZkDiscoveryProperties.class})
public class SpringRedisZkDiscoveryConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(SpringRedisZkDiscoveryConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(ZkClientFactory.class)
    public ZkClientFactory zkClientFactory(SpringRedisZkDiscoveryProperties properties) {
        SpringRedisZkDiscoveryProperties.ZkConf zk = properties.getZkConf();
        return new ZkClientFactory(zk.getSessionTimeoutMs(),
                zk.getConnectionTimeoutMs(), zk.getBaseSleepTimeMs(), zk.getMaxRetries());
    }

    @Bean
    @ConditionalOnMissingBean(RedisProxyJedisPool.class)
    public RedisProxyJedisPool redisProxyJedisPool(SpringRedisZkDiscoveryProperties properties) {
        SpringRedisZkDiscoveryProperties.ZkConf zkConf = properties.getZkConf();
        ZkProxyDiscovery zkProxyDiscovery = new ZkProxyDiscovery(zkClientFactory(properties),
                zkConf.getZkUrl(), zkConf.getBasePath(), properties.getApplicationName(), zkConf.getReloadIntervalSeconds());

        boolean sidCarFirst = zkConf.isSidCarFirst();
        SpringRedisZkDiscoveryProperties.RedisConf redisConf = properties.getRedisConf();
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxIdle(redisConf.getMaxIdle());
        poolConfig.setMinIdle(redisConf.getMinIdle());
        poolConfig.setMaxTotal(redisConf.getMaxActive());
        poolConfig.setMaxWaitMillis(redisConf.getMaxWaitMillis());
        Long bid = properties.getBid();
        String bgroup = properties.getBgroup();
        if (bid == null || bid < 0 || bgroup == null) {
            return new RedisProxyJedisPool(zkProxyDiscovery, poolConfig, redisConf.getTimeout(), properties.getPassword(), sidCarFirst);
        } else {
            return new RedisProxyJedisPool(bid, bgroup, zkProxyDiscovery, poolConfig, redisConf.getTimeout(), properties.getPassword(), sidCarFirst);
        }
    }

    @Bean
    public RedisProxyRedisConnectionFactory redisProxyRedisConnectionFactory(SpringRedisZkDiscoveryProperties properties) {
        RedisProxyRedisConnectionFactory redisProxyRedisConnectionFactory = new RedisProxyRedisConnectionFactory(redisProxyJedisPool(properties));
        logger.info("RedisProxyRedisConnectionFactory init success");
        return redisProxyRedisConnectionFactory;
    }
}
