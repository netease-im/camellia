package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.Response;

import java.util.List;
import java.util.Map;

/**
 * Sample Code
 * Created by caojiajun on 2019/11/13.
 */
@SpringBootApplication
public class Application implements InitializingBean {

    @Autowired
    private CamelliaRedisTemplate template;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void test1() {
        template.del("k1", "k2", "k3", "k4", "hk");
        //用法和jedis一样，但是封装掉了连接池取连接和还连接的操作
        String set = template.set("k1", "v1");
        System.out.println(set);
        String k1 = template.get("k1");
        System.out.println(k1);
        String setex = template.setex("k2", 10, "v2");
        System.out.println(setex);
        List<String> mget = template.mget("k1", "k2");
        System.out.println(mget);
        Long del = template.del("k1", "k2", "k3");
        System.out.println(del);

        //使用pipeline
        //pipeline对象实现了Closeable接口，使用完毕请close，或者使用try-resource的语法，这是和jedis的pipeline使用有差别的地方
        try (ICamelliaRedisPipeline pipelined = template.pipelined()) {
            Response<String> response1 = pipelined.set("k3", "v3");
            Response<Long> response2 = pipelined.setnx("k3", "v3");
            Response<Long> response3 = pipelined.zadd("k4", 1.0, "v3");

            pipelined.sync();

            Response<String> response4 = pipelined.get("k1");
            Response<Map<String, String>> response5 = pipelined.hgetAll("hk");

            pipelined.sync();

            System.out.println(response1.get());
            System.out.println(response2.get());
            System.out.println(response3.get());
            System.out.println(response4.get());
            System.out.println(response5.get());
        }
    }

    public void test2() {
        stringRedisTemplate.opsForValue().set("sk2", "v2");
        System.out.println(stringRedisTemplate.opsForValue().get("sk2"));
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        test1();
        test2();
    }
}
