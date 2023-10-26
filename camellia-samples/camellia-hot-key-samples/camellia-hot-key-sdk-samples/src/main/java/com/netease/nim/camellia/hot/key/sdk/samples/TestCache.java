package com.netease.nim.camellia.hot.key.sdk.samples;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2023/7/18
 */
public class TestCache {

    public static void main(String[] args) {
        for (int i=0; i<100; i++) {
            test1();
            test2();
        }
    }

    private static void test1() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>(256);
        long start = System.currentTimeMillis();
        for (int j=0; j<100; j++) {
            for (int i = 0; i < 10000; i++) {
                String string = UUID.randomUUID().toString();
                map.put(string, string);
                map.remove(string);
            }
            map.clear();
        }
        System.out.println(map.getClass().getSimpleName() + ",spend=" + (System.currentTimeMillis() - start));
    }

    private static void test2() {
        ConcurrentLinkedHashMap<String, String> map = new ConcurrentLinkedHashMap.Builder<String, String>()
                .initialCapacity(10000).maximumWeightedCapacity(10000).build();
        long start = System.currentTimeMillis();
        for (int j=0; j<100; j++) {
            for (int i = 0; i < 10000; i++) {
                String string = UUID.randomUUID().toString();
                map.put(string, string);
                map.remove(string);
            }
            map.clear();
        }
        System.out.println(map.getClass().getSimpleName() + ",spend=" + (System.currentTimeMillis() - start));
    }
}
