package com.netease.nim.camellia.id.gen.snowflake.samples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by caojiajun on 2021/9/26
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.netease.nim.camellia.id.gen.springboot.snowflake"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}
