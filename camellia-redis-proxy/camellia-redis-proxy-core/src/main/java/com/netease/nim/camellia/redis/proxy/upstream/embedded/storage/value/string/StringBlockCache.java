package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.string;

import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.codec.StringValueCodec;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.file.FileReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block.BlockLocation;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block.BlockType;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block.IValueManifest;

import java.io.IOException;
import java.util.List;

/**
 * Created by caojiajun on 2025/1/6
 */
public class StringBlockCache {

    private IValueManifest valueManifest;
    private FileReadWrite fileReadWrite;

    public byte[] get(short slot, KeyInfo keyInfo) throws IOException {
        BlockLocation blockLocation = keyInfo.getValueLocation().blockLocation();
        BlockType blockType = valueManifest.blockType(blockLocation.fileId());
        long fileOffset = (long) blockType.getBlockSize() * blockType.getBlockSize();
        byte[] data = fileReadWrite.read(blockLocation.fileId(), fileOffset, blockType.getBlockSize());
        List<byte[]> list = StringValueCodec.decode(data, blockType);
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() > keyInfo.getValueLocation().offset()) {
            return null;
        }
        return list.get(keyInfo.getValueLocation().offset());
    }

    public void updateBlockCache(short slot, long fileId, long offset, byte[] block) {

    }
}
