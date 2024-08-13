package com.netease.nim.camellia.redis.proxy.upstream.kv.domain;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.MD5Util;

import java.nio.charset.StandardCharsets;

/**
 * Created by caojiajun on 2024/4/7
 */
public class KeyDesign {

    private static final byte[] HASH_TAG_LEFT = "{".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HASH_TAG_RIGHT = "}".getBytes(StandardCharsets.UTF_8);

    private final int prefixLen;
    private final byte[] namespace;
    private final byte[] metaPrefix;
    private final byte[] cachePrefix;
    private final byte[] subKeyPrefix;
    private final byte[] subKey2Prefix;
    private final byte[] indexKeyPrefix;

    public KeyDesign(byte[] namespace) {
        this.namespace = namespace;
        this.metaPrefix = BytesUtils.merge("m#".getBytes(StandardCharsets.UTF_8), namespace);
        this.cachePrefix = BytesUtils.merge("c#".getBytes(StandardCharsets.UTF_8), namespace);
        this.subKeyPrefix = BytesUtils.merge("s#".getBytes(StandardCharsets.UTF_8), namespace);
        this.subKey2Prefix = BytesUtils.merge("k#".getBytes(StandardCharsets.UTF_8), namespace);
        this.indexKeyPrefix = BytesUtils.merge("i#".getBytes(StandardCharsets.UTF_8), namespace);
        this.prefixLen = subKeyPrefix.length + 8;
    }

    public byte[] getNamespace() {
        return namespace;
    }

    public byte[] getMetaPrefix() {
        return metaPrefix;
    }

    public byte[] getSubKeyPrefix() {
        return subKeyPrefix;
    }

    public byte[] getSubKeyPrefix2() {
        return subKey2Prefix;
    }

    public byte[] getSubIndexKeyPrefix() {
        return indexKeyPrefix;
    }

    //
    // meta key
    //

    public byte[] metaKey(byte[] key) {
        return BytesUtils.merge(prefix(metaPrefix, key), key);
    }

    public byte[] decodeKeyByMetaKey(byte[] metaKey) {
        if (metaKey.length <= prefixLen) {
            return null;
        }
        byte[] key = new byte[metaKey.length - prefixLen];
        System.arraycopy(metaKey, prefixLen, key, 0, key.length);
        return key;
    }

    //
    // cache key
    //

    public byte[] cacheKey(KeyMeta keyMeta, byte[] key) {
        long version = keyMeta.getKeyVersion();
        key = BytesUtils.merge(HASH_TAG_LEFT, key, HASH_TAG_RIGHT);
        return BytesUtils.merge(cachePrefix, key, Utils.stringToBytes(String.valueOf(version)));
    }

    public byte[] zsetMemberIndexCacheKey(KeyMeta keyMeta, byte[] key, Index index) {
        long version = keyMeta.getKeyVersion();
        key = BytesUtils.merge(HASH_TAG_LEFT, key, HASH_TAG_RIGHT);
        byte[] data = BytesUtils.merge(cachePrefix, key, Utils.stringToBytes(String.valueOf(version)));
        if (index != null && index.getRef().length > 0) {
            data = BytesUtils.merge(data, index.getRef());
        }
        return data;
    }

    //
    // sub key
    // prefix
    //

    private byte[] prefix(byte[] prefix, byte[] key) {
        byte[] array = new byte[prefix.length + 8];
        System.arraycopy(prefix, 0, array, 0, prefix.length);
        byte[] md5 = MD5Util.md5(key);
        System.arraycopy(md5, 0, array, prefix.length, 8);
        return array;
    }

    public byte[] subKeyPrefix(KeyMeta keyMeta, byte[] key) {
        byte[] keySize = BytesUtils.toBytes(key.length);
        byte[] data = BytesUtils.merge(prefix(subKeyPrefix, key), keySize, key);
        data = BytesUtils.merge(data, BytesUtils.toBytes(keyMeta.getKeyVersion()));
        return data;
    }

    public byte[] subKeyPrefix2(KeyMeta keyMeta, byte[] key) {
        byte[] keySize = BytesUtils.toBytes(key.length);
        byte[] data = BytesUtils.merge(prefix(subKey2Prefix, key), keySize, key);
        data = BytesUtils.merge(data, BytesUtils.toBytes(keyMeta.getKeyVersion()));
        return data;
    }

    public byte[] subIndexKeyPrefix(KeyMeta keyMeta, byte[] key) {
        byte[] keySize = BytesUtils.toBytes(key.length);
        byte[] data = BytesUtils.merge(prefix(indexKeyPrefix, key), keySize, key);
        data = BytesUtils.merge(data, BytesUtils.toBytes(keyMeta.getKeyVersion()));
        return data;
    }

    //
    // sub key
    // encode
    //

    public byte[] hashFieldSubKey(KeyMeta keyMeta, byte[] key, byte[] field) {
        byte[] data = subKeyPrefix(keyMeta, key);
        if (field.length > 0) {
            data = BytesUtils.merge(data, field);
        }
        return data;
    }

    public byte[] setMemberSubKey(KeyMeta keyMeta, byte[] key, byte[] member) {
        byte[] data = subKeyPrefix(keyMeta, key);
        if (member.length > 0) {
            data = BytesUtils.merge(data, member);
        }
        return data;
    }

    public byte[] zsetMemberSubKey1(KeyMeta keyMeta, byte[] key, byte[] member) {
        byte[] data = subKeyPrefix(keyMeta, key);
        if (member.length > 0) {
            data = BytesUtils.merge(data, member);
        }
        return data;
    }

    public byte[] zsetIndexSubKey(KeyMeta keyMeta, byte[] key, Index index) {
        byte[] data = subIndexKeyPrefix(keyMeta, key);
        if (index != null && index.getRef().length > 0) {
            data = BytesUtils.merge(data, index.getRef());
        }
        return data;
    }

    public byte[] zsetMemberSubKey2(KeyMeta keyMeta, byte[] key, byte[] member, byte[] score) {
        byte[] data = subKeyPrefix2(keyMeta, key);
        if (score.length > 0) {
            data = BytesUtils.merge(data, score);
        }
        if (member.length > 0) {
            data = BytesUtils.merge(data, member);
        }
        return data;
    }

    //
    // sub key
    // decode
    //

    public byte[] decodeKeyBySubKey(byte[] subKey) {
        int keyLen = BytesUtils.toInt(subKey, prefixLen);
        byte[] key = new byte[keyLen];
        System.arraycopy(subKey, prefixLen + 4, key, 0, keyLen);
        return key;
    }

    public long decodeKeyVersionBySubKey(byte[] subKey, int keyLen) {
        return BytesUtils.toLong(subKey, prefixLen + 4 + keyLen);
    }

    public byte[] decodeHashFieldBySubKey(byte[] subKey, byte[] key) {
        int prefixSize = prefixLen + 4 + key.length + 8;
        int fieldSize = subKey.length - prefixSize;
        byte[] field = new byte[fieldSize];
        System.arraycopy(subKey, prefixSize, field, 0, fieldSize);
        return field;
    }

    public byte[] decodeSetMemberBySubKey(byte[] subKey, byte[] key) {
        int prefixSize = prefixLen + 4 + key.length + 8;
        int memberSize = subKey.length - prefixSize;
        byte[] field = new byte[memberSize];
        System.arraycopy(subKey, prefixSize, field, 0, memberSize);
        return field;
    }

    public byte[] decodeZSetMemberBySubKey1(byte[] subKey1, byte[] key) {
        int prefixSize = prefixLen + 4 + key.length + 8;
        int fieldSize = subKey1.length - prefixSize;
        byte[] field = new byte[fieldSize];
        System.arraycopy(subKey1, prefixSize, field, 0, fieldSize);
        return field;
    }

    public double decodeZSetScoreBySubKey2(byte[] subKey2, byte[] key) {
        int prefixSize = prefixLen + 4 + key.length + 8;
        return BytesUtils.toDouble(subKey2, prefixSize);
    }

    public byte[] decodeZSetMemberBySubKey2(byte[] subKey2, byte[] key) {
        int prefixSize = prefixLen + 4 + key.length + 8;
        byte[] member = new byte[subKey2.length - prefixSize - 8];
        System.arraycopy(subKey2, prefixSize + 8, member, 0, member.length);
        return member;
    }

    //
    // encode version
    //

    public EncodeVersion hashKeyMetaVersion() {
        int version = ProxyDynamicConf.getInt("kv.hash.key.meta.version", 0);
        if (version == 0) {
            return EncodeVersion.version_0;
        } else if (version == 1) {
            return EncodeVersion.version_1;
        } else {
            throw new KvException("ERR illegal key meta version");
        }
    }

    public EncodeVersion zsetKeyMetaVersion() {
        int version = ProxyDynamicConf.getInt("kv.zset.key.meta.version", 0);
        if (version == 0) {
            return EncodeVersion.version_0;
        } else if (version == 1) {
            return EncodeVersion.version_1;
        } else {
            throw new KvException("ERR illegal key meta version");
        }
    }

    public EncodeVersion setKeyMetaVersion() {
        int version = ProxyDynamicConf.getInt("kv.set.key.meta.version", 0);
        if (version == 0) {
            return EncodeVersion.version_0;
        } else if (version == 1) {
            return EncodeVersion.version_1;
        } else {
            throw new KvException("ERR illegal key meta version");
        }
    }
}
