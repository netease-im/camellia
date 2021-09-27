package com.netease.nim.camellia.id.gen.strict.samples;

import com.netease.nim.camellia.id.gen.common.IDRange;
import com.netease.nim.camellia.id.gen.strict.CamelliaStrictIdGen;
import com.netease.nim.camellia.id.gen.strict.CamelliaStrictIdGenConfig;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2021/9/24
 */
public class CamelliaStrictIdGenTest {

    private static final AtomicLong id = new AtomicLong();

    public static void main(String[] args) {
        CamelliaStrictIdGenConfig config = new CamelliaStrictIdGenConfig();
        config.setTemplate(new CamelliaRedisTemplate("redis://@127.0.0.1:6379"));
        config.setMaxStep(100);
        config.setIdLoader((tag, step) -> {
            IDRange idRange = new IDRange(id.get() + 1, id.addAndGet(step));
            System.out.println("load [" + idRange.getStart() + "," + idRange.getEnd() + "] in " + Thread.currentThread().getName());
            return idRange;
        });

        CamelliaStrictIdGen idGen = new CamelliaStrictIdGen(config);
        int i=2000;
        while (i -- > 0) {
            System.out.println("peek, id = " + idGen.peekId("tag"));
            System.out.println("get, id = " + idGen.genId("tag"));
        }
    }
}
