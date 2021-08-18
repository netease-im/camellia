package com.netease.nim.camellia.spring.redis.samples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.util.SafeEncoder;

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
        template.executePipelined((RedisCallback<Object>) connection -> {
            connection.setEx(SafeEncoder.encode("k1"), 100, SafeEncoder.encode("v2"));
            connection.setEx(SafeEncoder.encode("k12"), 100, SafeEncoder.encode("v12"));
            return null;
        });
        String k1 = template.opsForValue().get("k1");
        String k12 = template.opsForValue().get("k12");
        if (k1 != null && k1.equals("v2") && k12 != null && k12.equals("v12")) {
            logger.info("test success");
        } else {
            logger.info("test fail");
        }

    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        test();
    }
}
