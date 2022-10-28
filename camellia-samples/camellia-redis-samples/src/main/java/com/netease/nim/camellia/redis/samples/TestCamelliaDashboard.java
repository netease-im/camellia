package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.CamelliaRedisTemplateManager;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2021/4/20
 */
public class TestCamelliaDashboard {

    public static void test() {
        String dashboardUrl = "http://127.0.0.1:8080";//dashboard地址
        Map<String, String> headerMap= new HashMap<>();
        headerMap.put("api-key", "secureToken"); // api-key of camellia dashboard
        long bid = 1;
        String bgroup = "default";
        boolean monitorEnable = true;//是否上报监控数据到dashboard
        long checkIntervalMillis = 5000;//检查resourceTable的间隔

        CamelliaRedisEnv redisEnv = CamelliaRedisEnv.defaultRedisEnv();

        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, dashboardUrl, bid, bgroup, monitorEnable, checkIntervalMillis, headerMap);
        String k1 = template.get("k1");
        System.out.println(k1);

        //如果要同时管理多组bid/bgroup，你可以使用CamelliaRedisTemplateManager
        CamelliaRedisTemplateManager manager = new CamelliaRedisTemplateManager(dashboardUrl, headerMap);
        CamelliaRedisTemplate redisTemplate1 = manager.getRedisTemplate(1L, "default");
        String k2 = redisTemplate1.get("k2");
        System.out.println(k2);
        CamelliaRedisTemplate redisTemplate2 = manager.getRedisTemplate(2L, "default");
        String k3 = redisTemplate2.get("k3");
        System.out.println(k3);
    }

    public static void main(String[] args) {
        test();
    }
}
