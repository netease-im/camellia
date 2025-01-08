package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block;

import java.io.File;

/**
 * Created by caojiajun on 2025/1/8
 */
public record IndexFile(long fileId, BlockType blockType) {

    public static IndexFile parse(File file) {
        String name = file.getName();
        if (!name.endsWith(".index")) {
            return null;
        }
        String[] split = name.split("\\.")[0].split("_");
        long fileId = Long.parseLong(split[0]);
        int type = Integer.parseInt(split[1]);
        BlockType blockType = BlockType.getByValue(type);
        return new IndexFile(fileId, blockType);
    }
}
