package com.netease.nim.camellia.naming.springboot;

import com.netease.nim.camellia.naming.core.CamelliaNamingException;
import com.netease.nim.camellia.naming.core.ICamelliaNamingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.EventListener;


/**
 *
 * Created by caojiajun on 2020/3/31.
 */
@Configuration
@EnableConfigurationProperties({CamelliaNamingProperties.class})
public class CamelliaNamingConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaNamingConfiguration.class);

    @Value("${server.port:-1}")
    private int port;

    @Value("${spring.application.name:}")
    private String applicationName;

    private CamelliaNamingBoot boot;

    @Bean
    @ConditionalOnProperty(name = "camellia-naming.enable", havingValue = "true")
    public ICamelliaNamingService camelliaNamingService(CamelliaNamingProperties properties) {
        CamelliaNamingBootConf bootConf = new CamelliaNamingBootConf();
        if (properties.getType() == CamelliaNamingProperties.Type.ZK) {
            bootConf.setType(CamelliaNamingBootConf.Type.ZK);
        } else if (properties.getType() == CamelliaNamingProperties.Type.NACOS) {
            bootConf.setType(CamelliaNamingBootConf.Type.NACOS);
        } else {
            throw new CamelliaNamingException("unknown naming type");
        }
        bootConf.setServiceName(properties.getServiceName());
        bootConf.setHost(properties.getHost());
        bootConf.setPort(properties.getPort());
        bootConf.setRegisterEnable(properties.isRegisterEnable());
        bootConf.setConfig(properties.getConfig());
        this.boot = CamelliaNamingBootStarter.init(bootConf, applicationName, port);
        return this.boot.getService();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void register(ApplicationReadyEvent readyEvent) {
        if (boot == null) {
            logger.warn("camellia naming boot is null, skip register");
            return;
        }
        boot.register();
    }

    @EventListener({ContextStoppedEvent.class, ContextClosedEvent.class})
    public void close(ApplicationEvent event) {
        if (boot == null) {
            logger.warn("camellia naming boot is null, skip deregister");
            return;
        }
        boot.deregister();
    }

}
