package com.netease.nim.camellia.redis.proxy.upstream.local.storage.file;


import java.io.IOException;

/**
 * Created by caojiajun on 2025/1/9
 */
public interface IBlockReadWrite {

    /**
     * update block cache
     * @param fileId fileId
     * @param offset offset
     * @param block block
     */
    void updateBlockCache(long fileId, long offset, byte[] block);

    /**
     * write one or more blocks
     * @param fileId fileId
     * @param offset offset
     * @param data data
     * @throws IOException exception
     */
    void writeBlocks(long fileId, long offset, byte[] data) throws IOException;

    /**
     * read one or more blocks
     * @param fileId fileId
     * @param offset offset
     * @param size size
     * @return data
     @throws IOException exception
     */
    byte[] readBlocks(long fileId, long offset, int size) throws IOException;
}
