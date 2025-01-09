package com.netease.nim.camellia.redis.proxy.upstream.local.storage.compress;

/**
 * Created by caojiajun on 2025/1/3
 */
public class NoneCompressor implements ICompressor {

    @Override
    public byte[] compress(byte[] data, int offset, int len) {
        if (data == null) {
            return null;
        }
        if (offset == 0 && len == data.length) {
            return data;
        }
        byte[] result = new byte[len];
        System.arraycopy(data, offset, result, 0, result.length);
        return result;
    }

    @Override
    public byte[] decompress(byte[] data, int offset, int len, int decompressLen) {
        if (data == null) {
            return null;
        }
        if (len != decompressLen) {
            throw new IllegalArgumentException("none compress len/decompressLen not equals");
        }
        if (offset == 0 && len == data.length) {
            return data;
        }
        byte[] result = new byte[len];
        System.arraycopy(data, offset, result, 0, result.length);
        return result;
    }
}
