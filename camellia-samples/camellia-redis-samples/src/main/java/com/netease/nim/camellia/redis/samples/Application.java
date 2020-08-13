package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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

    public void test() {
        //用法和jedis一样，但是封装掉了连接池取连接和还连接的操作
        String set = template.set("k1", "v1");
        String k1 = template.get("k1");
        String setex = template.setex("k2", 10, "v2");
        List<String> mget = template.mget("k1", "k2");
        Long del = template.del("k1", "k2", "k3");

        //使用pipeline
        //pipeline对象实现了Closeable接口，使用完毕请close，或者使用try-resource的语法，这是和jedis的pipeline使用有差别的地方
        try (ICamelliaRedisPipeline pipelined = template.pipelined()) {
            Response<String> response1 = pipelined.set("k3", "v3");
            Response<Long> response2 = pipelined.setnx("k3", "v3");
            Response<Long> response3 = pipelined.zadd("k3", 1.0, "v3");

            pipelined.sync();

            Response<String> response4 = pipelined.get("k1");
            Response<Map<String, String>> response5 = pipelined.hgetAll("hk");

            pipelined.sync();
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
