package com.netease.nim.camellia.redis.zk.discovery.springboot;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.CamelliaRedisProxyFactory;
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
        return new CamelliaRedisProxyZkFactory(properties);
    }
}
