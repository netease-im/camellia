package com.netease.nim.camellia.id.gen.segment.server.bootstrap;

import com.netease.nim.camellia.core.constant.TemplateBanner;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

import java.io.PrintStream;

/**
 * Created by caojiajun on 2021/9/27
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.netease.nim.camellia.id.gen.springboot.segment", "com.netease.nim.camellia.id.gen.springboot.idloader"})
@MapperScan("com.netease.nim.camellia.id.gen.springboot.idloader")
public class Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        application.setBannerMode(Banner.Mode.LOG);
        application.setBanner(new CamelliaBanner());
        application.run(args);
    }

    public static class CamelliaBanner extends TemplateBanner implements Banner {

        public CamelliaBanner() {
            super("Camellia-Id-Gen-Segment-Server");
        }

        @Override
        public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
            print(out);
        }
    }
}
