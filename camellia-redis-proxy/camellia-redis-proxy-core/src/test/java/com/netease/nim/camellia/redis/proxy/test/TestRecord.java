package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants.LocalStorageConstants;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block.BlockLocation;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block.BlockType;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block.ValueLocation;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;


/**
 * Created by caojiajun on 2025/1/8
 */
public class TestRecord {

    @Test
    public void test() {
        int size = BlockType._4k.valueManifestSize(LocalStorageConstants.data_file_size);
        String s = Utils.humanReadableByteCountBin(size);//*.index
        System.out.println(s);

        int i = (int) (2 * LocalStorageConstants.data_file_size / 4096);
        String s1 = Utils.humanReadableByteCountBin(i);//*.slot
        System.out.println(s1);
    }

    @Test
    public void test1() {
        BlockLocation blockLocation1 = new BlockLocation(1L, 10);
        BlockLocation blockLocation2 = new BlockLocation(1L, 10);

        Assert.assertEquals(blockLocation1, blockLocation2);
        Assert.assertEquals(blockLocation1.hashCode(), blockLocation2.hashCode());

        ValueLocation valueLocation1 = new ValueLocation(blockLocation1, 1000);
        ValueLocation valueLocation2 = new ValueLocation(blockLocation2, 1000);

        Assert.assertEquals(valueLocation1, valueLocation2);
        Assert.assertEquals(valueLocation1.hashCode(), valueLocation2.hashCode());
    }

    @Test
    public void test2() {
        BlockLocation blockLocation1 = new BlockLocation(1L, 11);
        BlockLocation blockLocation2 = new BlockLocation(1L, 10);

        Assert.assertNotEquals(blockLocation1, blockLocation2);
        Assert.assertNotEquals(blockLocation1.hashCode(), blockLocation2.hashCode());

        ValueLocation valueLocation1 = new ValueLocation(blockLocation1, 1000);
        ValueLocation valueLocation2 = new ValueLocation(blockLocation2, 1000);

        Assert.assertNotEquals(valueLocation1, valueLocation2);
        Assert.assertNotEquals(valueLocation1.hashCode(), valueLocation2.hashCode());
    }

    @Test
    public void test3() {
        Key cacheKey1 = new Key("123".getBytes(StandardCharsets.UTF_8));
        Key cacheKey2 = new Key("123".getBytes(StandardCharsets.UTF_8));

        Assert.assertEquals(cacheKey1, cacheKey2);
        Assert.assertEquals(cacheKey1.hashCode(), cacheKey2.hashCode());
    }

    @Test
    public void test4() {
        Key cacheKey1 = new Key("123".getBytes(StandardCharsets.UTF_8));
        Key cacheKey2 = new Key("1235".getBytes(StandardCharsets.UTF_8));

        Assert.assertNotEquals(cacheKey1, cacheKey2);
        Assert.assertNotEquals(cacheKey1.hashCode(), cacheKey2.hashCode());
    }
}
