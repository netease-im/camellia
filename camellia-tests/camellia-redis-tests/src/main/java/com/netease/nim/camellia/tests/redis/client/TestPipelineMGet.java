package com.netease.nim.camellia.tests.redis.client;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import redis.clients.jedis.Response;
import redis.clients.util.SafeEncoder;

import java.util.List;
import java.util.Objects;

/**
 * Created by caojiajun on 2025/7/4
 */
public class TestPipelineMGet {

    public static void main(String[] args) throws Exception {
        String s = "redis://@127.0.0.1:6379";

        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(s);
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(resourceTable);

        while (true) {
            {
                template.del("k1", "k2", "k3", "k4");
                template.set("k1", "v1");
                template.set("k2", "v2");
                template.set("k4", "v4");
                template.set("{k1}abc", "v5");
                try (ICamelliaRedisPipeline pipeline = template.pipelined()) {
                    Response<List<String>> mget = pipeline.mget("k1", "k2", "k3", "k4", "{k1}abc");
                    pipeline.sync();
                    List<String> strings = mget.get();
                    assertEquals(strings.get(0), "v1");
                    assertEquals(strings.get(1), "v2");
                    assertEquals(strings.get(2), null);
                    assertEquals(strings.get(3), "v4");
                    assertEquals(strings.get(4), "v5");
                }
            }
            {
                template.del("k1", "k2", "k3", "k4");
                template.set("k1", "v1");
                template.set("k2", "v2");
                template.set("k4", "v4");
                template.set("{k1}abc", "v5");
                try (ICamelliaRedisPipeline pipeline = template.pipelined()) {
                    Response<List<byte[]>> mget = pipeline.mget(SafeEncoder.encode("k1"), SafeEncoder.encode("k2"),
                            SafeEncoder.encode("k3"), SafeEncoder.encode("k4"), SafeEncoder.encode("{k1}abc"));
                    pipeline.sync();
                    List<byte[]> strings = mget.get();
                    assertEquals(SafeEncoder.encode(strings.get(0)), "v1");
                    assertEquals(SafeEncoder.encode(strings.get(1)), "v2");
                    assertEquals(strings.get(2), null);
                    assertEquals(SafeEncoder.encode(strings.get(3)), "v4");
                    assertEquals(SafeEncoder.encode(strings.get(4)), "v5");
                }
            }
            Thread.sleep(1000);
        }

    }

    private static void assertEquals(Object result, Object expect) {
        if (Objects.equals(result, expect)) {
            System.out.println("SUCCESS, thread=" + Thread.currentThread().getName());
        } else {
            System.out.println("ERROR, expect " + expect + " but found " + result);
            throw new RuntimeException();
        }
    }
}
