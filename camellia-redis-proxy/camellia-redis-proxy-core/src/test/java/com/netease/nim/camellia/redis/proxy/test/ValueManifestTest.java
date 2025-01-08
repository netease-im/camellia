package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block.BlockLocation;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block.BlockType;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block.ValueManifest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Created by caojiajun on 2025/1/8
 */
public class ValueManifestTest {

    private static final String dir = "/tmp";

    @Before
    public void before() {
        clearIndexFiles();
    }

    @Test
    public void test() throws IOException {
        {
            ValueManifest valueManifest = new ValueManifest(dir);
            valueManifest.load();

            short slot = 1;

            BlockLocation location1 = valueManifest.allocate(slot, BlockType._4k);
            Assert.assertEquals(0, location1.blockId());

            BlockLocation location2 = valueManifest.allocate(slot, BlockType._4k);
            Assert.assertEquals(1, location2.blockId());

            valueManifest.commit(slot, location1);
            valueManifest.commit(slot, location2);
        }

        {
            ValueManifest valueManifest = new ValueManifest(dir);
            valueManifest.load();

            short slot = 1;

            BlockLocation location1 = valueManifest.allocate(slot, BlockType._4k);
            Assert.assertEquals(2, location1.blockId());

            BlockLocation location2 = valueManifest.allocate(slot, BlockType._4k);
            Assert.assertEquals(3, location2.blockId());

            valueManifest.commit(slot, location1);
            valueManifest.commit(slot, location2);
        }
    }

    @After
    public void after() {
        clearIndexFiles();
    }

    private void clearIndexFiles() {
        File file = new File(dir);
        File[] files = file.listFiles();
        if (files == null) return;
        for (File file1 : files) {
            if (file1.getName().endsWith(".index") || file1.getName().endsWith(".data") || file1.getName().endsWith(".slot")) {
                boolean delete = file1.delete();
                System.out.println("delete " + file1.getName() + ", result = " + delete);
            }
        }
    }
}
