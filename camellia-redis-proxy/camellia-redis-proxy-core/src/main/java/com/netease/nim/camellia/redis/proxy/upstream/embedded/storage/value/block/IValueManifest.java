package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block;

import java.io.IOException;

/**
 * Created by caojiajun on 2025/1/6
 */
public interface IValueManifest {

    /**
     * init and load
     * @throws IOException exception
     */
    void load() throws IOException;

    /**
     * allocate block
     * @param slot slot
     * @param blockType block type, 4k、32k、256k、1024k、no_align
     * @return block location
     * @throws IOException exception
     */
    BlockLocation allocate(short slot, BlockType blockType) throws IOException;

    /**
     * block type
     * @param fileId fileId
     * @return block type
     */
    BlockType blockType(long fileId) throws IOException;

    /**
     * block flush to disk
     * @param slot slot
     * @param location location
     * @throws IOException exception
     */
    void flush(short slot, BlockLocation location) throws IOException;


}
