package com.netease.nim.camellia.dashboard.samples;

import com.netease.nim.camellia.dashboard.CamelliaDashboardScanBase;
import com.netease.nim.camellia.dashboard.springboot.EnableCamelliaDashboard;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


/**
 * Created by caojiajun on 2019/11/13.
 */
@SpringBootApplication
@EnableCamelliaDashboard
@ComponentScan(basePackageClasses = {CamelliaDashboardScanBase.class, Application.class})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
