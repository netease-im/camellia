package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.constants.EmbeddedStorageConstants;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block.BlockType;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.junit.Test;

/**
 * Created by caojiajun on 2025/1/8
 */
public class TestFileSize {

    @Test
    public void test() {
        int size = BlockType._4k.valueManifestSize(EmbeddedStorageConstants.data_file_size);
        String s = Utils.humanReadableByteCountBin(size);//*.index
        System.out.println(s);

        int i = (int) (2 * EmbeddedStorageConstants.data_file_size / 4096);
        String s1 = Utils.humanReadableByteCountBin(i);//*.slot
        System.out.println(s1);
    }
}
