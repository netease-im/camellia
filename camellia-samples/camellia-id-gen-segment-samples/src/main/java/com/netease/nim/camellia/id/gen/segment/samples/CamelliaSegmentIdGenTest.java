package com.netease.nim.camellia.id.gen.segment.samples;

import com.netease.nim.camellia.id.gen.common.IDRange;
import com.netease.nim.camellia.id.gen.segment.CamelliaSegmentIdGen;
import com.netease.nim.camellia.id.gen.segment.CamelliaSegmentIdGenConfig;

import java.util.concurrent.atomic.AtomicLong;


/**
 * Created by caojiajun on 2021/9/24
 */
public class CamelliaSegmentIdGenTest {

    private static final AtomicLong id = new AtomicLong();

    public static void main(String[] args) throws Exception {
        CamelliaSegmentIdGenConfig config = new CamelliaSegmentIdGenConfig();
        config.setStep(20);
        config.setIdLoader((tag, step) -> {
            IDRange idRange = new IDRange(id.get() + 1, id.addAndGet(step));
            System.out.println("load [" + idRange.getStart() + "-" + idRange.getEnd() + "] in " + Thread.currentThread().getName());
            return idRange;
        });
        CamelliaSegmentIdGen idGen = new CamelliaSegmentIdGen(config);
        int i=2000;
        while (i -- > 0) {
            System.out.println(idGen.genIds("tag", 3));
//            Thread.sleep(1000);
            System.out.println(idGen.genId("tag"));
//            Thread.sleep(1000);
        }
    }
}
