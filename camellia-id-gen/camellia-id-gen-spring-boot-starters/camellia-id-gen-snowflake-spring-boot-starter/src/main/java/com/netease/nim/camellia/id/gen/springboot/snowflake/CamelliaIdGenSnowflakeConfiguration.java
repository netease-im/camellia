package com.netease.nim.camellia.id.gen.springboot.snowflake;

import com.netease.nim.camellia.id.gen.common.CamelliaIdGenException;
import com.netease.nim.camellia.id.gen.monitor.IdGenMonitor;
import com.netease.nim.camellia.id.gen.snowflake.CamelliaSnowflakeConfig;
import com.netease.nim.camellia.id.gen.snowflake.CamelliaSnowflakeIdGen;
import com.netease.nim.camellia.id.gen.snowflake.RedisWorkerIdGen;
import com.netease.nim.camellia.id.gen.snowflake.WorkerIdGen;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * Created by caojiajun on 2021/9/26
 */
@Configuration
@EnableConfigurationProperties({CamelliaIdGenSnowflakeProperties.class})
public class CamelliaIdGenSnowflakeConfiguration {

    @Autowired(required = false)
    private CamelliaRedisTemplate camelliaRedisTemplate;

    @Bean
    public CamelliaSnowflakeIdGen camelliaSnowflakeIdGen(CamelliaIdGenSnowflakeProperties properties) {
        return new CamelliaSnowflakeIdGen(camelliaSnowflakeConfig(properties));
    }

    @Bean
    public CamelliaSnowflakeConfig camelliaSnowflakeConfig(CamelliaIdGenSnowflakeProperties properties) {
        CamelliaSnowflakeConfig config = new CamelliaSnowflakeConfig();
        config.setTwepoch(properties.getTwepoch());
        config.setSequenceBits(properties.getSequenceBits());
        config.setWorkerIdBits(properties.getWorkerIdBits());
        config.setRegionBits(properties.getRegionBits());
        config.setRegionId(properties.getRegionId());
        config.setWorkerIdGen(workerIdGen(properties));
        IdGenMonitor.init(properties.getMonitorIntervalSeconds());
        return config;
    }

    @Bean
    @ConditionalOnMissingBean(value = WorkerIdGen.class)
    public WorkerIdGen workerIdGen(CamelliaIdGenSnowflakeProperties properties) {
        long workerId = properties.getWorkerId();
        if (workerId >= 0) {
            return maxWorkerId -> workerId;
        }
        if (camelliaRedisTemplate == null) {
            throw new CamelliaIdGenException("redis template not found");
        }
        CamelliaIdGenSnowflakeProperties.RedisWorkerIdGenConf conf = properties.getRedisWorkerIdGenConf();
        return new RedisWorkerIdGen(camelliaRedisTemplate, conf.getNamespace(),
                conf.getLockExpireMillis(), conf.getRenewIntervalMillis(), conf.isExitIfRenewFail());
    }
}
