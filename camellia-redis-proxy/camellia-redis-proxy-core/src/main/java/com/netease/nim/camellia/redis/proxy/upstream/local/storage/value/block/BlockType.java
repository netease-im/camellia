package com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants.EmbeddedStorageConstants;


/**
 * Created by caojiajun on 2025/1/6
 */
public enum BlockType {
    _4k(1, 4*1024, 2),
    _32k(2, 32*1024, 2),
    _256k(3, 256*1024, 4),
    _1024k(4, 1024*1024, 4),
    ;

    private final int type;
    private final int blockSize;
    private final int sizeLen;

    BlockType(int type, int blockSize, int sizeLen) {
        this.type = type;
        this.blockSize = blockSize;
        this.sizeLen = sizeLen;
    }

    public int getType() {
        return type;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public int getSizeLen() {
        return sizeLen;
    }

    public static BlockType getByValue(int type) {
        for (BlockType blockType : BlockType.values()) {
            if (blockType.getType() == type) {
                return blockType;
            }
        }
        return null;
    }

    public static BlockType fromData(byte[] data) {
        if (data.length + 4 + 2 < EmbeddedStorageConstants._4k) {
            return BlockType._4k;
        } else if (data.length + 4 + 2 < EmbeddedStorageConstants._32k) {
            return BlockType._32k;
        } else if (data.length + 4 + 4 < EmbeddedStorageConstants._256k) {
            return BlockType._256k;
        } else if (data.length + 4 + 4 < EmbeddedStorageConstants._1024k) {
            return BlockType._1024k;
        } else {
            throw new IllegalArgumentException("data too long");
        }
    }

    public int valueManifestSize(long dataFileCapacity) {
        int blockCount = (int) (dataFileCapacity / getBlockSize());
        return blockCount / 8;
    }

    public int valueSlotManifestSize(long dataFileCapacity) {
        int blockCount = (int) (dataFileCapacity / getBlockSize());
        return blockCount * 2;
    }
}
