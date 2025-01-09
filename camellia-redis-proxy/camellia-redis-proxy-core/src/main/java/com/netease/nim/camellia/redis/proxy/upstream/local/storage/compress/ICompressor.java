package com.netease.nim.camellia.redis.proxy.upstream.local.storage.compress;

/**
 * Created by caojiajun on 2025/1/3
 */
public interface ICompressor {

    byte[] compress(byte[] data, int offset, int len);

    byte[] decompress(byte[] data, int offset, int len, int decompressLen);
}
