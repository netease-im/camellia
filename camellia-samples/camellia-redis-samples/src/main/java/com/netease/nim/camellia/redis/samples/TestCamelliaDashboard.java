package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;

/**
 *
 * Created by caojiajun on 2021/4/20
 */
public class TestCamelliaDashboard {

    public static void test() {
        String dashboardUrl = "http://127.0.0.1:8080";//dashboard地址
        long bid = 1;
        String bgroup = "default";
        boolean monitorEnable = true;//是否上报监控数据到dashboard
        long checkIntervalMillis = 5000;//检查resourceTable的间隔

        CamelliaRedisEnv redisEnv = CamelliaRedisEnv.defaultRedisEnv();

        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, dashboardUrl, bid, bgroup, monitorEnable, checkIntervalMillis);
        String k1 = template.get("k1");
        System.out.println(k1);
    }

    public static void main(String[] args) {
        test();
    }
}
