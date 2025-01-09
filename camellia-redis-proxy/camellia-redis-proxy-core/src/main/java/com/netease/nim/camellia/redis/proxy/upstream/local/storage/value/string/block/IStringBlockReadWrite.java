package com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string.block;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.file.IBlockReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block.BlockType;

import java.io.IOException;

/**
 * Created by caojiajun on 2025/1/9
 */
public interface IStringBlockReadWrite extends IBlockReadWrite {

    /**
     * get value
     * @param keyInfo keyInfo
     * @return value
     * @throws IOException exception
     */
    byte[] get(KeyInfo keyInfo) throws IOException;

    /**
     * get block
     * @param blockType block type
     * @param fileId fileId
     * @param offset offset
     * @return block
     */
    byte[] getBlock(BlockType blockType, long fileId, long offset) throws IOException;

}
