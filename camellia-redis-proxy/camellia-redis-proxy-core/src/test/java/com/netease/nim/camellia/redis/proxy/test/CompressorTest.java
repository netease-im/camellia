package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.compress.NoneCompressor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.compress.ZstdCompressor;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * Created by caojiajun on 2025/1/3
 */
public class CompressorTest {

    @Test
    public void testZstd1() {
        ZstdCompressor compressor = new ZstdCompressor();
        String data = "A".repeat(2000);
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        System.out.println(bytes.length);
        byte[] compressed = compressor.compress(bytes, 0, bytes.length);
        System.out.println(compressed.length);
        byte[] decompress = compressor.decompress(compressed, 0, compressed.length, bytes.length);
        Assert.assertEquals(new String(decompress, StandardCharsets.UTF_8), data);
    }

    @Test
    public void testZstd2() {
        ZstdCompressor compressor = new ZstdCompressor();
        String data = "A".repeat(2000);
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        System.out.println(bytes.length);
        byte[] compressed = compressor.compress(bytes, 10, bytes.length - 10);
        System.out.println(compressed.length);
        byte[] decompress = compressor.decompress(compressed, 0, compressed.length, bytes.length - 10);
        Assert.assertEquals(new String(decompress, StandardCharsets.UTF_8), "A".repeat(1990));
    }

    @Test
    public void testNone() {
        NoneCompressor compressor = new NoneCompressor();
        String data = "A".repeat(2000);
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        System.out.println(bytes.length);
        byte[] compressed = compressor.compress(bytes, 0, bytes.length);
        System.out.println(compressed.length);
        byte[] decompress = compressor.decompress(compressed, 0, compressed.length, bytes.length);
        Assert.assertEquals(new String(compressed, StandardCharsets.UTF_8), data);
        Assert.assertEquals(new String(decompress, StandardCharsets.UTF_8), data);
    }

}
