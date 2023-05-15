package com.netease.nim.camellia.hot.key.server.samples;

import com.netease.nim.camellia.hot.key.server.springboot.EnableCamelliaHotKeyServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCamelliaHotKeyServer
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
