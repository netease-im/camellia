package com.netease.nim.camellia.redis.proxy.hbase.springboot;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.hbase.springboot.CamelliaHBaseConfiguration;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.hbase.RedisHBaseCommandInvoker;
import com.netease.nim.camellia.redis.proxy.hbase.discovery.DiscoverHolder;
import com.netease.nim.camellia.redis.proxy.hbase.discovery.IRedisProxyHBaseRegister;
import com.netease.nim.camellia.redis.proxy.hbase.discovery.InstanceUrlGenerator;
import com.netease.nim.camellia.redis.proxy.springboot.CamelliaRedisProxyConfiguration;
import com.netease.nim.camellia.redis.springboot.CamelliaRedisConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.Inet4Address;
import java.net.UnknownHostException;

/**
 *
 * Created by caojiajun on 2020/4/3.
 */
@Configuration
@AutoConfigureBefore(CamelliaRedisProxyConfiguration.class)
@Import(value = {CamelliaHBaseConfiguration.class, CamelliaRedisConfiguration.class})
public class CamelliaRedisProxyHBaseConfiguration {

    @Value("${spring.application.name:camellia-redis-proxy-hbase}")
    private String applicationName;

    @Value("${server.port:6379}")
    private int port;

    @Bean
    @ConditionalOnMissingBean(InstanceUrlGenerator.class)
    public InstanceUrlGenerator instanceUrlGenerator() {
        return () -> {
            try {
                return Inet4Address.getLocalHost().getHostName() + ":" + port;
            } catch (UnknownHostException e) {
                return null;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(IRedisProxyHBaseRegister.class)
    public IRedisProxyHBaseRegister redisProxyHBaseRegister(CamelliaRedisTemplate redisTemplate) {
        return new IRedisProxyHBaseRegister.Default(redisTemplate, "camellia_redis_hbase_register_" + applicationName, instanceUrlGenerator().instanceUrl());
    }

    @Bean
    public CommandInvoker commandInvoker(CamelliaRedisTemplate redisTemplate, CamelliaHBaseTemplate hBaseTemplate) {
        DiscoverHolder.instanceUrlGenerator = instanceUrlGenerator();
        DiscoverHolder.redisProxyHBaseRegister = redisProxyHBaseRegister(redisTemplate);
        try {
            return new RedisHBaseCommandInvoker(redisTemplate, hBaseTemplate);
        } finally {
            DiscoverHolder.registerIfNeed();
        }
    }
}
