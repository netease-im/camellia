package com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants;

/**
 * Created by caojiajun on 2025/1/2
 */
public class LocalStorageConstants {

    public static final int key_max_len = 1024;

    public static final int _4k = 4*1024;
    public static final int _32k = 32*1024;
    public static final int _64k = 64*1024;
    public static final int _256k = 256*1024;
    public static final int _1024k = 1024*1024;
    public static final int _8m = 8*1024*1024;
    public static final long max_key_capacity = (long) Integer.MAX_VALUE * _4k;
    public static final int key_manifest_bit_size = (int) (32*1024*1024*1024L / _64k);//32Gib
    public static final long data_file_size = 256*1024*1024*1024L;//256Gib
    public static final long wal_file_size = 1024*1024*1024;//1Gib
    public static final int block_header_len = 4+4+2+1+4+4;

    public static byte[] _4k_empty_bytes = new byte[_4k];
    public static byte[] _8k_empty_bytes = new byte[_4k*2];
    public static byte[] _16k_empty_bytes = new byte[_4k*4];
    public static byte[] _32k_empty_bytes = new byte[_4k*8];
    public static byte[] _64k_empty_bytes = new byte[_4k*16];

    public static byte[] emptyBytes(int size) {
        if (size == _4k) {
            return _4k_empty_bytes;
        } else if (size == _4k*2) {
            return _8k_empty_bytes;
        } else if (size == _4k*4) {
            return _16k_empty_bytes;
        } else if (size == _4k*8) {
            return _32k_empty_bytes;
        } else if (size == _4k*16) {
            return _64k_empty_bytes;
        } else {
            return new byte[size];
        }
    }
}
