package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by caojiajun on 2024/4/7
 */
public class TestLua {

    public static void main(String[] args) {
//        byte[][] bytes = new byte[2][];
//        bytes[0] = "abc".getBytes(StandardCharsets.UTF_8);
//        bytes[1] = "def".getBytes(StandardCharsets.UTF_8);
//        for (byte[] aByte : bytes) {
//            System.out.println(new String(aByte));
//        }
        CamelliaRedisTemplate template = new CamelliaRedisTemplate("redis://@127.0.0.1:6379");
        byte[] script = ("local ret1 = '1';\n" +
                "local ret2 = '1';\n" +
                "local ret3 = '1';\n" +
                "local arg1 = redis.call('exists', KEYS[1]);\n" +
                "if tonumber(arg1) == 1 then\n" +
                "\tret1 = '2';\n" +
                "\tret2 = redis.call('hset', KEYS[1], ARGV[1], ARGV[2]);\n" +
                "end\n" +
                "local arg2 = redis.call('pttl', KEYS[2]);\n" +
                "if tonumber(arg2) > 0 then\n" +
                "\tret3 = '2';\n" +
                "\tredis.call('psetex', KEYS[2], arg2, ARGV[2]);\n" +
                "end\n" +
                "return {ret1, ret2, ret3};").getBytes(StandardCharsets.UTF_8);

        Object eval = template.eval(script, 2, "hk1".getBytes(StandardCharsets.UTF_8),
                "key1".getBytes(StandardCharsets.UTF_8), "field".getBytes(StandardCharsets.UTF_8), "value".getBytes(StandardCharsets.UTF_8));
        if (eval instanceof List) {
            List list = (List) eval;
            for (Object o : list) {
                if (o instanceof byte[]) {
                    System.out.println(new String((byte[]) o));
                }
            }
        }
        System.out.println(eval);
    }
}
