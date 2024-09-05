package com.netease.nim.camellia.redis.proxy.upstream.kv.domain;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Created by caojiajun on 2024/4/7
 */
public class KeyDesign {

    private static final Logger logger = LoggerFactory.getLogger(KeyDesign.class);

    private static final byte[] HASH_TAG_LEFT = "{".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HASH_TAG_RIGHT = "}".getBytes(StandardCharsets.UTF_8);

    private final int prefixLen;
    private final byte[] namespace;
    private final byte[] metaPrefix;
    private final byte[] cachePrefix;
    private final byte[] subKeyPrefix;
    private final byte[] subKey2Prefix;
    private final byte[] indexKeyPrefix;

    public KeyDesign(String namespace) {
        this.namespace = namespace.getBytes(StandardCharsets.UTF_8);
        this.metaPrefix = BytesUtils.merge("m#".getBytes(StandardCharsets.UTF_8), this.namespace);
        this.cachePrefix = BytesUtils.merge("c#".getBytes(StandardCharsets.UTF_8), this.namespace);
        this.subKeyPrefix = BytesUtils.merge("s#".getBytes(StandardCharsets.UTF_8), this.namespace);
        this.subKey2Prefix = BytesUtils.merge("k#".getBytes(StandardCharsets.UTF_8), this.namespace);
        this.indexKeyPrefix = BytesUtils.merge("i#".getBytes(StandardCharsets.UTF_8), this.namespace);
        this.prefixLen = subKeyPrefix.length;
        reload();
        ProxyDynamicConf.registerCallback(this::reload);
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
        return BytesUtils.merge(metaPrefix, key);
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
    public byte[] subKeyPrefix(KeyMeta keyMeta, byte[] key) {
        byte[] keySize = BytesUtils.toBytes(key.length);
        byte[] data = BytesUtils.merge(subKeyPrefix, keySize, key);
        data = BytesUtils.merge(data, BytesUtils.toBytes(keyMeta.getKeyVersion()));
        return data;
    }

    public byte[] subKeyPrefix2(KeyMeta keyMeta, byte[] key) {
        byte[] keySize = BytesUtils.toBytes(key.length);
        byte[] data = BytesUtils.merge(subKey2Prefix, keySize, key);
        data = BytesUtils.merge(data, BytesUtils.toBytes(keyMeta.getKeyVersion()));
        return data;
    }

    public byte[] subIndexKeyPrefix(KeyMeta keyMeta, byte[] key) {
        byte[] keySize = BytesUtils.toBytes(key.length);
        byte[] data = BytesUtils.merge(indexKeyPrefix, keySize, key);
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

    private EncodeVersion hashKeyMetaVersion;
    private EncodeVersion zsetKeyMetaVersion;
    private EncodeVersion setKeyMetaVersion;

    private void reload() {
        int hash = RedisKvConf.getInt(Utils.bytesToString(namespace), "kv.hash.encode.version", 0);
        if (hashKeyMetaVersion == null || hash != hashKeyMetaVersion.getValue()) {
            if (hash == 0) {
                hashKeyMetaVersion = EncodeVersion.version_0;
            } else if (hash == 1) {
                hashKeyMetaVersion = EncodeVersion.version_1;
            } else {
                logger.warn("illegal hash encode version, namespace = {}", Utils.bytesToString(namespace));
                if (hashKeyMetaVersion == null) {
                    hashKeyMetaVersion = EncodeVersion.version_0;
                }
            }
            logger.info("hash encode version = {}, namespace = {}", hashKeyMetaVersion, Utils.bytesToString(namespace));
        }
        //
        int zset = RedisKvConf.getInt(Utils.bytesToString(namespace), "kv.zset.encode.version", 0);
        if (zsetKeyMetaVersion == null || zset != zsetKeyMetaVersion.getValue()) {
            if (zset == 0) {
                zsetKeyMetaVersion = EncodeVersion.version_0;
            } else if (zset == 1) {
                zsetKeyMetaVersion = EncodeVersion.version_1;
            } else {
                logger.warn("illegal zset encode version, namespace = {}", Utils.bytesToString(namespace));
                if (zsetKeyMetaVersion == null) {
                    zsetKeyMetaVersion = EncodeVersion.version_0;
                }
            }
            logger.info("zset encode version = {}, namespace = {}", zsetKeyMetaVersion, Utils.bytesToString(namespace));
        }
        //
        int set = RedisKvConf.getInt(Utils.bytesToString(namespace), "kv.set.encode.version", 0);
        if (setKeyMetaVersion == null || set != setKeyMetaVersion.getValue()) {
            if (set == 0) {
                setKeyMetaVersion = EncodeVersion.version_0;
            } else if (set == 1) {
                setKeyMetaVersion = EncodeVersion.version_1;
            } else {
                logger.warn("illegal set encode version, namespace = {}", Utils.bytesToString(namespace));
                if (setKeyMetaVersion == null) {
                    setKeyMetaVersion = EncodeVersion.version_0;
                }
            }
            logger.info("set encode version = {}, namespace = {}", setKeyMetaVersion, Utils.bytesToString(namespace));
        }
    }

    public EncodeVersion hashEncodeVersion() {
        return hashKeyMetaVersion;
    }

    public EncodeVersion zsetEncodeVersion() {
        return zsetKeyMetaVersion;
    }

    public EncodeVersion setEncodeVersion() {
        return setKeyMetaVersion;
    }
}
