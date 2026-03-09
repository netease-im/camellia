package com.netease.nim.camellia.hot.key.server.bootstrap;

import com.netease.nim.camellia.core.constant.TemplateBanner;
import com.netease.nim.camellia.hot.key.server.springboot.EnableCamelliaHotKeyServer;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.io.PrintStream;

@SpringBootApplication
@EnableCamelliaHotKeyServer
public class Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        application.setBannerMode(Banner.Mode.LOG);
        application.setBanner(new CamelliaBanner());
        application.run(args);
    }

    public static class CamelliaBanner extends TemplateBanner implements Banner {

        public CamelliaBanner() {
            super("Camellia-Hot-Key-Server");
        }

        @Override
        public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
            print(out);
        }
    }
}
