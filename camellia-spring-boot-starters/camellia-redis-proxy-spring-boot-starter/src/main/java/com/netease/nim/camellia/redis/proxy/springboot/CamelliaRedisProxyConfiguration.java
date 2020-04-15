package com.netease.nim.camellia.redis.proxy.springboot;

import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.command.async.AsyncCommandInvoker;
import com.netease.nim.camellia.redis.proxy.command.sync.SyncCommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.console.ConsoleService;
import com.netease.nim.camellia.redis.proxy.console.ConsoleServiceAdaptor;
import com.netease.nim.camellia.redis.proxy.springboot.conf.CamelliaRedisProxyProperties;
import com.netease.nim.camellia.redis.proxy.springboot.conf.NettyProperties;
import com.netease.nim.camellia.redis.proxy.springboot.conf.TranspondProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Constructor;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
@Configuration
@EnableConfigurationProperties({CamelliaRedisProxyProperties.class, NettyProperties.class, TranspondProperties.class})
public class CamelliaRedisProxyConfiguration {

    @Value("${server.port:6379}")
    private int port;

    @Value("${spring.application.name:camellia-redis-proxy}")
    private String applicationName;

    @Bean
    @ConditionalOnMissingBean(value = {CommandInvoker.class})
    public CommandInvoker commandInvoker(CamelliaRedisProxyProperties properties) throws Exception {

        CamelliaRedisProxyProperties.Type type = properties.getType();

        CamelliaTranspondProperties transpondProperties = new CamelliaTranspondProperties();
        TranspondProperties transpond = properties.getTranspond();

        transpondProperties.setType(CamelliaRedisProxyUtil.parseType(transpond));
        transpondProperties.setLocal(CamelliaRedisProxyUtil.parse(transpond.getLocal()));
        transpondProperties.setRemote(CamelliaRedisProxyUtil.parse(transpond.getRemote()));
        transpondProperties.setRedisConf(CamelliaRedisProxyUtil.parse(transpond.getRedisConf()));

        CommandInvoker commandInvoker;
        if (type == CamelliaRedisProxyProperties.Type.sync) {
            commandInvoker = new SyncCommandInvoker(transpondProperties);
        } else if (type == CamelliaRedisProxyProperties.Type.async) {
            commandInvoker = new AsyncCommandInvoker(transpondProperties);
        } else if (type == CamelliaRedisProxyProperties.Type.custom) {
            String className = properties.getCustomCommandInvokerClassName();
            if (className == null) {
                throw new IllegalArgumentException("custom type should provide customCommandInvokerClassName");
            }
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor(CamelliaTranspondProperties.class);
            commandInvoker = (CommandInvoker) constructor.newInstance(transpondProperties);
        } else {
            throw new IllegalArgumentException("only support sync/async/custom type");
        }
        return commandInvoker;
    }

    @Bean
    public CamelliaRedisProxyBoot redisProxyBoot(CamelliaRedisProxyProperties properties) throws Exception {
        CommandInvoker commandInvoker = commandInvoker(properties);
        return new CamelliaRedisProxyBoot(properties, commandInvoker, applicationName, port);
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
}
