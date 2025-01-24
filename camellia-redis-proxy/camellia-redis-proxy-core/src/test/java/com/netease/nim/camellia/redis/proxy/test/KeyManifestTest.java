package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.slot.KeyManifest;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.slot.SlotInfo;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by caojiajun on 2025/1/2
 */
public class KeyManifestTest {

    private final String dir = "/tmp";

    private static final int _64k = 64*1024;

    @Before
    public void before() {
        delete();
    }

    @Test
    public void test() throws IOException {

        KeyManifest keyManifest = new KeyManifest(dir);
        keyManifest.load();

        short slot = (short)ThreadLocalRandom.current().nextInt(RedisClusterCRC16Utils.SLOT_SIZE / 2);
        SlotInfo slotInfo1 = keyManifest.init(slot);
        SlotInfo slotInfo2 = keyManifest.init((short) (slot + 100));
        Assert.assertEquals(slotInfo1.fileId(), slotInfo2.fileId());
        Assert.assertEquals(slotInfo1.capacity(), _64k);
        Assert.assertEquals(slotInfo2.capacity(), _64k);
        Assert.assertEquals(slotInfo1.offset(), 0);
        Assert.assertEquals(slotInfo2.offset(), _64k);

        keyManifest.commit(slot, slotInfo1, null);
        keyManifest.commit((short) (slot + 100), slotInfo2, null);

        SlotInfo slotInfo = keyManifest.get(slot);
        Assert.assertEquals(slotInfo, slotInfo1);

        SlotInfo expand = keyManifest.expand(slot, slotInfo);
        Assert.assertEquals(expand.fileId(), slotInfo.fileId());
        Assert.assertEquals(expand.offset(), _64k * 2);
        Assert.assertEquals(expand.capacity(), _64k * 2);
        keyManifest.commit(slot, expand, null);

        slotInfo1 = expand;

        SlotInfo slotInfo3 = keyManifest.init((short) (slot + 200));
        Assert.assertEquals(slotInfo3, slotInfo);
        keyManifest.commit((short) (slot + 200), slotInfo3, null);

        KeyManifest keySlotMap2 = new KeyManifest(dir);
        keySlotMap2.load();

        SlotInfo slotInfo11 = keySlotMap2.get(slot);
        SlotInfo slotInfo22 = keySlotMap2.get((short) (slot + 100));
        SlotInfo slotInfo33 = keySlotMap2.get((short) (slot + 200));
        Assert.assertEquals(slotInfo1, slotInfo11);
        Assert.assertEquals(slotInfo2, slotInfo22);
        Assert.assertEquals(slotInfo3, slotInfo33);

        delete();
    }

    @Test
    public void test2() throws IOException {
        KeyManifest keyManifest = new KeyManifest(dir);
        keyManifest.load();

        long time1 = System.nanoTime();
        for (short i = 0; i<RedisClusterCRC16Utils.SLOT_SIZE; i++) {
            SlotInfo slotInfo = keyManifest.init(i);
            keyManifest.commit(i, slotInfo, null);
        }
        long time2 = System.nanoTime();
        System.out.println("init all, spend = " + (time2 - time1) / 1000000.0);

        for (int i=0; i<1000; i++) {
            short slot = (short) ThreadLocalRandom.current().nextInt(RedisClusterCRC16Utils.SLOT_SIZE);
            SlotInfo expand = keyManifest.expand(slot, keyManifest.get(slot));
            keyManifest.commit(slot, expand, null);
        }
        long time3 = System.nanoTime();
        System.out.println("rand expand 1000 slot, spend = " + (time3 - time2) / 1000000.0);

        delete();
    }

    @After
    public void after() {
        delete();
    }

    private void delete() {
        File file = new File(dir + "/key.manifest");
        if (file.exists()) {
            boolean delete = file.delete();
            System.out.println("delete " + delete);
        }
    }
}
