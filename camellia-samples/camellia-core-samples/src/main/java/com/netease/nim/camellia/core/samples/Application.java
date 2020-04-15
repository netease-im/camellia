package com.netease.nim.camellia.core.samples;

import com.netease.nim.camellia.core.api.CamelliaApiUtil;
import com.netease.nim.camellia.core.api.ReloadableProxyFactory;
import com.netease.nim.camellia.core.client.hub.standard.StandardProxyGenerator;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ResourceTableUtil;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/11/13.
 */
public class Application {

    public static void main(String[] args) {
        testLocalConf();
        testRemoteConf();
    }

    //基于本地静态配置
    private static void testLocalConf() {
        Resource rw = new Resource("rw");
        Resource w = new Resource("w");
        ResourceTable resourceTable = ResourceTableUtil.simple2W1RTable(rw, rw, w);
        StandardProxyGenerator<Cache> generator = new StandardProxyGenerator<>(Cache.class, resourceTable);

        Cache proxy = generator.generate();

        test(proxy);
    }

    //基于dashboard的动态配置
    private static void testRemoteConf() {
        ReloadableProxyFactory<Cache> factory = new ReloadableProxyFactory.Builder<Cache>()
                .service(CamelliaApiUtil.init("http://127.0.0.1:8080"))//dashboard的地址
                .bid(1L)//业务类型
                .bgroup("default")//业务分组
                .clazz(Cache.class)//代理对象
                .monitorEnable(true)//是否上报统计信息
                .checkIntervalMillis(5000)//配置检查间隔，单位ms
                .build();
        Cache proxy = factory.getDynamicProxy();

        test(proxy);
    }

    private static void test(Cache cache) {
        System.out.println(cache.get("k1"));
        System.out.println(cache.set("k1", "v1"));
        System.out.println(cache.getBulk("k1", "k2"));
        Map<String, String> kvs = new HashMap<>();
        kvs.put("k2", "v2");
        kvs.put("k3", "v3");
        System.out.println(cache.setBulk(kvs));
    }
}
