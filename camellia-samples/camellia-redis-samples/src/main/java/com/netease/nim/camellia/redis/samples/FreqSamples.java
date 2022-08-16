package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.toolkit.freq.CamelliaFreq;
import com.netease.nim.camellia.redis.toolkit.freq.CamelliaFreqConfig;
import com.netease.nim.camellia.redis.toolkit.freq.CamelliaFreqResponse;
import com.netease.nim.camellia.redis.toolkit.freq.CamelliaFreqType;

import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2022/8/8
 */
public class FreqSamples {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===test1===");
        test1();
        TimeUnit.SECONDS.sleep(2);
        System.out.println("===test2===");
        test2();
        TimeUnit.SECONDS.sleep(2);
        System.out.println("===test3===");
        test3();
        TimeUnit.SECONDS.sleep(2);
        System.out.println("===test4===");
        test4();
        TimeUnit.SECONDS.sleep(2);
        System.out.println("===test5===");
        test5();
        TimeUnit.SECONDS.sleep(2);
        System.out.println("===test6===");
        test6();
        System.out.println("===END====");
    }

    private static void test1() throws InterruptedException {
        CamelliaFreq freq = new CamelliaFreq(new CamelliaRedisTemplate("redis://@127.0.0.1:6379"));
        String freqKey = "k1";
        CamelliaFreqConfig config = new CamelliaFreqConfig();
        config.setThreshold(2);
        config.setCheckTime(1000);
        config.setBanTime(2000);
        for (int i=0; i<20; i++) {
            CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.CLUSTER, config);
            System.out.println(response.isPass());
            TimeUnit.MILLISECONDS.sleep(200);
        }
        TimeUnit.SECONDS.sleep(3);
        CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.CLUSTER, config);
        System.out.println(response.isPass());
    }

    private static void test2() throws InterruptedException {
        CamelliaFreq freq = new CamelliaFreq(new CamelliaRedisTemplate("redis://@127.0.0.1:6379"));
        String freqKey = "k1";
        CamelliaFreqConfig config = new CamelliaFreqConfig();
        config.setThreshold(2);
        config.setCheckTime(1000);
        config.setBanTime(2000);
        config.setDelayBanEnable(true);
        for (int i=0; i<20; i++) {
            CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.CLUSTER, config);
            System.out.println(response.isPass());
            TimeUnit.MILLISECONDS.sleep(200);
        }
        TimeUnit.SECONDS.sleep(3);
        CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.CLUSTER, config);
        System.out.println(response.isPass());
    }

    private static void test3() throws InterruptedException {
        CamelliaFreq freq = new CamelliaFreq(new CamelliaRedisTemplate("redis://@127.0.0.1:6379"));
        String freqKey = "k2";
        CamelliaFreqConfig config = new CamelliaFreqConfig();
        config.setThreshold(2);
        config.setCheckTime(1000);
        config.setBanTime(0);
        for (int i=0; i<20; i++) {
            CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.CLUSTER, config);
            System.out.println(response.isPass());
            TimeUnit.MILLISECONDS.sleep(200);
        }
        TimeUnit.SECONDS.sleep(3);
        CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.CLUSTER, config);
        System.out.println(response.isPass());
    }

    private static void test4() throws InterruptedException {
        CamelliaFreq freq = new CamelliaFreq(new CamelliaRedisTemplate("redis://@127.0.0.1:6379"));
        String freqKey = "k3";
        CamelliaFreqConfig config = new CamelliaFreqConfig();
        config.setThreshold(2);
        config.setCheckTime(1000);
        config.setBanTime(2000);
        for (int i=0; i<20; i++) {
            CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.STANDALONE, config);
            System.out.println(response.isPass());
            TimeUnit.MILLISECONDS.sleep(200);
        }
        TimeUnit.SECONDS.sleep(3);
        CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.STANDALONE, config);
        System.out.println(response.isPass());
    }

    private static void test5() throws InterruptedException {
        CamelliaFreq freq = new CamelliaFreq(new CamelliaRedisTemplate("redis://@127.0.0.1:6379"));
        String freqKey = "k3";
        CamelliaFreqConfig config = new CamelliaFreqConfig();
        config.setThreshold(2);
        config.setCheckTime(1000);
        config.setBanTime(2000);
        config.setDelayBanEnable(true);
        for (int i=0; i<20; i++) {
            CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.STANDALONE, config);
            System.out.println(response.isPass());
            TimeUnit.MILLISECONDS.sleep(200);
        }
        TimeUnit.SECONDS.sleep(3);
        CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.STANDALONE, config);
        System.out.println(response.isPass());
    }

    private static void test6() throws InterruptedException {
        CamelliaFreq freq = new CamelliaFreq(new CamelliaRedisTemplate("redis://@127.0.0.1:6379"));
        String freqKey = "k4";
        CamelliaFreqConfig config = new CamelliaFreqConfig();
        config.setThreshold(2);
        config.setCheckTime(1000);
        config.setBanTime(0);
        for (int i=0; i<20; i++) {
            CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.STANDALONE, config);
            System.out.println(response.isPass());
            TimeUnit.MILLISECONDS.sleep(200);
        }
        TimeUnit.SECONDS.sleep(3);
        CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.STANDALONE, config);
        System.out.println(response.isPass());
    }
}
