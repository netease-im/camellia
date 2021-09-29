package com.netease.nim.camellia.id.gen.segment.samples;

import com.netease.nim.camellia.id.gen.sdk.CamelliaIdGenSdkConfig;
import com.netease.nim.camellia.id.gen.sdk.CamelliaSegmentIdGenSdk;

import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2021/9/27
 */
public class CamelliaSegmentIdGenSdkTest {
    public static void main(String[] args) {
        CamelliaIdGenSdkConfig config = new CamelliaIdGenSdkConfig();
        config.setUrl("http://127.0.0.1:8083");
        config.setMaxRetry(2);//重试次数
        config.getSegmentIdGenSdkConfig().setCacheEnable(true);//表示sdk是否缓存id
        config.getSegmentIdGenSdkConfig().setStep(200);//sdk缓存的id数
        CamelliaSegmentIdGenSdk idGenSdk = new CamelliaSegmentIdGenSdk(config);

        System.out.println(idGenSdk.genId("a"));
        System.out.println(idGenSdk.genIds("a", 3));

        long target = 10*10000;
        int i = 0;
        long start = System.currentTimeMillis();
        while (true) {
            idGenSdk.genId("a");
            i++;
            if (i % 1000 == 0) {
                System.out.println("i=" + i);
            }
            if (i >= target) break;
        }
        long end = System.currentTimeMillis();
        System.out.println("QPS=" + (target / ((end - start)/1000.0)));
        //###idea里直接运行的简单测试结果：
        //开启sdk缓存并设置缓存step=200：
        //QPS=17286.084701815038
        //开启sdk缓存并设置缓存step=100：
        //QPS=8647.526807333103
        //不开启sdk缓存
        //QPS=4657.878801993572
    }
}
