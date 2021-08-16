package com.netease.nim.camellia.redis.spring.template.adaptor.springboot;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.springboot.CamelliaRedisConfiguration;
import com.netease.nim.camellia.spring.redis.base.CamelliaRedisTemplateRedisConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 *
 * Created by caojiajun on 2021/8/2
 */
@Configuration
@EnableConfigurationProperties({CamelliaRedisSpringTemplateAdaptorProperties.class})
@Import(value = {CamelliaRedisConfiguration.class})
@ConditionalOnExpression("${camellia-redis-spring-template-adaptor.enable:true}")
public class CamelliaRedisSpringTemplateAdaptorConfiguration {

    @Bean
    public CamelliaRedisTemplateRedisConnectionFactory redisProxyRedisConnectionFactory(CamelliaRedisTemplate template) {
        return new CamelliaRedisTemplateRedisConnectionFactory(template);
    }
}
