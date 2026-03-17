package com.netease.nim.camellia.redis.test;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.base.resource.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by caojiajun on 2026/3/17
 */
public class RedisResourceTest {

    // ==================== Redis/Rediss Tests ====================

    @Test
    public void testRedisWithPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("redis://passwd@127.0.0.1:6379"));
        Assert.assertTrue(resource instanceof RedisResource);
        Assert.assertEquals("redis://passwd@127.0.0.1:6379", resource.getUrl());
        Assert.assertEquals("passwd", ((RedisResource) resource).getPassword());
        Assert.assertEquals("127.0.0.1", ((RedisResource) resource).getHost());
        Assert.assertEquals(6379, ((RedisResource) resource).getPort());
        Assert.assertNull(((RedisResource) resource).getUserName());
        Assert.assertEquals(0, ((RedisResource) resource).getDb());
    }

    @Test
    public void testRedisWithUserAndPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("redis://user:passwd@127.0.0.1:6379"));
        Assert.assertTrue(resource instanceof RedisResource);
        Assert.assertEquals("redis://user:passwd@127.0.0.1:6379", resource.getUrl());
        Assert.assertEquals("passwd", ((RedisResource) resource).getPassword());
        Assert.assertEquals("127.0.0.1", ((RedisResource) resource).getHost());
        Assert.assertEquals(6379, ((RedisResource) resource).getPort());
        Assert.assertEquals("user", ((RedisResource) resource).getUserName());
        Assert.assertEquals(0, ((RedisResource) resource).getDb());
    }

    @Test
    public void testRedisWithDb() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("redis://user:passwd@127.0.0.1:6379?db=2"));
        Assert.assertTrue(resource instanceof RedisResource);
        Assert.assertEquals("redis://user:passwd@127.0.0.1:6379?db=2", resource.getUrl());
        Assert.assertEquals("passwd", ((RedisResource) resource).getPassword());
        Assert.assertEquals("127.0.0.1", ((RedisResource) resource).getHost());
        Assert.assertEquals(6379, ((RedisResource) resource).getPort());
        Assert.assertEquals("user", ((RedisResource) resource).getUserName());
        Assert.assertEquals(2, ((RedisResource) resource).getDb());
    }

    @Test
    public void testRedisPasswordWithAtSymbol() {
        // 密码中包含@符号，使用lastIndexOf("@")解析，所以可以正确处理
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("redis://p@sswd@127.0.0.1:6379"));
        Assert.assertTrue(resource instanceof RedisResource);
        Assert.assertEquals("p@sswd", ((RedisResource) resource).getPassword());
        Assert.assertEquals("127.0.0.1", ((RedisResource) resource).getHost());
        Assert.assertEquals(6379, ((RedisResource) resource).getPort());
        Assert.assertNull(((RedisResource) resource).getUserName());
    }

    @Test
    public void testRedisPasswordWithMultipleAtSymbols() {
        // 密码中包含多个@符号
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("redis://p@ss@w@d@127.0.0.1:6379"));
        Assert.assertTrue(resource instanceof RedisResource);
        Assert.assertEquals("p@ss@w@d", ((RedisResource) resource).getPassword());
        Assert.assertEquals("127.0.0.1", ((RedisResource) resource).getHost());
        Assert.assertEquals(6379, ((RedisResource) resource).getPort());
    }

    @Test
    public void testRedissWithPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("rediss://passwd@127.0.0.1:6379"));
        Assert.assertTrue(resource instanceof RedissResource);
        Assert.assertEquals("rediss://passwd@127.0.0.1:6379", resource.getUrl());
        Assert.assertEquals("passwd", ((RedissResource) resource).getPassword());
        Assert.assertEquals("127.0.0.1", ((RedissResource) resource).getHost());
        Assert.assertEquals(6379, ((RedissResource) resource).getPort());
        Assert.assertNull(((RedissResource) resource).getUserName());
        Assert.assertEquals(0, ((RedissResource) resource).getDb());
    }

    @Test
    public void testRedissWithUserAndPasswordAndDb() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("rediss://user:passwd@127.0.0.1:6379?db=3"));
        Assert.assertTrue(resource instanceof RedissResource);
        Assert.assertEquals("rediss://user:passwd@127.0.0.1:6379?db=3", resource.getUrl());
        Assert.assertEquals("passwd", ((RedissResource) resource).getPassword());
        Assert.assertEquals("127.0.0.1", ((RedissResource) resource).getHost());
        Assert.assertEquals(6379, ((RedissResource) resource).getPort());
        Assert.assertEquals("user", ((RedissResource) resource).getUserName());
        Assert.assertEquals(3, ((RedissResource) resource).getDb());
    }

    // ==================== RedisSentinel/RedissSentinel Tests ====================

    @Test
    public void testRedisSentinelWithPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("redis-sentinel://passwd@127.0.0.1:26379,127.0.0.1:26380/mymaster"));
        Assert.assertTrue(resource instanceof RedisSentinelResource);
        Assert.assertEquals("redis-sentinel://passwd@127.0.0.1:26379,127.0.0.1:26380/mymaster", resource.getUrl());
        Assert.assertEquals("passwd", ((RedisSentinelResource) resource).getPassword());
        Assert.assertEquals("mymaster", ((RedisSentinelResource) resource).getMaster());
        Assert.assertNull(((RedisSentinelResource) resource).getUserName());
        Assert.assertEquals(0, ((RedisSentinelResource) resource).getDb());

        List<RedisSentinelResource.Node> nodes = ((RedisSentinelResource) resource).getNodes();
        Assert.assertEquals(2, nodes.size());
        Assert.assertEquals("127.0.0.1", nodes.get(0).getHost());
        Assert.assertEquals(26379, nodes.get(0).getPort());
        Assert.assertEquals("127.0.0.1", nodes.get(1).getHost());
        Assert.assertEquals(26380, nodes.get(1).getPort());
    }

    @Test
    public void testRedisSentinelWithUserAndPasswordAndDb() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("redis-sentinel://user:passwd@127.0.0.1:26379,127.0.0.1:26380/mymaster?db=2"));
        Assert.assertTrue(resource instanceof RedisSentinelResource);
        Assert.assertEquals("redis-sentinel://user:passwd@127.0.0.1:26379,127.0.0.1:26380/mymaster?db=2", resource.getUrl());
        Assert.assertEquals("passwd", ((RedisSentinelResource) resource).getPassword());
        Assert.assertEquals("user", ((RedisSentinelResource) resource).getUserName());
        Assert.assertEquals("mymaster", ((RedisSentinelResource) resource).getMaster());
        Assert.assertEquals(2, ((RedisSentinelResource) resource).getDb());
    }

    @Test
    public void testRedisSentinelWithSentinelParams() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(
                new Resource("redis-sentinel://user:passwd@127.0.0.1:26379/mymaster?db=1&sentinelUserName=suser&sentinelPassword=spass&sentinelSSL=true"));
        Assert.assertTrue(resource instanceof RedisSentinelResource);
        Assert.assertEquals("mymaster", ((RedisSentinelResource) resource).getMaster());
        Assert.assertEquals("user", ((RedisSentinelResource) resource).getUserName());
        Assert.assertEquals("passwd", ((RedisSentinelResource) resource).getPassword());
        Assert.assertEquals(1, ((RedisSentinelResource) resource).getDb());
        Assert.assertEquals("suser", ((RedisSentinelResource) resource).getSentinelUserName());
        Assert.assertEquals("spass", ((RedisSentinelResource) resource).getSentinelPassword());
        Assert.assertTrue(((RedisSentinelResource) resource).isSentinelSSL());
    }

    @Test
    public void testRedissSentinelWithPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("rediss-sentinel://passwd@127.0.0.1:26379,127.0.0.1:26380/mymaster"));
        Assert.assertTrue(resource instanceof RedissSentinelResource);
        Assert.assertEquals("rediss-sentinel://passwd@127.0.0.1:26379,127.0.0.1:26380/mymaster", resource.getUrl());
        Assert.assertEquals("passwd", ((RedissSentinelResource) resource).getPassword());
        Assert.assertEquals("mymaster", ((RedissSentinelResource) resource).getMaster());
    }

    // ==================== RedisCluster/RedissCluster Tests ====================

    @Test
    public void testRedisClusterWithPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("redis-cluster://passwd@127.0.0.1:7000,127.0.0.1:7001,127.0.0.1:7002"));
        Assert.assertTrue(resource instanceof RedisClusterResource);
        Assert.assertEquals("redis-cluster://passwd@127.0.0.1:7000,127.0.0.1:7001,127.0.0.1:7002", resource.getUrl());
        Assert.assertEquals("passwd", ((RedisClusterResource) resource).getPassword());
        Assert.assertNull(((RedisClusterResource) resource).getUserName());

        List<RedisClusterResource.Node> nodes = ((RedisClusterResource) resource).getNodes();
        Assert.assertEquals(3, nodes.size());
        Assert.assertEquals("127.0.0.1", nodes.get(0).getHost());
        Assert.assertEquals(7000, nodes.get(0).getPort());
        Assert.assertEquals("127.0.0.1", nodes.get(1).getHost());
        Assert.assertEquals(7001, nodes.get(1).getPort());
        Assert.assertEquals("127.0.0.1", nodes.get(2).getHost());
        Assert.assertEquals(7002, nodes.get(2).getPort());
    }

    @Test
    public void testRedisClusterWithUserAndPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("redis-cluster://user:passwd@127.0.0.1:7000,127.0.0.1:7001"));
        Assert.assertTrue(resource instanceof RedisClusterResource);
        Assert.assertEquals("redis-cluster://user:passwd@127.0.0.1:7000,127.0.0.1:7001", resource.getUrl());
        Assert.assertEquals("passwd", ((RedisClusterResource) resource).getPassword());
        Assert.assertEquals("user", ((RedisClusterResource) resource).getUserName());
    }

    @Test
    public void testRedissClusterWithPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("rediss-cluster://passwd@127.0.0.1:7000,127.0.0.1:7001"));
        Assert.assertTrue(resource instanceof RedissClusterResource);
        Assert.assertEquals("rediss-cluster://passwd@127.0.0.1:7000,127.0.0.1:7001", resource.getUrl());
        Assert.assertEquals("passwd", ((RedissClusterResource) resource).getPassword());
    }

    // ==================== RedisSentinelSlaves/RedissSentinelSlaves Tests ====================

    @Test
    public void testRedisSentinelSlavesWithPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(
                new Resource("redis-sentinel-slaves://passwd@127.0.0.1:26379,127.0.0.1:26380/mymaster?withMaster=true"));
        Assert.assertTrue(resource instanceof RedisSentinelSlavesResource);
        Assert.assertEquals("passwd", ((RedisSentinelSlavesResource) resource).getPassword());
        Assert.assertEquals("mymaster", ((RedisSentinelSlavesResource) resource).getMaster());
        Assert.assertTrue(((RedisSentinelSlavesResource) resource).isWithMaster());
        Assert.assertNull(((RedisSentinelSlavesResource) resource).getUserName());
        Assert.assertEquals(0, ((RedisSentinelSlavesResource) resource).getDb());
    }

    @Test
    public void testRedisSentinelSlavesWithAllParams() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(
                new Resource("redis-sentinel-slaves://user:passwd@127.0.0.1:26379/mymaster?withMaster=false&db=1&sentinelUserName=suser&sentinelPassword=spass&sentinelSSL=true"));
        Assert.assertTrue(resource instanceof RedisSentinelSlavesResource);
        Assert.assertEquals("user", ((RedisSentinelSlavesResource) resource).getUserName());
        Assert.assertEquals("passwd", ((RedisSentinelSlavesResource) resource).getPassword());
        Assert.assertEquals("mymaster", ((RedisSentinelSlavesResource) resource).getMaster());
        Assert.assertFalse(((RedisSentinelSlavesResource) resource).isWithMaster());
        Assert.assertEquals(1, ((RedisSentinelSlavesResource) resource).getDb());
        Assert.assertEquals("suser", ((RedisSentinelSlavesResource) resource).getSentinelUserName());
        Assert.assertEquals("spass", ((RedisSentinelSlavesResource) resource).getSentinelPassword());
        Assert.assertTrue(((RedisSentinelSlavesResource) resource).isSentinelSSL());
    }

    @Test
    public void testRedissSentinelSlavesWithPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(
                new Resource("rediss-sentinel-slaves://passwd@127.0.0.1:26379/mymaster?withMaster=false"));
        Assert.assertTrue(resource instanceof RedissSentinelSlavesResource);
        Assert.assertEquals("passwd", ((RedissSentinelSlavesResource) resource).getPassword());
        Assert.assertEquals("mymaster", ((RedissSentinelSlavesResource) resource).getMaster());
        Assert.assertFalse(((RedissSentinelSlavesResource) resource).isWithMaster());
    }

    // ==================== RedisClusterSlaves/RedissClusterSlaves Tests ====================

    @Test
    public void testRedisClusterSlavesWithPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(
                new Resource("redis-cluster-slaves://passwd@127.0.0.1:7000,127.0.0.1:7001?withMaster=true"));
        Assert.assertTrue(resource instanceof RedisClusterSlavesResource);
        Assert.assertEquals("passwd", ((RedisClusterSlavesResource) resource).getPassword());
        Assert.assertTrue(((RedisClusterSlavesResource) resource).isWithMaster());
        Assert.assertNull(((RedisClusterSlavesResource) resource).getUserName());

        List<RedisClusterResource.Node> nodes = ((RedisClusterSlavesResource) resource).getNodes();
        Assert.assertEquals(2, nodes.size());
        Assert.assertEquals("127.0.0.1", nodes.get(0).getHost());
        Assert.assertEquals(7000, nodes.get(0).getPort());
    }

    @Test
    public void testRedisClusterSlavesWithUserAndPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(
                new Resource("redis-cluster-slaves://user:passwd@127.0.0.1:7000,127.0.0.1:7001?withMaster=false"));
        Assert.assertTrue(resource instanceof RedisClusterSlavesResource);
        Assert.assertEquals("user", ((RedisClusterSlavesResource) resource).getUserName());
        Assert.assertEquals("passwd", ((RedisClusterSlavesResource) resource).getPassword());
        Assert.assertFalse(((RedisClusterSlavesResource) resource).isWithMaster());
    }

    @Test
    public void testRedissClusterSlavesWithPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(
                new Resource("rediss-cluster-slaves://passwd@127.0.0.1:7000,127.0.0.1:7001?withMaster=true"));
        Assert.assertTrue(resource instanceof RedissClusterSlavesResource);
        Assert.assertEquals("passwd", ((RedissClusterSlavesResource) resource).getPassword());
        Assert.assertTrue(((RedissClusterSlavesResource) resource).isWithMaster());
    }

    // ==================== RedisProxies/RedissProxies Tests ====================

    @Test
    public void testRedisProxiesWithPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("redis-proxies://passwd@127.0.0.1:6379,127.0.0.1:6380"));
        Assert.assertTrue(resource instanceof RedisProxiesResource);
        Assert.assertEquals("redis-proxies://passwd@127.0.0.1:6379,127.0.0.1:6380", resource.getUrl());
        Assert.assertEquals("passwd", ((RedisProxiesResource) resource).getPassword());
        Assert.assertNull(((RedisProxiesResource) resource).getUserName());
        Assert.assertEquals(0, ((RedisProxiesResource) resource).getDb());

        List<RedisProxiesResource.Node> nodes = ((RedisProxiesResource) resource).getNodes();
        Assert.assertEquals(2, nodes.size());
        Assert.assertEquals("127.0.0.1", nodes.get(0).getHost());
        Assert.assertEquals(6379, nodes.get(0).getPort());
    }

    @Test
    public void testRedisProxiesWithUserAndPasswordAndDb() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.1:6380?db=2"));
        Assert.assertTrue(resource instanceof RedisProxiesResource);
        Assert.assertEquals("redis-proxies://user:passwd@127.0.0.1:6379,127.0.0.1:6380?db=2", resource.getUrl());
        Assert.assertEquals("user", ((RedisProxiesResource) resource).getUserName());
        Assert.assertEquals("passwd", ((RedisProxiesResource) resource).getPassword());
        Assert.assertEquals(2, ((RedisProxiesResource) resource).getDb());
    }

    @Test
    public void testRedissProxiesWithPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("rediss-proxies://passwd@127.0.0.1:6379,127.0.0.1:6380"));
        Assert.assertTrue(resource instanceof RedissProxiesResource);
        Assert.assertEquals("rediss-proxies://passwd@127.0.0.1:6379,127.0.0.1:6380", resource.getUrl());
        Assert.assertEquals("passwd", ((RedissProxiesResource) resource).getPassword());
    }

    @Test
    public void testRedissProxiesWithDb() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("rediss-proxies://user:passwd@127.0.0.1:6379?db=3"));
        Assert.assertTrue(resource instanceof RedissProxiesResource);
        Assert.assertEquals("rediss-proxies://user:passwd@127.0.0.1:6379?db=3", resource.getUrl());
        Assert.assertEquals("user", ((RedissProxiesResource) resource).getUserName());
        Assert.assertEquals("passwd", ((RedissProxiesResource) resource).getPassword());
        Assert.assertEquals(3, ((RedissProxiesResource) resource).getDb());
    }

    // ==================== RedisProxiesDiscovery/RedissProxiesDiscovery Tests ====================

    @Test
    public void testRedisProxiesDiscoveryWithPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("redis-proxies-discovery://passwd@myProxy"));
        Assert.assertTrue(resource instanceof RedisProxiesDiscoveryResource);
        Assert.assertEquals("redis-proxies-discovery://passwd@myProxy", resource.getUrl());
        Assert.assertEquals("passwd", ((RedisProxiesDiscoveryResource) resource).getPassword());
        Assert.assertEquals("myProxy", ((RedisProxiesDiscoveryResource) resource).getProxyName());
        Assert.assertNull(((RedisProxiesDiscoveryResource) resource).getUserName());
        Assert.assertEquals(0, ((RedisProxiesDiscoveryResource) resource).getDb());
    }

    @Test
    public void testRedisProxiesDiscoveryWithUserAndPasswordAndDb() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("redis-proxies-discovery://user:passwd@myProxy?db=2"));
        Assert.assertTrue(resource instanceof RedisProxiesDiscoveryResource);
        Assert.assertEquals("redis-proxies-discovery://user:passwd@myProxy?db=2", resource.getUrl());
        Assert.assertEquals("user", ((RedisProxiesDiscoveryResource) resource).getUserName());
        Assert.assertEquals("passwd", ((RedisProxiesDiscoveryResource) resource).getPassword());
        Assert.assertEquals("myProxy", ((RedisProxiesDiscoveryResource) resource).getProxyName());
        Assert.assertEquals(2, ((RedisProxiesDiscoveryResource) resource).getDb());
    }

    @Test
    public void testRedissProxiesDiscoveryWithPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("rediss-proxies-discovery://passwd@myProxy"));
        Assert.assertTrue(resource instanceof RedissProxiesDiscoveryResource);
        Assert.assertEquals("rediss-proxies-discovery://passwd@myProxy", resource.getUrl());
        Assert.assertEquals("passwd", ((RedissProxiesDiscoveryResource) resource).getPassword());
        Assert.assertEquals("myProxy", ((RedissProxiesDiscoveryResource) resource).getProxyName());
    }

    @Test
    public void testRedissProxiesDiscoveryWithDb() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("rediss-proxies-discovery://user:passwd@myProxy?db=3"));
        Assert.assertTrue(resource instanceof RedissProxiesDiscoveryResource);
        Assert.assertEquals("rediss-proxies-discovery://user:passwd@myProxy?db=3", resource.getUrl());
        Assert.assertEquals("user", ((RedissProxiesDiscoveryResource) resource).getUserName());
        Assert.assertEquals("passwd", ((RedissProxiesDiscoveryResource) resource).getPassword());
        Assert.assertEquals(3, ((RedissProxiesDiscoveryResource) resource).getDb());
    }

    // ==================== Sentinel/SSentinel Tests ====================

    @Test
    public void testSentinelWithPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("sentinel://passwd@127.0.0.1:26379,127.0.0.1:26380"));
        Assert.assertTrue(resource instanceof SentinelResource);
        Assert.assertEquals("sentinel://passwd@127.0.0.1:26379,127.0.0.1:26380", resource.getUrl());
        Assert.assertEquals("passwd", ((SentinelResource) resource).getSentinelPassword());
        Assert.assertNull(((SentinelResource) resource).getSentinelUserName());

        List<RedisSentinelResource.Node> nodes = ((SentinelResource) resource).getNodes();
        Assert.assertEquals(2, nodes.size());
        Assert.assertEquals("127.0.0.1", nodes.get(0).getHost());
        Assert.assertEquals(26379, nodes.get(0).getPort());
    }

    @Test
    public void testSentinelWithUserAndPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("sentinel://user:passwd@127.0.0.1:26379"));
        Assert.assertTrue(resource instanceof SentinelResource);
        Assert.assertEquals("sentinel://user:passwd@127.0.0.1:26379", resource.getUrl());
        Assert.assertEquals("user", ((SentinelResource) resource).getSentinelUserName());
        Assert.assertEquals("passwd", ((SentinelResource) resource).getSentinelPassword());
    }

    @Test
    public void testSSentinelWithPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("ssentinel://passwd@127.0.0.1:26379,127.0.0.1:26380"));
        Assert.assertTrue(resource instanceof SSentinelResource);
        Assert.assertEquals("ssentinel://passwd@127.0.0.1:26379,127.0.0.1:26380", resource.getUrl());
        Assert.assertEquals("passwd", ((SSentinelResource) resource).getSentinelPassword());
    }

    @Test
    public void testSSentinelWithUserAndPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("ssentinel://user:passwd@127.0.0.1:26379"));
        Assert.assertTrue(resource instanceof SSentinelResource);
        Assert.assertEquals("ssentinel://user:passwd@127.0.0.1:26379", resource.getUrl());
        Assert.assertEquals("user", ((SSentinelResource) resource).getSentinelUserName());
        Assert.assertEquals("passwd", ((SSentinelResource) resource).getSentinelPassword());
    }

    // ==================== UnixDomainSocket Tests ====================

    @Test
    public void testRedisUnixDomainSocketWithPassword() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("redis-uds://passwd@/tmp/redis.sock"));
        Assert.assertTrue(resource instanceof RedisUnixDomainSocketResource);
        Assert.assertEquals("redis-uds://passwd@/tmp/redis.sock", resource.getUrl());
        Assert.assertEquals("passwd", ((RedisUnixDomainSocketResource) resource).getPassword());
        Assert.assertEquals("/tmp/redis.sock", ((RedisUnixDomainSocketResource) resource).getUdsPath());
        Assert.assertNull(((RedisUnixDomainSocketResource) resource).getUserName());
        Assert.assertEquals(0, ((RedisUnixDomainSocketResource) resource).getDb());
    }

    @Test
    public void testRedisUnixDomainSocketWithUserAndPasswordAndDb() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("redis-uds://user:passwd@/var/run/redis/redis.sock?db=2"));
        Assert.assertTrue(resource instanceof RedisUnixDomainSocketResource);
        Assert.assertEquals("redis-uds://user:passwd@/var/run/redis/redis.sock?db=2", resource.getUrl());
        Assert.assertEquals("user", ((RedisUnixDomainSocketResource) resource).getUserName());
        Assert.assertEquals("passwd", ((RedisUnixDomainSocketResource) resource).getPassword());
        Assert.assertEquals("/var/run/redis/redis.sock", ((RedisUnixDomainSocketResource) resource).getUdsPath());
        Assert.assertEquals(2, ((RedisUnixDomainSocketResource) resource).getDb());
    }

    // ==================== RedisKv Tests ====================

    @Test
    public void testRedisKv() {
        Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource("redis-kv://mynamespace"));
        Assert.assertTrue(resource instanceof RedisKvResource);
        Assert.assertEquals("redis-kv://mynamespace", resource.getUrl());
        Assert.assertEquals("mynamespace", ((RedisKvResource) resource).getNamespace());
    }

    // ==================== Utility Methods Tests ====================

    @Test
    public void testGetUrlWithoutQueryString() {
        Assert.assertEquals("user:pass@host:port", RedisResourceUtil.getUrlWithoutQueryString("user:pass@host:port?db=1"));
        Assert.assertEquals("user:pass@host:port", RedisResourceUtil.getUrlWithoutQueryString("user:pass@host:port?db=1&ssl=true"));
        Assert.assertEquals("user:pass@host:port", RedisResourceUtil.getUrlWithoutQueryString("user:pass@host:port"));
    }

    @Test
    public void testGetParamMap() {
        java.util.Map<String, String> params = RedisResourceUtil.getParamMap("user:pass@host:port?db=1&ssl=true");
        Assert.assertEquals("1", params.get("db"));
        Assert.assertEquals("true", params.get("ssl"));

        java.util.Map<String, String> emptyParams = RedisResourceUtil.getParamMap("user:pass@host:port");
        Assert.assertTrue(emptyParams.isEmpty());
    }

    @Test
    public void testGetParams() {
        java.util.Map<String, String> params = RedisResourceUtil.getParams("db=1&ssl=true&timeout=5000");
        Assert.assertEquals("1", params.get("db"));
        Assert.assertEquals("true", params.get("ssl"));
        Assert.assertEquals("5000", params.get("timeout"));
    }

    @Test
    public void testGetUserNameAndPassword() {
        String[] result1 = RedisResourceUtil.getUserNameAndPassword("user:pass");
        Assert.assertEquals("user", result1[0]);
        Assert.assertEquals("pass", result1[1]);

        String[] result2 = RedisResourceUtil.getUserNameAndPassword("pass");
        Assert.assertNull(result2[0]);
        Assert.assertEquals("pass", result2[1]);

        String[] result3 = RedisResourceUtil.getUserNameAndPassword("");
        Assert.assertNull(result3[0]);
        Assert.assertNull(result3[1]);

        String[] result4 = RedisResourceUtil.getUserNameAndPassword(null);
        Assert.assertNull(result4[0]);
        Assert.assertNull(result4[1]);

        String[] result5 = RedisResourceUtil.getUserNameAndPassword(":pass");
        Assert.assertNull(result5[0]);
        Assert.assertEquals("pass", result5[1]);

        String[] result6 = RedisResourceUtil.getUserNameAndPassword("user:");
        Assert.assertEquals("user", result6[0]);
        Assert.assertNull(result6[1]);
    }

    @Test
    public void testIsClusterResource() {
        Assert.assertTrue(RedisResourceUtil.isClusterResource(new Resource("redis-cluster://passwd@127.0.0.1:7000,127.0.0.1:7001")));
        Assert.assertTrue(RedisResourceUtil.isClusterResource(new Resource("rediss-cluster://passwd@127.0.0.1:7000,127.0.0.1:7001")));
        Assert.assertTrue(RedisResourceUtil.isClusterResource(new Resource("redis-cluster-slaves://passwd@127.0.0.1:7000?withMaster=true")));
        Assert.assertTrue(RedisResourceUtil.isClusterResource(new Resource("rediss-cluster-slaves://passwd@127.0.0.1:7000?withMaster=true")));

        Assert.assertFalse(RedisResourceUtil.isClusterResource(new Resource("redis://passwd@127.0.0.1:6379")));
        Assert.assertFalse(RedisResourceUtil.isClusterResource(new Resource("redis-sentinel://passwd@127.0.0.1:26379/master")));
    }

    // ==================== Exception Tests ====================

    @Test(expected = CamelliaRedisException.class)
    public void testRedisMissingAt() {
        RedisResourceUtil.parseResourceByUrl(new Resource("redis://127.0.0.1:6379"));
    }

    @Test(expected = CamelliaRedisException.class)
    public void testRedisSentinelMissingAt() {
        RedisResourceUtil.parseResourceByUrl(new Resource("redis-sentinel://127.0.0.1:26379/mymaster"));
    }

    @Test(expected = CamelliaRedisException.class)
    public void testRedisSentinelMissingSlash() {
        RedisResourceUtil.parseResourceByUrl(new Resource("redis-sentinel://passwd@127.0.0.1:26379,127.0.0.1:26380"));
    }

    @Test(expected = CamelliaRedisException.class)
    public void testRedisClusterMissingAt() {
        RedisResourceUtil.parseResourceByUrl(new Resource("redis-cluster://127.0.0.1:7000,127.0.0.1:7001"));
    }

    @Test(expected = CamelliaRedisException.class)
    public void testInvalidSentinelSSL() {
        RedisResourceUtil.parseResourceByUrl(new Resource("redis-sentinel://passwd@127.0.0.1:26379/mymaster?sentinelSSL=invalid"));
    }

    @Test(expected = CamelliaRedisException.class)
    public void testInvalidWithMaster() {
        RedisResourceUtil.parseResourceByUrl(new Resource("redis-sentinel-slaves://passwd@127.0.0.1:26379/mymaster?withMaster=invalid"));
    }

    @Test
    public void testParseNullResource() {
        Assert.assertNull(RedisResourceUtil.parseResourceByUrl(null));
    }

    // ==================== RedisType Tests ====================

    @Test
    public void testRedisTypeParse() {
        Assert.assertEquals(RedisType.Redis, RedisType.parseRedisType(new Resource("redis://passwd@127.0.0.1:6379")));
        Assert.assertEquals(RedisType.Rediss, RedisType.parseRedisType(new Resource("rediss://passwd@127.0.0.1:6379")));
        Assert.assertEquals(RedisType.RedisSentinel, RedisType.parseRedisType(new Resource("redis-sentinel://passwd@127.0.0.1:26379/master")));
        Assert.assertEquals(RedisType.RedissSentinel, RedisType.parseRedisType(new Resource("rediss-sentinel://passwd@127.0.0.1:26379/master")));
        Assert.assertEquals(RedisType.RedisCluster, RedisType.parseRedisType(new Resource("redis-cluster://passwd@127.0.0.1:7000")));
        Assert.assertEquals(RedisType.RedissCluster, RedisType.parseRedisType(new Resource("rediss-cluster://passwd@127.0.0.1:7000")));
        Assert.assertEquals(RedisType.RedisSentinelSlaves, RedisType.parseRedisType(new Resource("redis-sentinel-slaves://passwd@127.0.0.1:26379/master?withMaster=false")));
        Assert.assertEquals(RedisType.RedissSentinelSlaves, RedisType.parseRedisType(new Resource("rediss-sentinel-slaves://passwd@127.0.0.1:26379/master?withMaster=false")));
        Assert.assertEquals(RedisType.RedisClusterSlaves, RedisType.parseRedisType(new Resource("redis-cluster-slaves://passwd@127.0.0.1:7000?withMaster=false")));
        Assert.assertEquals(RedisType.RedissClusterSlaves, RedisType.parseRedisType(new Resource("rediss-cluster-slaves://passwd@127.0.0.1:7000?withMaster=false")));
        Assert.assertEquals(RedisType.RedisProxies, RedisType.parseRedisType(new Resource("redis-proxies://passwd@127.0.0.1:6379")));
        Assert.assertEquals(RedisType.RedissProxies, RedisType.parseRedisType(new Resource("rediss-proxies://passwd@127.0.0.1:6379")));
        Assert.assertEquals(RedisType.RedisProxiesDiscovery, RedisType.parseRedisType(new Resource("redis-proxies-discovery://passwd@proxy")));
        Assert.assertEquals(RedisType.RedissProxiesDiscovery, RedisType.parseRedisType(new Resource("rediss-proxies-discovery://passwd@proxy")));
        Assert.assertEquals(RedisType.Sentinel, RedisType.parseRedisType(new Resource("sentinel://passwd@127.0.0.1:26379")));
        Assert.assertEquals(RedisType.SSentinel, RedisType.parseRedisType(new Resource("ssentinel://passwd@127.0.0.1:26379")));
        Assert.assertEquals(RedisType.UnixDomainSocket, RedisType.parseRedisType(new Resource("redis-uds://passwd@/tmp/redis.sock")));
        Assert.assertEquals(RedisType.RedisKV, RedisType.parseRedisType(new Resource("redis-kv://namespace")));
        Assert.assertNull(RedisType.parseRedisType(new Resource("invalid://test")));
    }

    @Test
    public void testRedisTypeProperties() {
        Assert.assertEquals("redis://", RedisType.Redis.getPrefix());
        Assert.assertFalse(RedisType.Redis.isTlsEnable());

        Assert.assertEquals("rediss://", RedisType.Rediss.getPrefix());
        Assert.assertTrue(RedisType.Rediss.isTlsEnable());

        Assert.assertEquals("redis-sentinel://", RedisType.RedisSentinel.getPrefix());
        Assert.assertFalse(RedisType.RedisSentinel.isTlsEnable());

        Assert.assertEquals("rediss-sentinel://", RedisType.RedissSentinel.getPrefix());
        Assert.assertTrue(RedisType.RedissSentinel.isTlsEnable());

        Assert.assertEquals("redis-cluster://", RedisType.RedisCluster.getPrefix());
        Assert.assertFalse(RedisType.RedisCluster.isTlsEnable());

        Assert.assertEquals("rediss-cluster://", RedisType.RedissCluster.getPrefix());
        Assert.assertTrue(RedisType.RedissCluster.isTlsEnable());
    }
}
