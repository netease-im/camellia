package com.netease.nim.camellia.redis.samples;

import java.nio.charset.StandardCharsets;

/**
 * Created by caojiajun on 2024/4/7
 */
public class TestLua {

    public static void main(String[] args) {
        byte[][] bytes = new byte[2][];
        bytes[0] = "abc".getBytes(StandardCharsets.UTF_8);
        bytes[1] = "def".getBytes(StandardCharsets.UTF_8);
        for (byte[] aByte : bytes) {
            System.out.println(new String(aByte));
        }
//        CamelliaRedisTemplate template = new CamelliaRedisTemplate("redis://@127.0.0.1:6379");
//        byte[] script = ("local ret1 = redis.call('get', KEYS[1]);\n" +
//                "if ret1 then\n" +
//                "\tredis.call('pexpire', KEYS[1], ARGV[2]);\n" +
//                "\treturn {'1', ret1};\n" +
//                "end\n" +
//                "local arg1 = redis.call('exists', KEYS[2]);\n" +
//                "if tonumber(arg1) == 1 then\n" +
//                "\tlocal ret2 = redis.call('hget', KEYS[2], ARGV[1]);\n" +
//                "\tredis.call('pexpire', KEYS[2], ARGV[3]);\n" +
//                "\treturn {'2', ret2};\n" +
//                "end\n" +
//                "return {'3'};").getBytes(StandardCharsets.UTF_8);
//
//        Object eval = template.eval(script, 2, "key1".getBytes(StandardCharsets.UTF_8),
//                "key2".getBytes(StandardCharsets.UTF_8), "hk".getBytes(StandardCharsets.UTF_8),
//                String.valueOf(100*1000).getBytes(StandardCharsets.UTF_8), String.valueOf(100*1000).getBytes(StandardCharsets.UTF_8));
//        if (eval instanceof List) {
//            byte[] o = (byte[])((List<?>) eval).get(0);
//            String type = new String(o);
//            System.out.println("type=" + type);
//            if (type.equalsIgnoreCase("1") || type.equalsIgnoreCase("2")) {
//                byte[] value = (byte[])((List<?>) eval).get(1);
//                System.out.println(new String(value));
//            }
//            System.out.println(new String(o));
//        }
//        System.out.println(eval);
    }
}
