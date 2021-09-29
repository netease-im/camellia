package com.netease.nim.camellia.id.gen.snowflake.samples;

import com.netease.nim.camellia.id.gen.snowflake.CamelliaSnowflakeConfig;
import com.netease.nim.camellia.id.gen.snowflake.CamelliaSnowflakeIdGen;
import com.netease.nim.camellia.id.gen.snowflake.RedisWorkerIdGen;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;

/**
 * Created by caojiajun on 2021/9/18
 */
public class CamelliaSnowflakeIdGenTest {

    public static void main(String[] args) {
        CamelliaSnowflakeConfig config = new CamelliaSnowflakeConfig();
        config.setRegionBits(0);//单元id所占的比特位数，0表示不区分单元
        config.setRegionId(0);//regionId，如果regionBits为0，则regionId必须为0
        config.setWorkerIdBits(10);//workerId所占的比特位数
        config.setSequenceBits(12);//序列号所占比特位数
        //使用redis生成workerId
        config.setWorkerIdGen(new RedisWorkerIdGen(new CamelliaRedisTemplate("redis://@127.0.0.1:6379")));

        CamelliaSnowflakeIdGen idGen = new CamelliaSnowflakeIdGen(config);

        int i=2000;
        while (i -- > 0) {
            long id = idGen.genId();
            System.out.println(id);
            System.out.println(Long.toBinaryString(id));
            System.out.println(Long.toBinaryString(id).length());
        }

        long target = 1000*10000;
        int j = 0;
        long start = System.currentTimeMillis();
        while (true) {
            idGen.genId();
            j++;
            if (j % 100000 == 0) {
                System.out.println("i=" + j);
            }
            if (j >= target) break;
        }
        long end = System.currentTimeMillis();
        System.out.println("QPS=" + (target / ((end - start)/1000.0)));
        //QPS=4061738.424045491
    }
}
