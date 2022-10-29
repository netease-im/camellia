package com.netease.nim.camellia.delayqueue.server.springboot;

import com.netease.nim.camellia.delayqueue.server.CamelliaDelayQueueServer;
import com.netease.nim.camellia.delayqueue.server.CamelliaDelayQueueServerConfig;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



/**
 * Created by caojiajun on 2021/9/27
 */
@Configuration
@EnableConfigurationProperties({CamelliaDelayQueueServerProperties.class})
public class CamelliaDelayQueueServerConfiguration {

    @Autowired(required = false)
    private CamelliaRedisTemplate template;

    @Bean
    public CamelliaDelayQueueServer camelliaDelayQueueServer(CamelliaDelayQueueServerProperties properties) {
        CamelliaDelayQueueServerConfig serverConfig = new CamelliaDelayQueueServerConfig();
        serverConfig.setNamespace(properties.getNamespace());
        serverConfig.setAckTimeoutMillis(properties.getAckTimeoutMillis());
        serverConfig.setMaxRetry(properties.getMaxRetry());
        serverConfig.setCheckTimeoutThreadNum(properties.getCheckTimeoutThreadNum());
        serverConfig.setCheckTriggerThreadNum(properties.getCheckTriggerThreadNum());
        serverConfig.setMsgScheduleMillis(properties.getMsgScheduleMillis());
        serverConfig.setTopicScheduleSeconds(properties.getTopicScheduleSeconds());
        serverConfig.setTtlMillis(properties.getTtlMillis());
        serverConfig.setScheduleThreadNum(properties.getScheduleThreadNum());
        serverConfig.setEndLifeMsgExpireMillis(properties.getEndLifeMsgExpireMillis());
        serverConfig.setTopicActiveTagTimeoutMillis(properties.getTopicActiveTagTimeoutMillis());
        serverConfig.setMonitorIntervalSeconds(properties.getMonitorIntervalSeconds());
        if (template == null) {
            throw new IllegalArgumentException("redis template not found");
        }
        return new CamelliaDelayQueueServer(serverConfig, template);
    }

    @Bean
    public CamelliaDelayQueueLongPollingTaskExecutor camelliaDelayQueueLongPollingTaskExecutor(CamelliaDelayQueueServerProperties properties) {
        CamelliaDelayQueueServer server = camelliaDelayQueueServer(properties);
        return new CamelliaDelayQueueLongPollingTaskExecutor(server, properties.getLongPollingTaskQueueSize(),
                properties.getLongPollingScheduledThreadSize(), properties.getLongPollingScheduledQueueSize(),
                properties.getLongPollingMsgReadyCallbackThreadSize(), properties.getLongPollingMsgReadyCallbackQueueSize(),
                properties.getLongPollingTimeoutMillis());
    }
}
