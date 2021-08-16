package com.netease.nim.camellia.redis.spring.template.adaptor.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * Created by caojiajun on 2021/8/2
 */
@ConfigurationProperties(prefix = "camellia-redis-spring-template-adaptor")
public class CamelliaRedisSpringTemplateAdaptorProperties {

    private boolean enable = true;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
