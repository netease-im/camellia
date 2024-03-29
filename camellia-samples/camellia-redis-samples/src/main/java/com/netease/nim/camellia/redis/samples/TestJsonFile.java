package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.core.api.ReloadableLocalFileCamelliaApi;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;

import java.net.URL;

/**
 *
 * Created by caojiajun on 2021/4/20
 */
public class TestJsonFile {

    public static void test() {
        String fileName = "resource-table.json";//文件可以是json，也可以是单个的redis地址
//        String fileName = "simple.conf";
        URL resource = TestJsonFile.class.getClassLoader().getResource(fileName);
        if (resource == null) {
            System.out.println(fileName + " not exists");
            return;
        }
        ReloadableLocalFileCamelliaApi localFileCamelliaApi = new ReloadableLocalFileCamelliaApi(resource.getPath());

        CamelliaRedisEnv redisEnv = CamelliaRedisEnv.defaultRedisEnv();
        long checkIntervalMillis = 5000;//检查文件是否产生变更的检查周期，单位ms
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, localFileCamelliaApi, checkIntervalMillis);

        String k1 = template.get("k1");
        System.out.println(k1);
    }

    public static void main(String[] args) {
        test();
    }
}
