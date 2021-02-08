package com.netease.nim.camellia.redis.proxy.hbase.springboot;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.hbase.springboot.CamelliaHBaseConfiguration;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.hbase.RedisHBaseCommandInvoker;
import com.netease.nim.camellia.redis.proxy.springboot.CamelliaRedisProxyConfiguration;
import com.netease.nim.camellia.redis.springboot.CamelliaRedisConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


/**
 *
 * Created by caojiajun on 2020/4/3.
 */
@Configuration
@AutoConfigureBefore(CamelliaRedisProxyConfiguration.class)
@Import(value = {CamelliaHBaseConfiguration.class, CamelliaRedisConfiguration.class})
public class CamelliaRedisProxyHBaseConfiguration {

    @Bean
    public CommandInvoker commandInvoker(CamelliaRedisTemplate redisTemplate, CamelliaHBaseTemplate hBaseTemplate) {
        return new RedisHBaseCommandInvoker(redisTemplate, hBaseTemplate);
    }
}
