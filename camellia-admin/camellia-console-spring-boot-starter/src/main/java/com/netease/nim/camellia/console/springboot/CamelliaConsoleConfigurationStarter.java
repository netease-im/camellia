package com.netease.nim.camellia.console.springboot;

import com.netease.nim.camellia.console.conf.ConsoleProperties;
import com.netease.nim.camellia.console.conf.PropertyConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Configuration
@EnableConfigurationProperties({CamelliaConsoleConfiguration.class})
public class CamelliaConsoleConfigurationStarter {
    private static final Logger logger= LoggerFactory.getLogger(CamelliaConsoleConfigurationStarter.class);

    @Bean
    public ConsoleProperties dashboardConfiguration(CamelliaConsoleConfiguration configuration) {
        ConsoleProperties dashboardProperties = new ConsoleProperties();
        if(configuration.getHeartCallSeconds()< PropertyConstant.heartCallSeconds){
            logger.info("customer heartCallSeconds:{} is less than default:{},use default value", configuration.getHeartCallSeconds(), PropertyConstant.heartCallSeconds);
            configuration.setHeartCallSeconds(PropertyConstant.heartCallSeconds);
        }
        if(configuration.getReloadSeconds() < PropertyConstant.reloadSeconds){
            logger.info("customer reloadSeconds:{} is less than default:{},use default value", configuration.getReloadSeconds(), PropertyConstant.reloadSeconds);
            configuration.setReloadSeconds(PropertyConstant.reloadSeconds);
        }
        dashboardProperties.setHeartCallSeconds(configuration.getHeartCallSeconds());
        dashboardProperties.setReloadSeconds(configuration.getReloadSeconds());
        return dashboardProperties;
    }


}
