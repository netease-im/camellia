package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisHash;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by caojiajun on 2024/12/17
 */
public class RedisHashLRUTest {

    @Test
    public void test() {
        RedisHash hash = new RedisHash(new HashMap<>());

        Assert.assertNull(hash.hget(str2BytesKey("f1")));

        byte[] hset = hash.hset(str2BytesKey("f1"), str2Bytes("v0"));
        Assert.assertNull(hset);

        byte[] hset2 = hash.hset(str2BytesKey("f1"), str2Bytes("v1"));
        Assert.assertEquals(new BytesKey(hset2), str2BytesKey("v0"));

        byte[] v1 = hash.hget(str2BytesKey("f1"));
        Assert.assertEquals(new BytesKey(v1), new BytesKey(str2Bytes("v1")));

        Assert.assertEquals(hash.hlen(), 1);

        Map<BytesKey, byte[]> hdel = hash.hdel(Arrays.asList(str2BytesKey("f1"), str2BytesKey("f2")));
        Assert.assertEquals(hdel.size() , 1);
        Assert.assertEquals(new BytesKey(hdel.get(str2BytesKey("f1"))), str2BytesKey("v1"));

        for (int i=1; i<=10; i++) {
            hash.hset(str2BytesKey("f" + i), str2Bytes("v" + i));
        }

        Assert.assertEquals(hash.hlen(), 10);
        Map<BytesKey, byte[]> map = hash.hgetAll();
        Assert.assertEquals(map.size(), 10);

        Assert.assertEquals(hash.hexists(str2BytesKey("f2")), 1);

        Assert.assertEquals(hash.hstrlen(str2BytesKey("f3")), 2);
    }

    private static byte[] str2Bytes(String str) {
        return Utils.stringToBytes(str);
    }

    private static BytesKey str2BytesKey(String str) {
        return new BytesKey(Utils.stringToBytes(str));
    }
}
