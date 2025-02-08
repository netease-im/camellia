package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.slot.KeyManifest;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.slot.SlotInfo;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

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

        short slot1 = (short)ThreadLocalRandom.current().nextInt(RedisClusterCRC16Utils.SLOT_SIZE / 2);
        short slot2 = (short) (slot1 + 100);

        {
            SlotInfo slotInfo1 = keyManifest.init(slot1);
            Assert.assertEquals(slotInfo1.offset(), 0);
            Assert.assertEquals(slotInfo1.capacity(), _64k);

            SlotInfo slotInfo2 = keyManifest.init(slot2);
            Assert.assertEquals(slotInfo2.offset(), _64k);
            Assert.assertEquals(slotInfo2.capacity(), _64k);

            keyManifest.commit(slot1, slotInfo1, null);
            keyManifest.commit(slot2, slotInfo2, null);
        }

        {
            SlotInfo slotInfo1 = keyManifest.get(slot1);
            Assert.assertEquals(slotInfo1.offset(), 0);
            Assert.assertEquals(slotInfo1.capacity(), _64k);
            SlotInfo expandSlotInfo1 = keyManifest.expand(slot1, slotInfo1);
            Assert.assertEquals(expandSlotInfo1.offset(), _64k*2);
            Assert.assertEquals(expandSlotInfo1.capacity(), _64k*2);
        }

        {
            SlotInfo slotInfo2 = keyManifest.get(slot2);
            Assert.assertEquals(slotInfo2.offset(), _64k);
            Assert.assertEquals(slotInfo2.capacity(), _64k);
            SlotInfo expandSlotInfo2 = keyManifest.expand(slot1, slotInfo2);
            Assert.assertEquals(expandSlotInfo2.offset(), _64k*4);
            Assert.assertEquals(expandSlotInfo2.capacity(), _64k*2);

            SlotInfo expandSlotInfo22 = keyManifest.expand(slot2, expandSlotInfo2);
            Assert.assertEquals(expandSlotInfo22.offset(), _64k*6);
            Assert.assertEquals(expandSlotInfo22.capacity(), _64k*4);

            keyManifest.commit(slot2, expandSlotInfo22, Collections.singleton(expandSlotInfo2));
        }
        delete();
    }

    @Test
    public void test2() throws IOException, InterruptedException {
        KeyManifest keyManifest = new KeyManifest(dir);
        keyManifest.load();

        int count = 0;

        for (int i=0; i<RedisClusterCRC16Utils.SLOT_SIZE; i++) {
            short slot = (short) i;
            SlotInfo slotInfo = keyManifest.init(slot);
            Assert.assertEquals(slotInfo.offset(), i*_64k);
            Assert.assertEquals(slotInfo.capacity(), _64k);
            keyManifest.commit(slot, slotInfo, null);
            count ++;
        }
        long fileId = keyManifest.get((short) 0).fileId();
        {
            SlotInfo slotInfo = keyManifest.get((short) 0);
            System.out.println(slotInfo.fileId() + "," + slotInfo.offset() + "," + slotInfo.capacity());
        }
        {
            SlotInfo slotInfo = keyManifest.get((short) 100);
            System.out.println(slotInfo.fileId() + "," + slotInfo.offset() + "," + slotInfo.capacity());
        }
        {
            SlotInfo slotInfo = keyManifest.get((short) 1000);
            System.out.println(slotInfo.fileId() + "," + slotInfo.offset() + "," + slotInfo.capacity());
        }

        for (int loop=0; loop < 8; loop ++) {
            for (int i = 0; i < RedisClusterCRC16Utils.SLOT_SIZE; i++) {
                short slot = (short) i;
                SlotInfo slotInfo = keyManifest.get(slot);
                SlotInfo expand = keyManifest.expand(slot, slotInfo);
                Assert.assertEquals(slotInfo.capacity() * 2, expand.capacity());
                keyManifest.commit(slot, expand, null);
                count ++;
                if (expand.fileId() != fileId) {
                    System.out.println("xxx" + ",fileId=" + expand.fileId() + ",count=" + count + ",capacity=" + expand.capacity());
                    fileId = expand.fileId();
                    count = 0;
                }
            }

            {
                SlotInfo slotInfo = keyManifest.get((short) 0);
                System.out.println(slotInfo.fileId() + "," + slotInfo.offset() + "," + slotInfo.capacity());
            }
            {
                SlotInfo slotInfo = keyManifest.get((short) 100);
                System.out.println(slotInfo.fileId() + "," + slotInfo.offset() + "," + slotInfo.capacity());
            }
            {
                SlotInfo slotInfo = keyManifest.get((short) 1000);
                System.out.println(slotInfo.fileId() + "," + slotInfo.offset() + "," + slotInfo.capacity());
            }
            TimeUnit.SECONDS.sleep(1);
        }
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
