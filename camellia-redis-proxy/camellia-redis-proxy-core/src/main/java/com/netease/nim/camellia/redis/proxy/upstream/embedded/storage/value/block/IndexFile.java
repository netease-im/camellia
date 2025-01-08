package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by caojiajun on 2025/1/8
 */
public record IndexFile(long fileId, BlockType blockType) {

    private static final Logger logger = LoggerFactory.getLogger(IndexFile.class);

    public static IndexFile parse(File file) {
        try {
            String name = file.getName();
            if (!name.endsWith(".index")) {
                return null;
            }
            String[] split = name.split("\\.")[0].split("_");
            long fileId = Long.parseLong(split[0]);
            int type = Integer.parseInt(split[1]);
            BlockType blockType = BlockType.getByValue(type);
            if (blockType == null) {
                logger.error("illegal block type = {} for file = {}", type, name);
                return null;
            }
            return new IndexFile(fileId, blockType);
        } catch (Exception e) {
            logger.error("parse index file name error, file = {}", file.getName());
            return null;
        }
    }
}
