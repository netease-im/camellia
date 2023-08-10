package com.netease.nim.camellia.id.gen.strict.samples;

import com.netease.nim.camellia.id.gen.strict.CamelliaStrictIdGen2;
import com.netease.nim.camellia.id.gen.strict.CamelliaStrictIdGen2Config;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;

/**
 * Created by caojiajun on 2023/8/10
 */
public class CamelliaStrictIdGen2Test {

    public static void main(String[] args) {
        CamelliaStrictIdGen2Config config = new CamelliaStrictIdGen2Config();
        config.setSeqBits(11);//seq占11位，单个序列最多2048/秒
        config.setRedisTemplate(new CamelliaRedisTemplate("redis://@127.0.0.1:6379"));
        CamelliaStrictIdGen2 idGen = new CamelliaStrictIdGen2(config);
        int count = 30000;
        long start = System.currentTimeMillis();
        long lastId = 0;
        for (int i=0; i<count; i++) {
            long id = idGen.genId("test");
            if (i % 3000 == 0) {
                System.out.println("id=" + id);
            }
            if (id <= lastId) {
                System.out.println("error, id=" + id + ", lastId=" + lastId);
                System.out.println(Long.toBinaryString(id));
                System.out.println(Long.toBinaryString(lastId));
                System.exit(-1);
                return;
            }
            lastId = id;
//            System.out.println(id);
//            System.out.println(Long.toBinaryString(id));
        }
        long end = System.currentTimeMillis();
        System.out.println("QPS=" + (count / ((end - start)/1000.0)));
    }
}
