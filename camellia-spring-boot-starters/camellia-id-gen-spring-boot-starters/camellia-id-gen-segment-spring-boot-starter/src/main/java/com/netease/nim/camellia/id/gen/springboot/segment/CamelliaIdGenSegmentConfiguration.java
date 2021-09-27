package com.netease.nim.camellia.id.gen.springboot.segment;

import com.netease.nim.camellia.id.gen.common.IDLoader;
import com.netease.nim.camellia.id.gen.segment.CamelliaSegmentIdGen;
import com.netease.nim.camellia.id.gen.segment.CamelliaSegmentIdGenConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Created by caojiajun on 2021/9/27
 */
@Configuration
@EnableConfigurationProperties({CamelliaIdGenSegmentProperties.class})
public class CamelliaIdGenSegmentConfiguration {

    @Autowired(required = false)
    private IDLoader idLoader;

    @Bean
    public CamelliaSegmentIdGen camelliaSegmentIdGen(CamelliaIdGenSegmentProperties properties) {
        CamelliaSegmentIdGenConfig config = new CamelliaSegmentIdGenConfig();
        config.setRegionBits(properties.getRegionBits());
        config.setRegionId(properties.getRegionId());
        config.setMaxRetry(properties.getMaxRetry());
        config.setRetryIntervalMillis(properties.getRetryIntervalMillis());
        config.setTagCount(properties.getTagCount());
        config.setStep(properties.getStep());
        config.setIdLoader(idLoader);
        return new CamelliaSegmentIdGen(config);
    }
}
