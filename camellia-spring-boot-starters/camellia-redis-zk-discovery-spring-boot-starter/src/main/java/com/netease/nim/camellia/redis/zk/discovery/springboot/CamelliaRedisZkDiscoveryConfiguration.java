package com.netease.nim.camellia.redis.zk.discovery.springboot;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.CamelliaRedisProxyFactory;
import com.netease.nim.camellia.redis.proxy.ProxyJedisPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * Created by caojiajun on 2020/8/12
 */
@Configuration
@AutoConfigureBefore(CamelliaRedisTemplate.class)
@EnableConfigurationProperties({CamelliaRedisZkDiscoveryProperties.class})
public class CamelliaRedisZkDiscoveryConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisZkDiscoveryConfiguration.class);

    @Bean
    public CamelliaRedisProxyFactory redisProxyFactory(CamelliaRedisZkDiscoveryProperties properties) {
        logger.info("CamelliaRedisZkDiscoveryProperties init success");
        return new CamelliaRedisProxyZkFactory(proxyJedisPoolConfig(properties), proxyDiscoveryFactory(properties));
    }

    @Bean
    public ZkProxyDiscoveryFactory proxyDiscoveryFactory(CamelliaRedisZkDiscoveryProperties properties) {
        logger.info("EurekaProxyDiscoveryFactory init success");
        return new ZkProxyDiscoveryFactory(properties);
    }

    @Bean
    public ProxyJedisPoolConfig proxyJedisPoolConfig(CamelliaRedisZkDiscoveryProperties properties) {
        ProxyJedisPoolConfig proxyJedisPoolConfig = new ProxyJedisPoolConfig();
        proxyJedisPoolConfig.setSideCarFirst(properties.isSideCarFirst());
        proxyJedisPoolConfig.setDefaultRegion(properties.getDefaultRegion());
        proxyJedisPoolConfig.setRegionResolveConf(properties.getRegionResolveConf());
        proxyJedisPoolConfig.setJedisPoolInitialSize(properties.getJedisPoolInitialSize());
        proxyJedisPoolConfig.setJedisPoolLazyInit(properties.isJedisPoolLazyInit());
        logger.info("ProxyJedisPoolConfig init success");
        return proxyJedisPoolConfig;
    }
}
