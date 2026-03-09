package com.netease.nim.camellia.redis.proxy.bootstrap;

import com.netease.nim.camellia.core.constant.TemplateBanner;
import com.netease.nim.camellia.redis.proxy.springboot.EnableCamelliaRedisProxyServer;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.io.PrintStream;

@SpringBootApplication
@EnableCamelliaRedisProxyServer
public class Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        application.setBannerMode(Banner.Mode.LOG);
        application.setBanner(new CamelliaBanner());
        application.run(args);
    }

    public static class CamelliaBanner extends TemplateBanner implements Banner {

        public CamelliaBanner() {
            super("Camellia-Redis-Proxy-Server");
        }

        @Override
        public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
            print(out);
        }
    }
}
