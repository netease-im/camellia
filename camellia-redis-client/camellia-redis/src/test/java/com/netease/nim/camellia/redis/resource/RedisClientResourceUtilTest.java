package com.netease.nim.camellia.redis.resource;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.resource.CamelliaRedisProxyResource;
import com.netease.nim.camellia.redis.proxy.CamelliaRedisProxyContext;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.JedisPool;

/**
 * Created by caojiajun on 2026/4/30
 */
public class RedisClientResourceUtilTest {

    private static final JedisPool jedisPool = new JedisPool();

    @BeforeClass
    public static void beforeClass() {
        CamelliaRedisProxyContext.register(resource -> jedisPool);
    }

    @Test
    public void shouldPreserveCamelliaRedisProxyDbParam() {
        Resource resource = RedisClientResourceUtil.parseResourceByUrl(
                new Resource("camellia-redis-proxy://pass@proxyName?bid=1&bgroup=default&db=2"));

        Assert.assertTrue(resource instanceof CamelliaRedisProxyResource);
        CamelliaRedisProxyResource proxyResource = (CamelliaRedisProxyResource) resource;
        Assert.assertEquals(1L, proxyResource.getBid());
        Assert.assertEquals("default", proxyResource.getBgroup());
        Assert.assertEquals(2, proxyResource.getDb());
        Assert.assertEquals("camellia-redis-proxy://pass@proxyName?bid=1&bgroup=default&db=2", proxyResource.getUrl());
    }

    @Test
    public void shouldPreserveCamelliaRedisProxyDbParamWithoutBidAndBgroup() {
        Resource resource = RedisClientResourceUtil.parseResourceByUrl(
                new Resource("camellia-redis-proxy://pass@proxyName?db=3"));

        Assert.assertTrue(resource instanceof CamelliaRedisProxyResource);
        CamelliaRedisProxyResource proxyResource = (CamelliaRedisProxyResource) resource;
        Assert.assertEquals(-1L, proxyResource.getBid());
        Assert.assertNull(proxyResource.getBgroup());
        Assert.assertEquals(3, proxyResource.getDb());
        Assert.assertEquals("camellia-redis-proxy://pass@proxyName?db=3", proxyResource.getUrl());
    }
}
