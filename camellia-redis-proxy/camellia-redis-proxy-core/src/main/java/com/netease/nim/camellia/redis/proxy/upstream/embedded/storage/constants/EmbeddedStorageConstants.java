package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.constants;

/**
 * Created by caojiajun on 2025/1/2
 */
public class EmbeddedStorageConstants {

    public static final int _4k = 4*1024;
    public static final int _64k = 64*1024;
    public static final int bit_size = (int)(16*1024*1024*1024L / _64k);//16Gib
}
