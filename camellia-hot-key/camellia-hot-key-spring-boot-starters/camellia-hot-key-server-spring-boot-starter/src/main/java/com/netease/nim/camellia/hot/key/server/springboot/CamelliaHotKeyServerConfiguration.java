package com.netease.nim.camellia.hot.key.server.springboot;

import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.hot.key.server.springboot.conf.CamelliaHotKeyServerProperties;
import com.netease.nim.camellia.hot.key.server.springboot.conf.ConfUtils;
import com.netease.nim.camellia.hot.key.server.springboot.conf.NettyProperties;
import com.netease.nim.camellia.hot.key.server.springboot.conf.SpringProxyBeanFactory;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.springboot.CamelliaRedisConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Created by caojiajun on 2023/5/10
 */
@Configuration
@EnableConfigurationProperties({CamelliaHotKeyServerProperties.class, NettyProperties.class})
@Import(value = {CamelliaRedisConfiguration.class})
public class CamelliaHotKeyServerConfiguration implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaHotKeyServerConfiguration.class);

    private ApplicationContext applicationContext;

    @Value("${server.port:6379}")
    private int port;

    @Value("${spring.application.name:camellia-redis-proxy}")
    private String applicationName;

    @Bean
    public SpringProxyBeanFactory springProxyBeanFactory() {
        return new SpringProxyBeanFactory(applicationContext);
    }

    @Bean
    public CamelliaHotKeyServerBoot hotKeyServerBoot(CamelliaHotKeyServerProperties serverProperties, CamelliaRedisTemplate template) {
        HotKeyServerProperties properties = ConfUtils.parse(serverProperties);
        properties.setApplicationName(applicationName);
        properties.setPort(port);
        properties.setRedisTemplate(template);
        properties.setBeanFactory(springProxyBeanFactory());
        return new CamelliaHotKeyServerBoot(properties);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
