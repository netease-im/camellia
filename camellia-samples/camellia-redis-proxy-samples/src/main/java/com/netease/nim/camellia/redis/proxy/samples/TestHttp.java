package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.http.HttpCommandConverter;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.Arrays;
import java.util.List;

/**
 * Created by caojiajun on 2024/1/15
 */
public class TestHttp {


    public static void main(String[] args) {
        test(HttpCommandConverter.toCommand("set k1 v1"), Arrays.asList("set", "k1", "v1"));
        System.out.println("===");
        test(HttpCommandConverter.toCommand("set k1 'ab c'"), Arrays.asList("set", "k1", "ab c"));
        System.out.println("===");
        test(HttpCommandConverter.toCommand("set k1 \"ab c\""), Arrays.asList("set", "k1", "ab c"));
        System.out.println("===");
        test(HttpCommandConverter.toCommand("set k1 \"ab'c\""), Arrays.asList("set", "k1", "ab'c"));
        System.out.println("===");
        test(HttpCommandConverter.toCommand("set k1 \"ab' c\""), Arrays.asList("set", "k1", "ab' c"));
        System.out.println("===");
        test(HttpCommandConverter.toCommand("set k1 'ab\"c'"), Arrays.asList("set", "k1", "ab\"c"));
        System.out.println("===");
        test(HttpCommandConverter.toCommand("set k1 'ab\" c'   abc"), Arrays.asList("set", "k1", "ab\" c", "abc"));

        System.out.println("SUCCESS!!!");
    }

    public static void test(Command command, List<String> list) {
        byte[][] objects = command.getObjects();
        for (int i=0; i<objects.length; i++) {
            String s = Utils.bytesToString(objects[i]);
            System.out.println(s);
            boolean pass = s.equals(list.get(i));
            System.out.println(pass);
            if (!pass) {
                System.out.println("ERROR");
                System.exit(-1);
            }
        }
    }
}
