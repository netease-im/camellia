package com.netease.nim.camellia.id.gen.snowflake.samples;

import com.netease.nim.camellia.id.gen.snowflake.CamelliaSnowflakeConfig;
import com.netease.nim.camellia.id.gen.snowflake.CamelliaSnowflakeIdGen;
import com.netease.nim.camellia.id.gen.snowflake.RedisWorkerIdGen;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;

/**
 * Created by caojiajun on 2021/9/18
 */
public class CamelliaSnowflakeIdGenTest {

    public static void main(String[] args) throws Exception {
        CamelliaSnowflakeConfig config = new CamelliaSnowflakeConfig();
        config.setRegionBits(0);
        config.setWorkerIdBits(10);
        config.setSequenceBits(2);
        config.setWorkerIdGen(new RedisWorkerIdGen(new CamelliaRedisTemplate("redis://@127.0.0.1:6379")));
        config.setRegionId(0);
        CamelliaSnowflakeIdGen idGen = new CamelliaSnowflakeIdGen(config);

        int i=2000;
        while (i -- > 0) {
            long id = idGen.genId();
            System.out.println(id);
            System.out.println(Long.toBinaryString(id));
            System.out.println(Long.toBinaryString(id).length());
//            TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextInt(3));
        }
    }
}
