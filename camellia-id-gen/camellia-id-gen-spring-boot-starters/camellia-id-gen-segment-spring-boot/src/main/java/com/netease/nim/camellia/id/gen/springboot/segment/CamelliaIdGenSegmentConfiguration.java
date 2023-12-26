package com.netease.nim.camellia.id.gen.springboot.segment;

import com.netease.nim.camellia.id.gen.common.IDLoader;
import com.netease.nim.camellia.id.gen.monitor.IdGenMonitor;
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
        config.setRegionIdShiftingBits(properties.getRegionIdShiftingBits());
        config.setMaxRetry(properties.getMaxRetry());
        config.setRetryIntervalMillis(properties.getRetryIntervalMillis());
        config.setTagCount(properties.getTagCount());
        config.setStep(properties.getStep());
        config.setIdLoader(idLoader);
        IdGenMonitor.init(properties.getMonitorIntervalSeconds());
        return new CamelliaSegmentIdGen(config);
    }

    @Bean
    public IdSyncInMultiRegionService idSyncInMultiRegionService(CamelliaIdGenSegmentProperties properties) {
        return new IdSyncInMultiRegionService(idLoader, properties.getIdSyncInMultiRegionsConf());
    }

}
