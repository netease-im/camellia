package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.ServerConf;
import com.netease.nim.camellia.redis.proxy.console.ConsoleService;
import com.netease.nim.camellia.redis.proxy.console.ConsoleServiceAdaptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
@Configuration
@EnableConfigurationProperties({CamelliaRedisProxyProperties.class})
public class CamelliaRedisProxyConfiguration implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Value("${server.port:6379}")
    private int port;

    @Value("${spring.application.name:camellia-redis-proxy}")
    private String applicationName;

    @Autowired(required = false)
    private ConsoleService consoleService;

    @Bean
    public SpringProxyBeanFactory springProxyBeanFactory() {
        return new SpringProxyBeanFactory(applicationContext);
    }

    @Bean
    public CamelliaRedisProxyBoot redisProxyBoot(CamelliaRedisProxyProperties properties) throws Exception {
        CamelliaServerProperties serverProperties = new CamelliaServerProperties();
        serverProperties.setConfig(properties.getConfig());

        ServerConf.init(serverProperties, port, applicationName, springProxyBeanFactory());

        return new CamelliaRedisProxyBoot(consoleServerBoot());
    }

    @Bean
    public CamelliaConsoleServerBoot consoleServerBoot() {
        if (consoleService == null) {
            consoleService = new ConsoleServiceAdaptor();
        }
        return new CamelliaConsoleServerBoot(consoleService);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
