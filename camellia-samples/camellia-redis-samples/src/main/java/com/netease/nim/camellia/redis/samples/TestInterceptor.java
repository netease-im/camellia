package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.intercept.InterceptContext;
import com.netease.nim.camellia.redis.intercept.RedisInterceptor;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by caojiajun on 2023/7/19
 */
public class TestInterceptor {

    public static void main(String[] args) {
        CamelliaRedisEnv redisEnv = new CamelliaRedisEnv.Builder()
                .addInterceptor(new RedisInterceptor() {
                    @Override
                    public void before(InterceptContext context) {
                        System.out.println("before,key=" + new String(context.getKey(), StandardCharsets.UTF_8) + ",command=" + context.getCommand() + ",pipeline=" + context.isPipeline() + ",resource=" + context.getResource().getUrl());
                    }

                    @Override
                    public void after(InterceptContext context) {
                        System.out.println("after,key=" + new String(context.getKey(), StandardCharsets.UTF_8) + ",command=" + context.getCommand() + ",pipeline=" + context.isPipeline() + ",resource=" + context.getResource().getUrl());
                    }
                }).build();
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, ResourceTableUtil.simpleTable(new Resource("redis://@127.0.0.1:6379")));

        String k1 = template.get("k1");
        System.out.println(k1);
        String set = template.set("k1", "v1");
        System.out.println(set);

        List<String> mget = template.mget("k1", "k2", "k3");
        System.out.println(mget);

        String mset = template.mset("k1", "V2", "k2", "v2");
        System.out.println(mset);

        try (ICamelliaRedisPipeline pipeline = template.pipelined()) {
            pipeline.zadd("zk", 1, "vv1");
            pipeline.expire("zk", 100);
            pipeline.sync();
        }

    }
}
