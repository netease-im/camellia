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
 * v0 无缓存，且key-meta里有field-count
 * v1 无缓存，且key-meta里没有field-count
 * v2 v0结构，且有hgetall+hget缓存
 * v3 v1结构，且有hgetall+hget缓存
 * <p>
 * zset
 * v0 无缓存，使用纯kv-client实现，kv-client底层有2个sub-key，一个是member->score，一个是score+member->null
 * v1 有缓存，kv-client底层有1个sub-key，是member->score，redis-key有1个，就是zset本身
 * v2 有缓存，kv-client底层有2个sub-key，一个是member->score，一个是index->member，redis-key有2个，一个是zset_with_index，一个是index
 * v3 redis同时做storage和缓存，kv-client底层有1个sub-key，就是index->member，redis-key有2个，storage是zset_with_index，cache是index
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
        return array[value];
    }
}
