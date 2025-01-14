package com.netease.nim.camellia.redis.proxy.test;

import org.junit.Assert;
import org.junit.Test;

import java.util.BitSet;

import static com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants.LocalStorageConstants._4k;
import static com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants.LocalStorageConstants.data_file_size;

/**
 * Created by caojiajun on 2025/1/14
 */
public class BitSetTest {

    @Test
    public void test() {
        BitSet bitSet = new BitSet((int) (data_file_size / _4k));
        for (int i=0; i<100; i++) {
            bitSet.set(i, true);
        }
        for (int i=1000; i<2000; i++) {
            bitSet.set(i, true);
        }

        long[] longArray = bitSet.toLongArray();

        BitSet bitSet1 = BitSet.valueOf(longArray);
        for (int i=0; i<100; i++) {
            Assert.assertTrue(bitSet1.get(i));
        }
        for (int i=200; i<900; i++) {
            Assert.assertFalse(bitSet1.get(i));
        }
        for (int i=1000; i<2000; i++) {
            Assert.assertTrue(bitSet1.get(i));
        }
        for (int i=3000; i<5000; i++) {
            Assert.assertFalse(bitSet1.get(i));
        }

        for (int i=6000; i<7000; i++) {
            bitSet1.set(i, true);
        }

        long[] longArray1 = bitSet1.toLongArray();
        BitSet bitSet2 = BitSet.valueOf(longArray1);

        for (int i=0; i<100; i++) {
            Assert.assertTrue(bitSet2.get(i));
        }
        for (int i=200; i<900; i++) {
            Assert.assertFalse(bitSet2.get(i));
        }
        for (int i=1000; i<2000; i++) {
            Assert.assertTrue(bitSet2.get(i));
        }
        for (int i=3000; i<5000; i++) {
            Assert.assertFalse(bitSet2.get(i));
        }
        for (int i=6000; i<7000; i++) {
            Assert.assertTrue(bitSet2.get(i));
        }

    }
}
