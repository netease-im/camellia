package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block;

import java.io.IOException;

/**
 * Created by caojiajun on 2025/1/6
 */
public class ValueManifest implements IValueManifest {

    @Override
    public void load() throws IOException {

    }

    @Override
    public BlockLocation allocate(short slot, BlockType blockType) throws IOException {
        return null;
    }

    @Override
    public BlockType blockType(long fileId) throws IOException {
        return null;
    }

    @Override
    public void flush(short slot, BlockLocation location) throws IOException {

    }
}
