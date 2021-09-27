package com.netease.nim.camellia.id.gen.springboot.strict;

import com.netease.nim.camellia.id.gen.common.IDLoader;
import com.netease.nim.camellia.id.gen.strict.CamelliaStrictIdGen;
import com.netease.nim.camellia.id.gen.strict.CamelliaStrictIdGenConfig;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Created by caojiajun on 2021/9/27
 */
@Configuration
@EnableConfigurationProperties({CamelliaIdGenStrictProperties.class})
public class CamelliaIdGenStrictConfiguration {

    @Autowired(required = false)
    private IDLoader idLoader;

    @Autowired(required = false)
    private CamelliaRedisTemplate camelliaRedisTemplate;

    @Bean
    public CamelliaStrictIdGen camelliaSegmentIdGen(CamelliaIdGenStrictProperties properties) {
        CamelliaStrictIdGenConfig config = new CamelliaStrictIdGenConfig();
        config.setIdLoader(idLoader);
        config.setTemplate(camelliaRedisTemplate);
        config.setCacheKeyPrefix(config.getCacheKeyPrefix());
        config.setMaxStep(properties.getMaxStep());
        config.setDefaultStep(properties.getDefaultStep());
        config.setCacheExpireSeconds(properties.getCacheExpireSeconds());
        config.setCacheHoldSeconds(properties.getCacheHoldSeconds());
        config.setRegionBits(properties.getRegionBits());
        config.setMaxRetry(properties.getMaxRetry());
        config.setRetryIntervalMillis(properties.getRetryIntervalMillis());
        config.setRegionId(properties.getRegionId());
        config.setLockExpireMillis(properties.getLockExpireMillis());
        return new CamelliaStrictIdGen(config);
    }
}
