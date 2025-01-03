package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.compress;


import com.github.luben.zstd.Zstd;

/**
 * Created by caojiajun on 2025/1/3
 */
public class ZstdCompressor implements ICompressor {

    private final int compressionLevel;

    public ZstdCompressor() {
        compressionLevel = Zstd.maxCompressionLevel();
    }

    @Override
    public byte[] compress(byte[] data, int offset, int len) {
        if (data == null) {
            return null;
        }
        int maxSize = (int) Zstd.compressBound(len);
        byte[] compressedData = new byte[maxSize];
        int compressedSize = (int) Zstd.compressByteArray(compressedData, 0, compressedData.length, data, offset, len, compressionLevel);
        byte[] result = new byte[compressedSize];
        System.arraycopy(compressedData, 0, result, 0, compressedSize);
        return result;
    }

    @Override
    public byte[] decompress(byte[] data, int offset, int len, int decompressLen) {
        byte[] output = new byte[decompressLen];
        Zstd.decompressByteArray(output, 0, output.length, data, offset, len);
        return output;
    }
}
