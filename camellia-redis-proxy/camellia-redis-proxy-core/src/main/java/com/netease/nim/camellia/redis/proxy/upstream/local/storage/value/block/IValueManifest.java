package com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2025/1/6
 */
public interface IValueManifest {

    /**
     * get dir
     * @return dir
     */
    default String dir() {
        return null;
    }

    /**
     * init and load
     * @throws IOException exception
     */
    default void load() throws IOException {
    }

    /**
     * allocate block
     * @param slot slot
     * @param blockType block type, 4k、32k、256k、1024k、no_align
     * @return block location
     * @throws IOException exception
     */
    default BlockLocation allocate(short slot, BlockType blockType) throws IOException {
        return null;
    }

    /**
     * block type
     * @param fileId fileId
     * @return block type
     */
    default BlockType blockType(long fileId) {
        return null;
    }


    /**
     * flush block location to disk
     * @param slot slot
     * @param blockLocation location
     * @throws IOException exception
     */
    default void commit(short slot, BlockLocation blockLocation) throws IOException {

    }


    /**
     * recycle
     * @param slot slot
     * @param blockLocation location
     * @throws IOException exception
     */
    default void recycle(short slot, BlockLocation blockLocation) throws IOException {

    }

    /**
     * get block list of slot
     * @param slot slot
     * @param offset offset
     * @param limit limit
     * @return block list
     * @throws IOException exception
     */
    default List<BlockLocation> getBlocks(short slot, BlockType blockType, int offset, int limit) throws IOException {
        return new ArrayList<>();
    }
}
