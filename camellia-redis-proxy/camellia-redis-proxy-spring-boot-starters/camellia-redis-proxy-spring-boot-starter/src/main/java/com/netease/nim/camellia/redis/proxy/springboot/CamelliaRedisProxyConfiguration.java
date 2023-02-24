package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.console.ConsoleService;
import com.netease.nim.camellia.redis.proxy.console.ConsoleServiceAdaptor;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.springboot.conf.CamelliaRedisProxyProperties;
import com.netease.nim.camellia.redis.proxy.springboot.conf.NettyProperties;
import com.netease.nim.camellia.redis.proxy.springboot.conf.TranspondProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
@Configuration
@Import(CamelliaRedisProxyConfigurerSupport.class)
@EnableConfigurationProperties({CamelliaRedisProxyProperties.class, NettyProperties.class, TranspondProperties.class})
public class CamelliaRedisProxyConfiguration implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisProxyConfiguration.class);

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
    public CamelliaServerProperties camelliaServerProperties(CamelliaRedisProxyProperties properties) {
        return CamelliaRedisProxyUtil.parse(properties, springProxyBeanFactory(), applicationName, port);
    }

    @Bean
    public CamelliaRedisProxyBoot redisProxyBoot(CamelliaRedisProxyProperties properties) throws Exception {
        CamelliaServerProperties serverProperties = camelliaServerProperties(properties);
        GlobalRedisProxyEnv.init(serverProperties);

        CamelliaTranspondProperties transpondProperties = new CamelliaTranspondProperties();
        TranspondProperties transpond = properties.getTranspond();

        transpondProperties.setType(CamelliaRedisProxyUtil.parseType(transpond));
        transpondProperties.setLocal(CamelliaRedisProxyUtil.parse(transpond.getLocal()));
        transpondProperties.setRemote(CamelliaRedisProxyUtil.parse(transpond.getRemote()));
        transpondProperties.setCustom(CamelliaRedisProxyUtil.parse(transpond.getCustom()));
        transpondProperties.setRedisConf(CamelliaRedisProxyUtil.parse(transpond.getRedisConf()));
        transpondProperties.setNettyProperties(CamelliaRedisProxyUtil.parse(transpond.getNetty()));

        CommandInvoker commandInvoker = new CommandInvoker(serverProperties, transpondProperties);

        GlobalRedisProxyEnv.init(serverProperties);
        return new CamelliaRedisProxyBoot(serverProperties, commandInvoker);
    }

    @Bean
    @ConditionalOnClass({CamelliaConsoleServerBoot.class, ConsoleService.class})
    public CamelliaConsoleServerBoot consoleServerBoot() {
        return new CamelliaConsoleServerBoot();
    }

    @Bean
    @ConditionalOnMissingBean(value = ConsoleService.class)
    public ConsoleService consoleService() {
        return new ConsoleServiceAdaptor();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
