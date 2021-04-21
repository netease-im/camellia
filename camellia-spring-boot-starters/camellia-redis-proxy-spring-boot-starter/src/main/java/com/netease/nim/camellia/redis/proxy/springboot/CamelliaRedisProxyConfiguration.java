package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.command.async.AsyncCommandInvoker;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
@Configuration
@EnableConfigurationProperties({CamelliaRedisProxyProperties.class, NettyProperties.class, TranspondProperties.class})
public class CamelliaRedisProxyConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisProxyConfiguration.class);

    @Value("${server.port:6379}")
    private int port;

    @Value("${spring.application.name:camellia-redis-proxy}")
    private String applicationName;

    @Bean
    @ConditionalOnMissingBean(value = {CommandInvoker.class})
    public CommandInvoker commandInvoker(CamelliaRedisProxyProperties properties) throws Exception {
        CamelliaServerProperties serverProperties = CamelliaRedisProxyUtil.parse(properties, port);

        CamelliaTranspondProperties transpondProperties = new CamelliaTranspondProperties();
        TranspondProperties transpond = properties.getTranspond();

        transpondProperties.setType(CamelliaRedisProxyUtil.parseType(transpond));
        transpondProperties.setLocal(CamelliaRedisProxyUtil.parse(transpond.getLocal()));
        transpondProperties.setRemote(CamelliaRedisProxyUtil.parse(transpond.getRemote()));
        transpondProperties.setCustom(CamelliaRedisProxyUtil.parse(transpond.getCustom()));
        transpondProperties.setRedisConf(CamelliaRedisProxyUtil.parse(transpond.getRedisConf()));

        GlobalRedisProxyEnv.bossGroup = bossGroup(properties).get();
        GlobalRedisProxyEnv.workGroup = workGroup(properties).get();
        return new AsyncCommandInvoker(serverProperties, transpondProperties);
    }

    @Bean
    @Qualifier("bossGroup")
    public EventLoopGroupGetter bossGroup(CamelliaRedisProxyProperties properties) {
        CamelliaServerProperties serverProperties = CamelliaRedisProxyUtil.parse(properties, port);
        int bossThread = serverProperties.getBossThread();
        logger.info("CamelliaRedisProxyServer init, bossThread = {}", bossThread);
        GlobalRedisProxyEnv.bossThread = bossThread;
        return new EventLoopGroupGetter(new NioEventLoopGroup(bossThread, new DefaultThreadFactory("boss-group")));
    }

    @Bean
    @Qualifier("workGroup")
    public EventLoopGroupGetter workGroup(CamelliaRedisProxyProperties properties) {
        CamelliaServerProperties serverProperties = CamelliaRedisProxyUtil.parse(properties, port);
        int workThread = serverProperties.getWorkThread();
        logger.info("CamelliaRedisProxyServer init, workThread = {}", workThread);
        GlobalRedisProxyEnv.workThread = workThread;
        return new EventLoopGroupGetter(new NioEventLoopGroup(workThread, new DefaultThreadFactory("work-group")));
    }

    @Bean
    public CamelliaRedisProxyBoot redisProxyBoot(CamelliaRedisProxyProperties properties) throws Exception {
        CommandInvoker commandInvoker = commandInvoker(properties);
        CamelliaServerProperties serverProperties = CamelliaRedisProxyUtil.parse(properties, port);
        GlobalRedisProxyEnv.bossGroup = bossGroup(properties).get();
        GlobalRedisProxyEnv.workGroup = workGroup(properties).get();
        return new CamelliaRedisProxyBoot(serverProperties, GlobalRedisProxyEnv.bossGroup, GlobalRedisProxyEnv.workGroup, commandInvoker, applicationName);
    }

    @Bean
    @ConditionalOnClass({CamelliaConsoleServerBoot.class, ConsoleService.class})
    public CamelliaConsoleServerBoot consoleServerBoot() {
        return new CamelliaConsoleServerBoot();
    }

    @Bean
    @ConditionalOnMissingBean(value = ConsoleService.class)
    public ConsoleService consoleService() {
        return new ConsoleServiceAdaptor(port);
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
