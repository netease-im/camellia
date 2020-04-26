package com.netease.nim.camellia.redis.eureka.springboot;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.CamelliaRedisProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by caojiajun on 2020/4/23.
 */
@Configuration
@AutoConfigureBefore(CamelliaRedisTemplate.class)
@EnableConfigurationProperties({CamelliaRedisEurekaProperties.class})
public class CamelliaRedisEurekaConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisEurekaConfiguration.class);

    @Autowired(required = false)
    private DiscoveryClient discoveryClient;

    @Bean
    public CamelliaRedisProxyFactory redisProxyFactory(CamelliaRedisEurekaProperties properties) {
        logger.info("CamelliaRedisProxyEurekaFactory init success");
        return new CamelliaRedisProxyEurekaFactory(discoveryClient, properties);
    }
}
