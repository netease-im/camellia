package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.command.AsyncCommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.console.ConsoleService;
import com.netease.nim.camellia.redis.proxy.console.ConsoleServiceAdaptor;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.springboot.conf.CamelliaRedisProxyProperties;
import com.netease.nim.camellia.redis.proxy.springboot.conf.NettyProperties;
import com.netease.nim.camellia.redis.proxy.springboot.conf.TranspondProperties;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
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
    @ConditionalOnMissingBean(value = {CommandInvoker.class})
    public CommandInvoker commandInvoker(CamelliaRedisProxyProperties properties) {
        CamelliaTranspondProperties transpondProperties = new CamelliaTranspondProperties();
        TranspondProperties transpond = properties.getTranspond();

        transpondProperties.setType(CamelliaRedisProxyUtil.parseType(transpond));
        transpondProperties.setLocal(CamelliaRedisProxyUtil.parse(transpond.getLocal()));
        transpondProperties.setRemote(CamelliaRedisProxyUtil.parse(transpond.getRemote()));
        transpondProperties.setCustom(CamelliaRedisProxyUtil.parse(transpond.getCustom()));
        transpondProperties.setRedisConf(CamelliaRedisProxyUtil.parse(transpond.getRedisConf()));
        transpondProperties.setNettyProperties(CamelliaRedisProxyUtil.parse(transpond.getNetty()));

        GlobalRedisProxyEnv.bossGroup = bossGroup(properties).get();
        GlobalRedisProxyEnv.workGroup = workGroup(properties).get();
        return new AsyncCommandInvoker(camelliaServerProperties(properties), transpondProperties);
    }

    @Bean
    @Qualifier("bossGroup")
    public EventLoopGroupGetter bossGroup(CamelliaRedisProxyProperties properties) {
        int bossThread = camelliaServerProperties(properties).getBossThread();
        logger.info("CamelliaRedisProxyServer init, bossThread = {}", bossThread);
        GlobalRedisProxyEnv.bossThread = bossThread;
        return new EventLoopGroupGetter(new NioEventLoopGroup(bossThread, new DefaultThreadFactory("camellia-boss-group")));
    }

    @Bean
    @Qualifier("workGroup")
    public EventLoopGroupGetter workGroup(CamelliaRedisProxyProperties properties) {
        CamelliaServerProperties serverProperties = camelliaServerProperties(properties);
        int workThread = serverProperties.getWorkThread();
        logger.info("CamelliaRedisProxyServer init, workThread = {}", workThread);
        GlobalRedisProxyEnv.workThread = workThread;
        return new EventLoopGroupGetter(new NioEventLoopGroup(workThread, new DefaultThreadFactory("camellia-work-group")));
    }

    @Bean
    public CamelliaRedisProxyBoot redisProxyBoot(CamelliaRedisProxyProperties properties) throws Exception {
        CommandInvoker commandInvoker = commandInvoker(properties);
        CamelliaServerProperties serverProperties = camelliaServerProperties(properties);
        GlobalRedisProxyEnv.bossGroup = bossGroup(properties).get();
        GlobalRedisProxyEnv.workGroup = workGroup(properties).get();
        return new CamelliaRedisProxyBoot(serverProperties, GlobalRedisProxyEnv.bossGroup, GlobalRedisProxyEnv.workGroup, commandInvoker);
    }

    @Bean
    @ConditionalOnClass({CamelliaConsoleServerBoot.class, ConsoleService.class})
    public CamelliaConsoleServerBoot consoleServerBoot() {
        return new CamelliaConsoleServerBoot();
    }

    @Bean
    @ConditionalOnMissingBean(value = ConsoleService.class)
    public ConsoleService consoleService(CamelliaRedisProxyProperties properties) {
        CamelliaServerProperties serverProperties = camelliaServerProperties(properties);
        return new ConsoleServiceAdaptor(serverProperties.getPort());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private static class EventLoopGroupGetter {
        private final EventLoopGroup eventLoopGroup;

        public EventLoopGroupGetter(EventLoopGroup eventLoopGroup) {
            this.eventLoopGroup = eventLoopGroup;
        }

        public EventLoopGroup get() {
            return eventLoopGroup;
        }
    }
}
