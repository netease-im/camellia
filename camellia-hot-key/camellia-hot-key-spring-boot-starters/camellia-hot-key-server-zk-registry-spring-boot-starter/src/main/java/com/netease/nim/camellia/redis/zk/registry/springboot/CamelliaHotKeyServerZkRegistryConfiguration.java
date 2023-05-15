package com.netease.nim.camellia.redis.zk.registry.springboot;

import com.netease.nim.camellia.hot.key.server.springboot.CamelliaHotKeyServerBoot;
import com.netease.nim.camellia.hot.key.server.springboot.CamelliaHotKeyServerConfiguration;
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
@EnableConfigurationProperties({CamelliaHotKeyServerZkRegistryProperties.class})
@Import(value = {CamelliaHotKeyServerConfiguration.class})
public class CamelliaHotKeyServerZkRegistryConfiguration {

    @Autowired
    private CamelliaHotKeyServerBoot hotKeyServerBoot;

    @Bean
    public CamelliaHotKeyServerZkRegisterBoot camelliaRedisProxyZkRegisterBoot(CamelliaHotKeyServerZkRegistryProperties properties) {
        return new CamelliaHotKeyServerZkRegisterBoot(properties, hotKeyServerBoot.getApplicationName(), hotKeyServerBoot.getPort());
    }
}
