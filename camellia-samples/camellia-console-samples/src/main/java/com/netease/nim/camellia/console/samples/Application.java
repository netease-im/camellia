package com.netease.nim.camellia.console.samples;

import com.netease.nim.camellia.console.CamelliaConsoleScanBase;
import com.netease.nim.camellia.console.springboot.EnableCamelliaConsole;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@SpringBootApplication
@EnableCamelliaConsole
@ComponentScan(basePackageClasses = {CamelliaConsoleScanBase.class, Application.class})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
