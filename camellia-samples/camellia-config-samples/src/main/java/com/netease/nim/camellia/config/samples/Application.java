package com.netease.nim.camellia.config.samples;

import com.netease.nim.camellia.config.CamelliaConfigScanBase;
import com.netease.nim.camellia.config.springboot.EnableCamelliaConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


/**
 * Created by caojiajun on 2019/11/13.
 */
@SpringBootApplication
@EnableCamelliaConfig
@ComponentScan(basePackageClasses = {CamelliaConfigScanBase.class, Application.class})
@MapperScan("com.netease.nim.camellia.config.dao")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
