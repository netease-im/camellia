package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.CamelliaRedisProxyContext;
import com.netease.nim.camellia.redis.resource.RedisClientResourceUtil;
import redis.clients.jedis.JedisPool;

/**
 * Created by caojiajun on 2023/2/22
 */
public class TestResource {

    public static void main(String[] args) {
        test("redis://@127.0.0.1:6379", "redis://@127.0.0.1:6379");
        test("redis://@127.0.0.1:6379?db=10", "redis://@127.0.0.1:6379?db=10");
        test("redis://@127.0.0.1:6379?db=", "redis://@127.0.0.1:6379");
        test("redis://@127.0.0.1:6379?db", "redis://@127.0.0.1:6379");
        test("redis://@127.0.0.1:6379?", "redis://@127.0.0.1:6379");
        test("redis://passwd@127.0.0.1:6379", "redis://passwd@127.0.0.1:6379");
        test("redis://user:passwd@127.0.0.1:6379", "redis://user:passwd@127.0.0.1:6379");
        test("redis://user:passwd@127.0.0.1:6379?db=1", "redis://user:passwd@127.0.0.1:6379?db=1");

        test("redis-cluster://@127.0.0.1:6379,127.0.0.2:6379", "redis-cluster://@127.0.0.1:6379,127.0.0.2:6379");
        test("redis-cluster://passwd@127.0.0.1:6379,127.0.0.2:6379", "redis-cluster://passwd@127.0.0.1:6379,127.0.0.2:6379");
        test("redis-cluster://user:passwd@127.0.0.1:6379,127.0.0.2:6379", "redis-cluster://user:passwd@127.0.0.1:6379,127.0.0.2:6379");

        test("redis-cluster-slaves://@127.0.0.1:6379,127.0.0.2:6379", "redis-cluster-slaves://@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379", "redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?", "redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster", "redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=", "redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=true", "redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=true");
        test("redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false", "redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("redis-cluster-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379", "redis-cluster-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");

        test("redis-sentinel://@127.0.0.1:6379,127.0.0.2:6379/mymaster", "redis-sentinel://@127.0.0.1:6379,127.0.0.2:6379/mymaster");
        test("redis-sentinel://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster", "redis-sentinel://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster");
        test("redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster", "redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster");
        test("redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?", "redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster");
        test("redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db", "redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster");
        test("redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1", "redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1");
        test("redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelUserName=xxx&sentinelPassword=xxx", "redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelUserName=xxx&sentinelPassword=xxx");
        test("redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelPassword=xxx", "redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelPassword=xxx");

        test("redis-sentinel-slaves://@127.0.0.1:6379,127.0.0.2:6379/mymaster", "redis-sentinel-slaves://@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false");
        test("redis-sentinel-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster", "redis-sentinel-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false");
        test("redis-sentinel-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=true", "redis-sentinel-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=true");
        test("redis-sentinel-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false", "redis-sentinel-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false");
        test("redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster", "redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false");
        test("redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1", "redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1");
        test("redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&", "redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1");
        test("redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&withMaster", "redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1");
        test("redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&withMaster=", "redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1");
        test("redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1", "redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1");
        test("redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1&sentinelUserName=xxx&sentinelPassword=xxx", "redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1&sentinelUserName=xxx&sentinelPassword=xxx");
        test("redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1&sentinelPassword=xxx", "redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1&sentinelPassword=xxx");

        test("redis-proxies://@127.0.0.1:6379,127.0.0.2:6379", "redis-proxies://@127.0.0.1:6379,127.0.0.2:6379");
        test("redis-proxies://passwd@127.0.0.1:6379,127.0.0.2:6379", "redis-proxies://passwd@127.0.0.1:6379,127.0.0.2:6379");
        test("redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379", "redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379");
        test("redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379?", "redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379");
        test("redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379?db", "redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379");
        test("redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379?db=1", "redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379?db=1");

        test("redis-proxies-discovery://@proxyName", "redis-proxies-discovery://@proxyName");
        test("redis-proxies-discovery://passwd@proxyName", "redis-proxies-discovery://passwd@proxyName");
        test("redis-proxies-discovery://user:passwd@proxyName", "redis-proxies-discovery://user:passwd@proxyName");
        test("redis-proxies-discovery://user:passwd@proxyName?", "redis-proxies-discovery://user:passwd@proxyName");
        test("redis-proxies-discovery://user:passwd@proxyName?db", "redis-proxies-discovery://user:passwd@proxyName");
        test("redis-proxies-discovery://user:passwd@proxyName?db=1", "redis-proxies-discovery://user:passwd@proxyName?db=1");

        CamelliaRedisProxyContext.register(resource -> new JedisPool());
        test("camellia-redis-proxy://@proxyName", "camellia-redis-proxy://@proxyName");
        test("camellia-redis-proxy://passwd@proxyName", "camellia-redis-proxy://passwd@proxyName");
        test("camellia-redis-proxy://passwd@proxyName?", "camellia-redis-proxy://passwd@proxyName");
        test("camellia-redis-proxy://passwd@proxyName?bid", "camellia-redis-proxy://passwd@proxyName");
        test("camellia-redis-proxy://passwd@proxyName?bid=", "camellia-redis-proxy://passwd@proxyName");
        test("camellia-redis-proxy://passwd@proxyName?db=", "camellia-redis-proxy://passwd@proxyName");
        test("camellia-redis-proxy://passwd@proxyName?bid=1&bgroup=default", "camellia-redis-proxy://passwd@proxyName?bid=1&bgroup=default");
        test("camellia-redis-proxy://passwd@proxyName?db=1", "camellia-redis-proxy://passwd@proxyName?db=1");
        test("camellia-redis-proxy://passwd@proxyName?bid=1&bgroup=default&db=1", "camellia-redis-proxy://passwd@proxyName?bid=1&bgroup=default&db=1");
        System.out.println("success");
    }

    public static void test(String url, String target) {
        Resource resource = RedisClientResourceUtil.parseResourceByUrl(new Resource(url));
        if (!resource.getUrl().equals(target)) {
            throw new IllegalArgumentException("check " + resource.getUrl() + "===" + target + " fail");
        }
    }
}
