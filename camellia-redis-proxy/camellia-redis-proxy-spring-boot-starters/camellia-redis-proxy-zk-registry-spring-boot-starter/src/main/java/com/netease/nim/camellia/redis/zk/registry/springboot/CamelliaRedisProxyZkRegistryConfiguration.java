package com.netease.nim.camellia.redis.zk.registry.springboot;

import com.netease.nim.camellia.redis.proxy.springboot.CamelliaRedisProxyBoot;
import com.netease.nim.camellia.redis.proxy.springboot.CamelliaRedisProxyConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


/**
 *
 * Created by caojiajun on 2020/8/12
 */
@Configuration
@EnableConfigurationProperties({CamelliaRedisProxyZkRegistryProperties.class})
@Import(value = {CamelliaRedisProxyConfiguration.class})
public class CamelliaRedisProxyZkRegistryConfiguration {

    @Autowired
    private CamelliaRedisProxyBoot redisProxyBoot;

    @Bean
    public CamelliaRedisProxyZkRegisterBoot camelliaRedisProxyZkRegisterBoot(CamelliaRedisProxyZkRegistryProperties properties) {
        return new CamelliaRedisProxyZkRegisterBoot(properties, redisProxyBoot.getApplicationName(), redisProxyBoot.getPort());
    }
}
