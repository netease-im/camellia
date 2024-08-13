package com.netease.nim.camellia.redis.proxy.upstream.kv.meta;

/**
 * ######说明######
 * redis做storage时，不可以驱逐；做缓存时，可以驱逐
 * <p>
 * ######数据结构######
 * string
 * v0 只有key-meta，value作为key-meta的extra部分
 * <p>
 * hash
 * v0 key-meta里有field-count
 * v1 key-meta里没有field-count
 * <p>
 * zset
 * v0 kv-client底层有2个sub-key，一个是member-score，一个是score+member-null
 * v1 redis同时做storage和缓存，kv-client底层有1个sub-key，就是index-member，redis-key有2个，storage是zset_with_index，cache是index
 * <p>
 * set
 * v0 key-meta里有member-size
 * v1 key-meta里没有member-size
 * <p>
 * Created by caojiajun on 2024/4/11
 */
public enum EncodeVersion {

    version_0((byte) 0),
    version_1((byte) 1),
    version_2((byte) 2),
    version_3((byte) 3),
    version_4((byte) 4),
    version_5((byte) 5),
    version_6((byte) 6),
    version_7((byte) 7),
    version_8((byte) 8),
    version_9((byte) 9),
    version_10((byte) 10),
    ;

    private final byte value;

    EncodeVersion(byte value) {
        this.value = value;
    }

    public final byte getValue() {
        return value;
    }

    private static final EncodeVersion[] array = new EncodeVersion[127];
    static {
        for (byte i = 0; i<127; i++) {
            array[i] = getByValue0(i);
        }
    }

    private static EncodeVersion getByValue0(byte value) {
        for (EncodeVersion version : EncodeVersion.values()) {
            if (version.value == value) {
                return version;
            }
        }
        return null;
    }

    public static EncodeVersion getByValue(byte value) {
        if (value == 0) {
            return EncodeVersion.version_0;
        }
        if (value == 1) {
            return EncodeVersion.version_1;
        }
        return array[value];
    }
}
