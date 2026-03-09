package com.netease.nim.camellia.delayqueue.server.bootstrap;

import com.netease.nim.camellia.core.constant.TemplateBanner;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

import java.io.PrintStream;

/**
 * Created by caojiajun on 2022/7/21
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.netease.nim.camellia.delayqueue.server"})
public class Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        application.setBannerMode(Banner.Mode.LOG);
        application.setBanner(new CamelliaBanner());
        application.run(args);
    }

    public static class CamelliaBanner extends TemplateBanner implements Banner {

        public CamelliaBanner() {
            super("Camellia-Delay-Queue-Server");
        }

        @Override
        public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
            print(out);
        }
    }
}
