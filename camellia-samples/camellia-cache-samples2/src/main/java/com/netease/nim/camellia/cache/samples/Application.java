package com.netease.nim.camellia.cache.samples;

import com.netease.nim.camellia.cache.core.boot.EnableCamelliaCaching;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 *
 * Created by caojiajun on 2019/9/19.
 */
@SpringBootApplication
@EnableCamelliaCaching
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
