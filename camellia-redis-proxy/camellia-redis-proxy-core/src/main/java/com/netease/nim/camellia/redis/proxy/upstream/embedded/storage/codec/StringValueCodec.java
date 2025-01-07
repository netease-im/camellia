package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.codec;

import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.compress.CompressType;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.compress.CompressUtils;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block.*;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
import com.netease.nim.camellia.tools.utils.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.constants.EmbeddedStorageConstants.block_header_len;

/**
 * Created by caojiajun on 2025/1/6
 */
public class StringValueCodec {

    public static List<BlockInfo> encode(short slot, BlockType blockType, IValueManifest valueManifest, List<Pair<KeyInfo, byte[]>> values) throws IOException {
        //
        //data to sub-block
        List<SubBlock> subBlocks = new ArrayList<>();
        {
            SubBlock subBlock = new SubBlock();
            //new sub-block
            ByteBuffer decompressed = ByteBuffer.allocate(blockType.getBlockSize() - block_header_len);
            int size = 0;
            for (Pair<KeyInfo, byte[]> entry : values) {
                //
                byte[] data = entry.getSecond();
                int length = data.length;
                if (decompressed.remaining() < length + blockType.getSizeLen()) {
                    //sub-block compressed
                    CompressType compressType = CompressType.zstd;
                    byte[] compressed = CompressUtils.get(compressType).compress(decompressed.array(), 0, size);
                    if (compressed.length >= size) {
                        compressType = CompressType.none;
                        compressed = new byte[size];
                        System.arraycopy(decompressed.array(), 0, compressed, 0, compressed.length);
                    }
                    subBlock.compressType = compressType;//1
                    subBlock.decompressLen = size;//4
                    subBlock.compressLen = compressed.length;//4
                    subBlock.compressed = compressed;//n
                    subBlocks.add(subBlock);
                    //
                    //new sub-block
                    decompressed = ByteBuffer.allocate(blockType.getBlockSize() - block_header_len);
                    size = 0;
                    subBlock = new SubBlock();
                }
                subBlock.keyInfos.add(entry.getFirst());
                //add data to sub-block
                if (blockType == BlockType._4k || blockType == BlockType._32k) {
                    decompressed.putShort((short) length);
                } else {
                    decompressed.putInt(length);
                }
                decompressed.put(data);
                size += blockType.getSizeLen();
                size += length;
            }
            //sub-block compressed
            CompressType compressType = CompressType.zstd;
            byte[] compressed = CompressUtils.get(compressType).compress(decompressed.array(), 0, size);
            if (compressed.length >= size) {
                compressType = CompressType.none;
                compressed = new byte[size];
                System.arraycopy(decompressed.array(), 0, compressed, 0, compressed.length);
            }
            subBlock.compressType = compressType;//1
            subBlock.decompressLen = size;//4
            subBlock.compressLen = compressed.length;//4
            subBlock.compressed = compressed;//n
            subBlocks.add(subBlock);
        }

        //sub-block to block
        List<BlockInfo> blockInfos = new ArrayList<>();

        {
            //new block
            BlockLocation location = valueManifest.allocate(slot, blockType);
            ByteBuffer buffer = ByteBuffer.allocate(blockType.getBlockSize());
            int offset = 0;
            short subBlockCount = 0;
            buffer.putInt(0);//4
            buffer.putShort(subBlockCount);//2
            for (SubBlock block : subBlocks) {
                //
                if (buffer.remaining() < block.size()) {
                    //sub block merge
                    int crc = RedisClusterCRC16Utils.getCRC16(buffer.array(), 6, buffer.array().length);
                    buffer.putInt(0, crc);//4
                    buffer.putShort(4, subBlockCount);//2
                    blockInfos.add(new BlockInfo(blockType, location, buffer.array()));
                    //
                    //new block
                    location = valueManifest.allocate(slot, blockType);
                    buffer = ByteBuffer.allocate(blockType.getBlockSize());
                    subBlockCount = 0;
                    buffer.putInt(0);//4
                    buffer.putShort(subBlockCount);//2
                }
                //add sub-block to block
                buffer.put(block.compressType.getType());
                buffer.putInt(block.decompressLen);
                buffer.putInt(block.compressLen);
                buffer.put(block.compressed);
                for (KeyInfo keyInfo : block.keyInfos) {
                    keyInfo.setValueLocation(new ValueLocation(location, offset));
                    offset++;
                }
                subBlockCount++;
            }
            //sub block merge
            int crc = RedisClusterCRC16Utils.getCRC16(buffer.array(), 6, buffer.array().length);
            buffer.putInt(0, crc);
            buffer.putShort(4, subBlockCount);
            blockInfos.add(new BlockInfo(blockType, location, buffer.array()));
        }
        return blockInfos;
    }

    public static List<byte[]> decode(byte[] data, BlockType blockType) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int crc1 = buffer.getInt();
        int crc2 = RedisClusterCRC16Utils.getCRC16(data, 6, data.length);
        if (crc1 != crc2) {
            return new ArrayList<>();
        }

        List<byte[]> values = new ArrayList<>();

        short subBlockCount = buffer.getShort();
        for (int i=0; i<subBlockCount; i++) {
            CompressType compressType = CompressType.getByValue(buffer.get());
            int decompressLen = buffer.getInt();
            int compressLen = buffer.getInt();
            byte[] compressed = new byte[compressLen];
            buffer.get(compressed);
            byte[] decompressed = CompressUtils.get(compressType).decompress(compressed, 0, compressLen, decompressLen);
            ByteBuffer subBlockBuffer = ByteBuffer.wrap(decompressed);
            while (subBlockBuffer.hasRemaining()) {
                int size;
                if (blockType == BlockType._4k || blockType == BlockType._32k) {
                    size = subBlockBuffer.getShort();
                } else {
                    size = subBlockBuffer.getInt();
                }
                byte[] value = new byte[size];
                subBlockBuffer.get(value);
                values.add(value);
            }
        }
        return values;
    }

    private static class SubBlock {
        List<KeyInfo> keyInfos = new ArrayList<>();
        CompressType compressType;
        int decompressLen = 0;
        int compressLen = 0;
        byte[] compressed;

        int size() {
            return 1 + 4 + 4 + compressed.length;
        }
    }
}
