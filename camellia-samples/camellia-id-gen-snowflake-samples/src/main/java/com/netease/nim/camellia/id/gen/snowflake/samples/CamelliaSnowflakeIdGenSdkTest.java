package com.netease.nim.camellia.id.gen.snowflake.samples;

import com.netease.nim.camellia.id.gen.sdk.CamelliaIdGenSdkConfig;
import com.netease.nim.camellia.id.gen.sdk.CamelliaSnowflakeIdGenSdk;

import java.util.Date;

/**
 * Created by caojiajun on 2021/9/27
 */
public class CamelliaSnowflakeIdGenSdkTest {
    public static void main(String[] args) {
        CamelliaIdGenSdkConfig config = new CamelliaIdGenSdkConfig();
        config.setUrl("http://127.0.0.1:8081");
        config.setMaxRetry(5);//重试次数
        CamelliaSnowflakeIdGenSdk idGenSdk = new CamelliaSnowflakeIdGenSdk(config);
        long id = idGenSdk.genId();//生成id
        System.out.println(id);
        long ts = idGenSdk.decodeTs(id);//从id中解析出时间戳
        System.out.println(ts);
        System.out.println(new Date(ts));

        long target = 10*10000;
        int i = 0;
        long start = System.currentTimeMillis();
        while (true) {
            idGenSdk.genId();
            i++;
            if (i % 1000 == 0) {
                System.out.println("i=" + i);
            }
            if (i >= target) break;
        }
        long end = System.currentTimeMillis();
        System.out.println("QPS=" + (target / ((end - start)/1000.0)));
        //###idea里直接运行的简单测试结果：
        //QPS=5052.801778586226
    }
}
