package com.netease.nim.camellia.redis.proxy.hbase.springboot;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.hbase.springboot.CamelliaHBaseConfiguration;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.hbase.UpstreamRedisHBaseMixClientTemplateChooser;
import com.netease.nim.camellia.redis.proxy.springboot.CamelliaRedisProxyConfiguration;
import com.netease.nim.camellia.redis.proxy.springboot.CamelliaRedisProxyConfigurerSupport;
import com.netease.nim.camellia.redis.proxy.springboot.conf.CamelliaRedisProxyProperties;
import com.netease.nim.camellia.redis.springboot.CamelliaRedisConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


/**
 *
 * Created by caojiajun on 2020/4/3.
 */
@Configuration
@AutoConfigureBefore(CamelliaRedisProxyConfiguration.class)
@EnableConfigurationProperties({CamelliaRedisProxyProperties.class})
@Import(value = {CamelliaHBaseConfiguration.class, CamelliaRedisConfiguration.class, CamelliaRedisProxyConfigurerSupport.class})
public class CamelliaRedisProxyHBaseConfiguration {

    @Bean
    public UpstreamRedisHBaseMixClientTemplateChooser commandInvoker(CamelliaRedisTemplate redisTemplate,
                                                                     CamelliaHBaseTemplate hBaseTemplate) {
        return new UpstreamRedisHBaseMixClientTemplateChooser(redisTemplate, hBaseTemplate);
    }
}
