package com.netease.nim.camellia.spring.redis.samples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Sample Code
 * Created by caojiajun on 2019/11/13.
 */
@SpringBootApplication
public class Application implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Autowired
    private StringRedisTemplate template;

    public void test() {
        template.opsForValue().set("k1", "v1", 10, TimeUnit.SECONDS);
        logger.info("test success");
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        test();
    }
}
