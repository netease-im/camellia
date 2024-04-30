package com.netease.nim.camellia.redis.proxy.upstream.kv.domain;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.tools.utils.MD5Util;

import java.nio.charset.StandardCharsets;

/**
 * Created by caojiajun on 2024/4/7
 */
public class KeyStruct {

    private static final byte[] HASH_TAG_LEFT = "{".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HASH_TAG_RIGHT = "}".getBytes(StandardCharsets.UTF_8);

    private final int storePrefixLen;
    private final byte[] namespace;
    private final byte[] metaPrefix;
    private final byte[] cachePrefix;
    private final byte[] subKeyPrefix;
    private final byte[] indexKeyPrefix;

    public KeyStruct(byte[] namespace) {
        this.namespace = namespace;
        this.metaPrefix = BytesUtils.merge("m#".getBytes(StandardCharsets.UTF_8), namespace);
        this.cachePrefix = BytesUtils.merge("c#".getBytes(StandardCharsets.UTF_8), namespace);
        this.subKeyPrefix = BytesUtils.merge("s#".getBytes(StandardCharsets.UTF_8), namespace);
        this.indexKeyPrefix = BytesUtils.merge("i#".getBytes(StandardCharsets.UTF_8), namespace);
        this.storePrefixLen = subKeyPrefix.length;
    }

    public byte[] getNamespace() {
        return namespace;
    }

    public byte[] metaKey(byte[] key) {
        return BytesUtils.merge(metaPrefix, key);
    }

    public byte[] cacheKey(KeyMeta keyMeta, byte[] key) {
        long version = keyMeta.getKeyVersion();
        return BytesUtils.merge(cachePrefix, key, BytesUtils.toBytes(version));
    }

    public byte[] hashFieldCacheKey(KeyMeta keyMeta, byte[] key, byte[] field) {
        long version = keyMeta.getKeyVersion();
        byte[] data = BytesUtils.merge(cachePrefix, key, BytesUtils.toBytes(version));
        data = BytesUtils.merge(HASH_TAG_LEFT, data, HASH_TAG_RIGHT);
        if (field.length > 0) {
            data = BytesUtils.merge(data, field);
        }
        return data;
    }

    public byte[] hashFieldSubKey(KeyMeta keyMeta, byte[] key, byte[] field) {
        byte[] keySize = BytesUtils.toBytes(key.length);
        byte[] data = BytesUtils.merge(subKeyPrefix, keySize, key);
        data = BytesUtils.merge(data, BytesUtils.toBytes(keyMeta.getKeyVersion()));
        if (field.length > 0) {
            data = BytesUtils.merge(data, field);
        }
        return data;
    }

    public byte[] zsetMemberSubKey1(KeyMeta keyMeta, byte[] key, byte[] member) {
        byte[] keySize = BytesUtils.toBytes(key.length);
        byte[] data = BytesUtils.merge(subKeyPrefix, keySize, key);
        data = BytesUtils.merge(data, BytesUtils.toBytes(keyMeta.getKeyVersion()));
        if (member.length > 0) {
            data = BytesUtils.merge(data, member);
        }
        return data;
    }

    public byte[] zsetIndexSubKey(KeyMeta keyMeta, byte[] key, Index index) {
        byte[] keySize = BytesUtils.toBytes(key.length);
        byte[] data = BytesUtils.merge(indexKeyPrefix, keySize, key);
        data = BytesUtils.merge(data, BytesUtils.toBytes(keyMeta.getKeyVersion()));
        if (index != null && index.getData().length > 0) {
            data = BytesUtils.merge(data, index.getData());
        }
        return data;
    }

    public byte[] zsetMemberIndexCacheKey(KeyMeta keyMeta, byte[] key, Index index) {
        long version = keyMeta.getKeyVersion();
        byte[] data = BytesUtils.merge(cachePrefix, key, BytesUtils.toBytes(version));
        data = BytesUtils.merge(HASH_TAG_LEFT, data, HASH_TAG_RIGHT);
        if (index != null && index.getData().length > 0) {
            data = BytesUtils.merge(data, index.getData());
        }
        return data;
    }

    public byte[] zsetMemberSubKey2(KeyMeta keyMeta, byte[] key, byte[] member, byte[] score) {
        byte[] keySize = BytesUtils.toBytes(key.length);
        byte[] data = BytesUtils.merge(subKeyPrefix, keySize, key);
        data = BytesUtils.merge(data, BytesUtils.toBytes(keyMeta.getKeyVersion()));
        if (score.length > 0) {
            data = BytesUtils.merge(data, score);
        }
        if (member.length > 0) {
            data = BytesUtils.merge(data, member);
        }
        return data;
    }

    public byte[] decodeHashFieldBySubKey(byte[] subKey, byte[] key) {
        int prefixSize = storePrefixLen + 4 + key.length + 8;
        int fieldSize = subKey.length - prefixSize;
        byte[] field = new byte[fieldSize];
        System.arraycopy(subKey, prefixSize, field, 0, fieldSize);
        return field;
    }

    public byte[] getMetaPrefix() {
        return metaPrefix;
    }

    public byte[] decodeKeyByMetaKey(byte[] metaKey) {
        if (metaKey.length <= metaPrefix.length) {
            return null;
        }
        byte[] key = new byte[metaKey.length - metaPrefix.length];
        System.arraycopy(metaKey, metaPrefix.length, key, 0, key.length);
        return key;
    }

    public byte[] getSubKeyPrefix() {
        return subKeyPrefix;
    }

    public byte[] decodeKeyBySubKey(byte[] subKey) {
        int keyLen = BytesUtils.toInt(subKey, storePrefixLen);
        byte[] key = new byte[keyLen];
        System.arraycopy(subKey, storePrefixLen + 4, key, 0, keyLen);
        return key;
    }

    public long decodeKeyVersionBySubKey(byte[] subKey, int keyLen) {
        return BytesUtils.toLong(subKey, storePrefixLen + 4 + keyLen);
    }

    public static void main(String[] args) {
        byte[] bytes = MD5Util.md5("abc".getBytes(StandardCharsets.UTF_8));
        System.out.println(bytes.length);
    }

    public EncodeVersion hashKeyMetaVersion() {
        int version = ProxyDynamicConf.getInt("kv.hash.key.meta.version", 0);
        if (version == 0) {
            return EncodeVersion.version_0;
        } else if (version == 1) {
            return EncodeVersion.version_1;
        } else if (version == 2) {
            return EncodeVersion.version_2;
        } else if (version == 3) {
            return EncodeVersion.version_3;
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
        } else if (version == 2) {
            return EncodeVersion.version_2;
        } else if (version == 3) {
            return EncodeVersion.version_3;
        } else if (version == 4) {
            return EncodeVersion.version_4;
        } else {
            throw new KvException("ERR illegal key meta version");
        }
    }
}
