package com.netease.nim.camellia.delayqueue.server.samples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by caojiajun on 2022/7/21
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.netease.nim.camellia.delayqueue.server"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
