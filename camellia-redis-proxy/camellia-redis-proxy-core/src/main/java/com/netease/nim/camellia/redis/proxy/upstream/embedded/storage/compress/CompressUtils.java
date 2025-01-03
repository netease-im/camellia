package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.compress;

/**
 * Created by caojiajun on 2025/1/3
 */
public class CompressUtils {

    private static final NoneCompressor noneCompressor = new NoneCompressor();
    private static final ZstdCompressor zstdCompressor = new ZstdCompressor();

    public static ICompressor get(CompressType compressType) {
        if (compressType == CompressType.none) {
            return noneCompressor;
        } else if (compressType == CompressType.zstd) {
            return zstdCompressor;
        } else {
            throw new IllegalArgumentException("unknown compress type");
        }
    }

}
