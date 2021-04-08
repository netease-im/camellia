package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;

/**
 * Created by caojiajun on 2021/4/8
 */
public class TestRedisSentinelSlaves {

    public static void main(String[] args) throws InterruptedException {
        String slaves = "redis-sentinel-slaves://@127.0.0.1:26379/master1?withMaster=true";
        String master = "redis-sentinel://@127.0.0.1:26379/master1";

        ResourceTable resourceTable = ResourceTableUtil.simpleRwSeparateTable(new Resource(slaves), new Resource(master));
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(resourceTable);
        int i = 0;
        while (true) {
            try {
                System.out.println(template.set("k1", "v" + (i++)));
                System.out.println(template.get("k1"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            Thread.sleep(5000);
        }
    }
}
