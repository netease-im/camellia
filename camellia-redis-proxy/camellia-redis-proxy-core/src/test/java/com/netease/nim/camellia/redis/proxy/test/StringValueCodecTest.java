package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.codec.StringValueCodec;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.enums.DataType;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block.*;
import com.netease.nim.camellia.tools.utils.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by caojiajun on 2025/1/7
 */
public class StringValueCodecTest {

    private static final long fileId = System.currentTimeMillis();


    @Test
    public void test() throws IOException {
        short slot = 1;
        BlockType blockType = BlockType._4k;
        IValueManifest valueManifest = new MockValueManifest();

        List<Pair<KeyInfo, byte[]>> values = new ArrayList<>();

        values.add(new Pair<>(keyInfo("k1", 1000L, null, null), UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
        values.add(new Pair<>(keyInfo("k2", 1000L, null, null), UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
        values.add(new Pair<>(keyInfo("k3", 1000L, null, null), UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
        values.add(new Pair<>(keyInfo("k4", 1000L, null, null), "111".getBytes(StandardCharsets.UTF_8)));
        values.add(new Pair<>(keyInfo("k5", 1000L, null, null), "hhhhh".getBytes(StandardCharsets.UTF_8)));
        values.add(new Pair<>(keyInfo("k6", 1000L, null, null), "1k12l12121".getBytes(StandardCharsets.UTF_8)));
        values.add(new Pair<>(keyInfo("k7", 1000L, null, null), "sasasas".getBytes(StandardCharsets.UTF_8)));

        List<BlockInfo> blockInfos = StringValueCodec.encode(slot, blockType, valueManifest, values);

        Assert.assertEquals(blockInfos.size(), 1);

        BlockInfo blockInfo = blockInfos.getFirst();
        List<byte[]> list = StringValueCodec.decode(blockInfo.data(), blockType);

        Assert.assertEquals(list.size(), values.size());

        for (int i=0; i<list.size(); i++) {
            Assert.assertEquals(new String(list.get(i), StandardCharsets.UTF_8), new String(values.get(i).getSecond(), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void test2() throws IOException {
        short slot = 1;
        BlockType blockType = BlockType._4k;
        IValueManifest valueManifest = new MockValueManifest();

        List<Pair<KeyInfo, byte[]>> values = new ArrayList<>();

        for (int i=0; i<10000; i++) {
            values.add(new Pair<>(keyInfo("k" + i, 1000L, null, null), UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
        }

        List<BlockInfo> blockInfos = StringValueCodec.encode(slot, blockType, valueManifest, values);

        for (int i=0;i<blockInfos.size(); i++) {
            BlockInfo blockInfo = blockInfos.get(i);
            int blockId = blockInfo.blockLocation().blockId();
            Assert.assertEquals(blockId, i);
        }

        System.out.println(blockInfos.size());

        List<byte[]> result = new ArrayList<>();
        for (BlockInfo blockInfo : blockInfos) {
            List<byte[]> list = StringValueCodec.decode(blockInfo.data(), blockType);
            result.addAll(list);
        }

        Assert.assertEquals(result.size(), values.size());

        for (int i=0; i<result.size(); i++) {
            Assert.assertEquals(new String(result.get(i), StandardCharsets.UTF_8), new String(values.get(i).getSecond(), StandardCharsets.UTF_8));
        }
    }

    private KeyInfo keyInfo(String key, long expireTime, ValueLocation location, String extra) {
        KeyInfo keyInfo = new KeyInfo();
        keyInfo.setDataType(DataType.string);
        keyInfo.setKey(key.getBytes(StandardCharsets.UTF_8));
        keyInfo.setExpireTime(expireTime);
        keyInfo.setValueLocation(location);
        if (extra != null) {
            keyInfo.setExtra(extra.getBytes(StandardCharsets.UTF_8));
        }
        return keyInfo;
    }

    public static class MockValueManifest implements IValueManifest {

        private final AtomicInteger blockId = new AtomicInteger(0);

        @Override
        public void load() throws IOException {

        }

        @Override
        public BlockLocation allocate(short slot, BlockType blockType) throws IOException {
            return new BlockLocation(fileId, blockId.getAndIncrement());
        }

        @Override
        public BlockType blockType(long fileId) throws IOException {
            return null;
        }

        @Override
        public void commit(short slot, BlockLocation blockLocation) throws IOException {

        }

        @Override
        public void clear(short slot, BlockLocation blockLocation) throws IOException {

        }
    }
}
