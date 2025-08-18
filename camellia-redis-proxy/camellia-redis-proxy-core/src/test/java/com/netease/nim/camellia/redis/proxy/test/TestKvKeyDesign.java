package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.Index;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KeyDesign;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by caojiajun on 2025/8/18
 */
public class TestKvKeyDesign {

    @Test
    public void test() {
        KeyDesign keyDesign = new KeyDesign("a");
        {
            long keyVersion = System.currentTimeMillis();
            long expireTime = keyVersion + 3600 * 1000L;
            KeyMeta keyMeta = new KeyMeta(EncodeVersion.version_0, KeyType.hash, keyVersion, expireTime);

            byte[] key = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
            byte[] field = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
            byte[] hashFieldSubKey = keyDesign.hashFieldSubKey(keyMeta, key, field);

            byte[] decodeKeyBySubKey = keyDesign.decodeKeyBySubKey(hashFieldSubKey);
            Assert.assertArrayEquals(decodeKeyBySubKey, key);
            Assert.assertEquals(keyVersion, keyDesign.decodeKeyVersionBySubKey(hashFieldSubKey, key.length));

            byte[] decodeHashFieldBySubKey = keyDesign.decodeHashFieldBySubKey(hashFieldSubKey, key);
            Assert.assertArrayEquals(decodeHashFieldBySubKey, field);

            byte[] subKeyPrefix = keyDesign.getSubKeyPrefix();
            byte[] subKeyPrefixCheck = new byte[subKeyPrefix.length];
            System.arraycopy(hashFieldSubKey, 0, subKeyPrefixCheck,0, subKeyPrefixCheck.length);
            Assert.assertArrayEquals(subKeyPrefix, subKeyPrefixCheck);

            byte[] subKeyPrefix2 = keyDesign.getSubKeyPrefix2();
            byte[] subKeyPrefix2Check = new byte[subKeyPrefix2.length];
            System.arraycopy(subKeyPrefix2, 0, subKeyPrefix2Check,0, subKeyPrefix2Check.length);
            Assert.assertArrayEquals(subKeyPrefix2, subKeyPrefix2Check);
        }

        {
            long keyVersion = System.currentTimeMillis();
            long expireTime = keyVersion + 3600 * 1000L;
            KeyMeta keyMeta = new KeyMeta(EncodeVersion.version_0, KeyType.set, keyVersion, expireTime);

            byte[] key = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
            byte[] member = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
            byte[] setMemberSubKey = keyDesign.setMemberSubKey(keyMeta, key, member);

            byte[] decodeKeyBySubKey = keyDesign.decodeKeyBySubKey(setMemberSubKey);
            Assert.assertArrayEquals(decodeKeyBySubKey, key);
            Assert.assertEquals(keyVersion, keyDesign.decodeKeyVersionBySubKey(setMemberSubKey, key.length));

            byte[] decodeSetMemberBySubKey = keyDesign.decodeSetMemberBySubKey(setMemberSubKey, key);
            Assert.assertArrayEquals(decodeSetMemberBySubKey, member);

            byte[] subKeyPrefix = keyDesign.getSubKeyPrefix();
            byte[] subKeyPrefixCheck = new byte[subKeyPrefix.length];
            System.arraycopy(setMemberSubKey, 0, subKeyPrefixCheck,0, subKeyPrefixCheck.length);
            Assert.assertArrayEquals(subKeyPrefix, subKeyPrefixCheck);

        }

        {
            long keyVersion = System.currentTimeMillis();
            long expireTime = keyVersion + 3600 * 1000L;
            KeyMeta keyMeta = new KeyMeta(EncodeVersion.version_0, KeyType.zset, keyVersion, expireTime);

            byte[] key = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
            byte[] member = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
            double v = ThreadLocalRandom.current().nextDouble(1000);

            byte[] zsetMemberSubKey1 = keyDesign.zsetMemberSubKey1(keyMeta, key, member);

            byte[] decodeKeyBySubKey1 = keyDesign.decodeKeyBySubKey(zsetMemberSubKey1);
            Assert.assertArrayEquals(decodeKeyBySubKey1, key);
            Assert.assertEquals(keyVersion, keyDesign.decodeKeyVersionBySubKey(zsetMemberSubKey1, key.length));

            byte[] decodeZSetMemberBySubKey1 = keyDesign.decodeZSetMemberBySubKey1(zsetMemberSubKey1, key);
            Assert.assertArrayEquals(decodeZSetMemberBySubKey1, member);

            byte[] score = BytesUtils.toBytes(v);
            byte[] zsetMemberSubKey2 = keyDesign.zsetMemberSubKey2(keyMeta, key, member, score);

            byte[] decodeKeyBySubKey2 = keyDesign.decodeKeyBySubKey(zsetMemberSubKey2);
            Assert.assertArrayEquals(decodeKeyBySubKey2, key);
            Assert.assertEquals(keyVersion, keyDesign.decodeKeyVersionBySubKey(zsetMemberSubKey2, key.length));

            double decodeZSetScoreBySubKey2 = keyDesign.decodeZSetScoreBySubKey2(zsetMemberSubKey2, key);
            Assert.assertArrayEquals(BytesUtils.toBytes(v), BytesUtils.toBytes(decodeZSetScoreBySubKey2));

            byte[] subKeyPrefix = keyDesign.getSubKeyPrefix();
            byte[] subKeyPrefixCheck = new byte[subKeyPrefix.length];
            System.arraycopy(zsetMemberSubKey1, 0, subKeyPrefixCheck,0, subKeyPrefixCheck.length);
            Assert.assertArrayEquals(subKeyPrefix, subKeyPrefixCheck);

            byte[] subKeyPrefix2 = keyDesign.getSubKeyPrefix2();
            byte[] subKeyPrefix2Check = new byte[subKeyPrefix2.length];
            System.arraycopy(zsetMemberSubKey2, 0, subKeyPrefix2Check,0, subKeyPrefix2Check.length);
            Assert.assertArrayEquals(subKeyPrefix2, subKeyPrefix2Check);
        }

        {
            long keyVersion = System.currentTimeMillis();
            long expireTime = keyVersion + 3600 * 1000L;
            KeyMeta keyMeta = new KeyMeta(EncodeVersion.version_1, KeyType.zset, keyVersion, expireTime);

            byte[] key = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
            byte[] member = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);

            Index index = Index.fromRaw(member);

            Assert.assertTrue(index.isIndex());
            byte[] zsetIndexSubKey = keyDesign.zsetIndexSubKey(keyMeta, key, index);

            byte[] decodeKeyBySubKey = keyDesign.decodeKeyBySubKey(zsetIndexSubKey);
            Assert.assertArrayEquals(decodeKeyBySubKey, key);


            byte[] subIndexKeyPrefix = keyDesign.getSubIndexKeyPrefix();
            byte[] subIndexKeyPrefixCheck = new byte[subIndexKeyPrefix.length];
            System.arraycopy(zsetIndexSubKey, 0, subIndexKeyPrefixCheck,0, subIndexKeyPrefixCheck.length);
            Assert.assertArrayEquals(subIndexKeyPrefix, subIndexKeyPrefixCheck);
        }

    }
}
