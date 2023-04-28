package com.netease.nim.camellia.tools.samples;

import com.netease.nim.camellia.tools.cache.CamelliaLoadingCache;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2023/4/28
 */
public class TestCamelliaLoadingCache {

    private static final AtomicLong id = new AtomicLong(0);

    public static void main(String[] args) throws InterruptedException {
        CamelliaLoadingCache<String, String> camelliaLoadingCache = new CamelliaLoadingCache.Builder<String, String>()
                .maxLoadTimeMs(1000)
                .expireMillis(1000)
                .build(key -> {
                    id.incrementAndGet();
                    System.out.println("load start=" + id.get());
                    if (id.get() == 2) {
                        Thread.sleep(2000);
                    }
                    if (id.get() == 4) {
                        throw new IllegalArgumentException("error");
                    }
                    System.out.println("load success=" + id.get());
                    return key + "-" + id.get();
                });
        String key;

        long start = System.currentTimeMillis();
        key = camelliaLoadingCache.get("key");
        System.out.println(key + ",spend=" + (System.currentTimeMillis() - start));
        Thread.sleep(60*1000);
        while (true) {
            start = System.currentTimeMillis();
            key = camelliaLoadingCache.get("key");
            System.out.println(key + ",spend=" + (System.currentTimeMillis() - start));
            Thread.sleep(500);
        }
    }
}
