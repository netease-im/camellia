package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConfLoader;
import com.netease.nim.camellia.redis.proxy.kv.obkv.OBKVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Created by caojiajun on 2025/5/27
 */
public class TestOBKVClient {

    private void init() {
        Map<String, String> initConf = new HashMap<>();
        initConf.put("kv.obkv.param.url", "");
        initConf.put("kv.obkv.full.user.name", "");
        initConf.put("kv.obkv.password", "");
        initConf.put("kv.obkv.sys.user.name", "");
        initConf.put("kv.obkv.sys.password", "");

        initConf.put("kv.obkv.table.name", "camellia_kv");

        initConf.put("kv.obkv.batch.split.size", "1000");
        ProxyDynamicConf.init(initConf, new ProxyDynamicConfLoader() {
            @Override
            public void init(Map<String, String> initConf) {
            }

            @Override
            public Map<String, String> load() {
                return initConf;
            }
        });
    }

    @Test
    public void test() {
        init();

        if (ProxyDynamicConf.getString("kv.obkv.param.url", "").isEmpty()) {
            return;
        }

        byte[] k1 = UUID.randomUUID().toString().getBytes();
        byte[] v1 = UUID.randomUUID().toString().getBytes();
        byte[] k2 = UUID.randomUUID().toString().getBytes();
        byte[] v2 = UUID.randomUUID().toString().getBytes();
        byte[] k3 = UUID.randomUUID().toString().getBytes();
        byte[] v3 = UUID.randomUUID().toString().getBytes();
        byte[] k4 = UUID.randomUUID().toString().getBytes();
        byte[] v4 = UUID.randomUUID().toString().getBytes();

        byte[] k5 = UUID.randomUUID().toString().getBytes();

        OBKVClient obkvClient = new OBKVClient();
        obkvClient.init("default");

        obkvClient.put(1, k1, v1);

        List<KeyValue> list = new ArrayList<>();
        list.add(new KeyValue(k2, v2));
        list.add(new KeyValue(k3, v3));
        list.add(new KeyValue(k4, v4));
        obkvClient.batchPut(2, list);

        KeyValue keyValue = obkvClient.get(1, k1);
        Assert.assertArrayEquals(keyValue.getKey(), k1);
        Assert.assertArrayEquals(keyValue.getValue(), v1);

        List<KeyValue> keyValues = obkvClient.batchGet(2, k2, k3, k4);
        Assert.assertEquals(3, keyValues.size());

        boolean exists = obkvClient.exists(2, k2);
        Assert.assertTrue(exists);

        boolean exists1 = obkvClient.exists(2, k5);
        Assert.assertFalse(exists1);

        boolean[] exists2 = obkvClient.exists(2, k2, k3, k4);
        Assert.assertTrue(exists2[0]);
        Assert.assertTrue(exists2[1]);
        Assert.assertTrue(exists2[2]);

        obkvClient.delete(2, k2);
        KeyValue keyValue1 = obkvClient.get(2, k2);
        Assert.assertNull(keyValue1);

        obkvClient.batchDelete(2, k3, k4);

        List<KeyValue> keyValues1 = obkvClient.batchGet(2, k2, k3, k4);
        Assert.assertEquals(0, keyValues1.size());
    }

}
