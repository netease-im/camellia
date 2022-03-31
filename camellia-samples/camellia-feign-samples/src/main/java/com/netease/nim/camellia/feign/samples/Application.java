package com.netease.nim.camellia.feign.samples;

import com.netease.nim.camellia.feign.springboot.EnableCamelliaFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by caojiajun on 2022/3/30
 */
@SpringBootApplication
@EnableCamelliaFeignClients(basePackages = {"com.netease.nim.camellia.feign.samples"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}
