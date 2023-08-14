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

        test("rediss://@127.0.0.1:6379", "rediss://@127.0.0.1:6379");
        test("rediss://@127.0.0.1:6379?db=10", "rediss://@127.0.0.1:6379?db=10");
        test("rediss://@127.0.0.1:6379?db=", "rediss://@127.0.0.1:6379");
        test("rediss://@127.0.0.1:6379?db", "rediss://@127.0.0.1:6379");
        test("rediss://@127.0.0.1:6379?", "rediss://@127.0.0.1:6379");
        test("rediss://passwd@127.0.0.1:6379", "rediss://passwd@127.0.0.1:6379");
        test("rediss://user:passwd@127.0.0.1:6379", "rediss://user:passwd@127.0.0.1:6379");
        test("rediss://user:passwd@127.0.0.1:6379?db=1", "rediss://user:passwd@127.0.0.1:6379?db=1");

        test("redis-cluster://@127.0.0.1:6379,127.0.0.2:6379", "redis-cluster://@127.0.0.1:6379,127.0.0.2:6379");
        test("redis-cluster://passwd@127.0.0.1:6379,127.0.0.2:6379", "redis-cluster://passwd@127.0.0.1:6379,127.0.0.2:6379");
        test("redis-cluster://user:passwd@127.0.0.1:6379,127.0.0.2:6379", "redis-cluster://user:passwd@127.0.0.1:6379,127.0.0.2:6379");

        test("rediss-cluster://@127.0.0.1:6379,127.0.0.2:6379", "rediss-cluster://@127.0.0.1:6379,127.0.0.2:6379");
        test("rediss-cluster://passwd@127.0.0.1:6379,127.0.0.2:6379", "rediss-cluster://passwd@127.0.0.1:6379,127.0.0.2:6379");
        test("rediss-cluster://user:passwd@127.0.0.1:6379,127.0.0.2:6379", "rediss-cluster://user:passwd@127.0.0.1:6379,127.0.0.2:6379");

        test("redis-cluster-slaves://@127.0.0.1:6379,127.0.0.2:6379", "redis-cluster-slaves://@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379", "redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?", "redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster", "redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=", "redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=true", "redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=true");
        test("redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false", "redis-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("redis-cluster-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379", "redis-cluster-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");

        test("rediss-cluster-slaves://@127.0.0.1:6379,127.0.0.2:6379", "rediss-cluster-slaves://@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("rediss-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379", "rediss-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("rediss-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?", "rediss-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("rediss-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster", "rediss-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("rediss-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=", "rediss-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("rediss-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=true", "rediss-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=true");
        test("rediss-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false", "rediss-cluster-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");
        test("rediss-cluster-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379", "rediss-cluster-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379?withMaster=false");


        test("redis-sentinel://@127.0.0.1:6379,127.0.0.2:6379/mymaster", "redis-sentinel://@127.0.0.1:6379,127.0.0.2:6379/mymaster");
        test("redis-sentinel://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster", "redis-sentinel://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster");
        test("redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster", "redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster");
        test("redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?", "redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster");
        test("redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db", "redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster");
        test("redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1", "redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1");
        test("redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelUserName=xxx&sentinelPassword=xxx", "redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelUserName=xxx&sentinelPassword=xxx");
        test("redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelPassword=xxx", "redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelPassword=xxx");
        test("redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelPassword=xxx&sentinelSSL=true", "redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelPassword=xxx&sentinelSSL=true");
        test("redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelPassword=xxx&sentinelSSL=false", "redis-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelPassword=xxx");

        test("rediss-sentinel://@127.0.0.1:6379,127.0.0.2:6379/mymaster", "rediss-sentinel://@127.0.0.1:6379,127.0.0.2:6379/mymaster");
        test("rediss-sentinel://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster", "rediss-sentinel://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster");
        test("rediss-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster", "rediss-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster");
        test("rediss-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?", "rediss-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster");
        test("rediss-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db", "rediss-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster");
        test("rediss-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1", "rediss-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1");
        test("rediss-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelUserName=xxx&sentinelPassword=xxx", "rediss-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelUserName=xxx&sentinelPassword=xxx");
        test("rediss-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelPassword=xxx", "rediss-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelPassword=xxx");
        test("rediss-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelPassword=xxx&sentinelSSL=true", "rediss-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelPassword=xxx&sentinelSSL=true");
        test("rediss-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelPassword=xxx&sentinelSSL=false", "rediss-sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&sentinelPassword=xxx");

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
        test("redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1&sentinelPassword=xxx&sentinelSSL=true", "redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1&sentinelPassword=xxx&sentinelSSL=true");
        test("redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1&sentinelPassword=xxx&sentinelSSL=false", "redis-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1&sentinelPassword=xxx");

        test("rediss-sentinel-slaves://@127.0.0.1:6379,127.0.0.2:6379/mymaster", "rediss-sentinel-slaves://@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false");
        test("rediss-sentinel-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster", "rediss-sentinel-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false");
        test("rediss-sentinel-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=true", "rediss-sentinel-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=true");
        test("rediss-sentinel-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false", "rediss-sentinel-slaves://passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false");
        test("rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster", "rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false");
        test("rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1", "rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1");
        test("rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&", "rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1");
        test("rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&withMaster", "rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1");
        test("rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?db=1&withMaster=", "rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1");
        test("rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1", "rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1");
        test("rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1&sentinelUserName=xxx&sentinelPassword=xxx", "rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1&sentinelUserName=xxx&sentinelPassword=xxx");
        test("rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1&sentinelPassword=xxx", "rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1&sentinelPassword=xxx");
        test("rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1&sentinelPassword=xxx&sentinelSSL=true", "rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1&sentinelPassword=xxx&sentinelSSL=true");
        test("rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1&sentinelPassword=xxx&sentinelSSL=false", "rediss-sentinel-slaves://user:passwd@127.0.0.1:6379,127.0.0.2:6379/mymaster?withMaster=false&db=1&sentinelPassword=xxx");

        test("redis-proxies://@127.0.0.1:6379,127.0.0.2:6379", "redis-proxies://@127.0.0.1:6379,127.0.0.2:6379");
        test("redis-proxies://passwd@127.0.0.1:6379,127.0.0.2:6379", "redis-proxies://passwd@127.0.0.1:6379,127.0.0.2:6379");
        test("redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379", "redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379");
        test("redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379?", "redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379");
        test("redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379?db", "redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379");
        test("redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379?db=1", "redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379?db=1");

        test("rediss-proxies://@127.0.0.1:6379,127.0.0.2:6379", "rediss-proxies://@127.0.0.1:6379,127.0.0.2:6379");
        test("rediss-proxies://passwd@127.0.0.1:6379,127.0.0.2:6379", "rediss-proxies://passwd@127.0.0.1:6379,127.0.0.2:6379");
        test("rediss-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379", "rediss-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379");
        test("rediss-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379?", "rediss-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379");
        test("rediss-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379?db", "rediss-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379");
        test("rediss-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379?db=1", "rediss-proxies://user:passwd@127.0.0.1:6379,127.0.0.2:6379?db=1");

        test("redis-proxies-discovery://@proxyName", "redis-proxies-discovery://@proxyName");
        test("redis-proxies-discovery://passwd@proxyName", "redis-proxies-discovery://passwd@proxyName");
        test("redis-proxies-discovery://user:passwd@proxyName", "redis-proxies-discovery://user:passwd@proxyName");
        test("redis-proxies-discovery://user:passwd@proxyName?", "redis-proxies-discovery://user:passwd@proxyName");
        test("redis-proxies-discovery://user:passwd@proxyName?db", "redis-proxies-discovery://user:passwd@proxyName");
        test("redis-proxies-discovery://user:passwd@proxyName?db=1", "redis-proxies-discovery://user:passwd@proxyName?db=1");

        test("rediss-proxies-discovery://@proxyName", "rediss-proxies-discovery://@proxyName");
        test("rediss-proxies-discovery://passwd@proxyName", "rediss-proxies-discovery://passwd@proxyName");
        test("rediss-proxies-discovery://user:passwd@proxyName", "rediss-proxies-discovery://user:passwd@proxyName");
        test("rediss-proxies-discovery://user:passwd@proxyName?", "rediss-proxies-discovery://user:passwd@proxyName");
        test("rediss-proxies-discovery://user:passwd@proxyName?db", "rediss-proxies-discovery://user:passwd@proxyName");
        test("rediss-proxies-discovery://user:passwd@proxyName?db=1", "rediss-proxies-discovery://user:passwd@proxyName?db=1");

        test("sentinel://@127.0.0.1:6379,127.0.0.2:6379", "sentinel://@127.0.0.1:6379,127.0.0.2:6379");
        test("sentinel://passwd@127.0.0.1:6379,127.0.0.2:6379", "sentinel://passwd@127.0.0.1:6379,127.0.0.2:6379");
        test("sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379", "sentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379");

        test("ssentinel://@127.0.0.1:6379,127.0.0.2:6379", "ssentinel://@127.0.0.1:6379,127.0.0.2:6379");
        test("ssentinel://passwd@127.0.0.1:6379,127.0.0.2:6379", "ssentinel://passwd@127.0.0.1:6379,127.0.0.2:6379");
        test("ssentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379", "ssentinel://user:passwd@127.0.0.1:6379,127.0.0.2:6379");

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
