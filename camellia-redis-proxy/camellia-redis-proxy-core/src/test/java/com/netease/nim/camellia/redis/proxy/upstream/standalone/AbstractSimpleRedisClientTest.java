package com.netease.nim.camellia.redis.proxy.upstream.standalone;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import org.junit.Assert;
import org.junit.Test;

public class AbstractSimpleRedisClientTest {

    private static final String RESOURCE_URL = "redis://pass@127.0.0.1:6379";

    private AbstractSimpleRedisClient client(final RedisConnectionAddr addr) {
        return new AbstractSimpleRedisClient() {
            @Override
            public RedisConnectionAddr getAddr() {
                return addr;
            }

            @Override
            public Resource getResource() {
                return new Resource(RESOURCE_URL);
            }

            @Override
            public void start() {
            }

            @Override
            public boolean isValid() {
                return true;
            }
        };
    }

    @Test
    public void shouldUseUrlDbIfClientHasNotSelectedDb() {
        RedisConnectionAddr urlAddr = new RedisConnectionAddr("127.0.0.1", 6379, null, "pass", false, 1, true);
        AbstractSimpleRedisClient client = client(urlAddr);
        //client没有执行过select，使用url里配置的db
        Assert.assertSame(urlAddr, client.getAddr(-1));
        Assert.assertEquals(1, client.getAddr(-1).getDb());
        //client select的db和url里配置的一致，直接复用
        Assert.assertSame(urlAddr, client.getAddr(1));
    }

    @Test
    public void shouldUseClientDbIfClientHasSelectedDb() {
        RedisConnectionAddr urlAddr = new RedisConnectionAddr("127.0.0.1", 6379, null, "pass", false, 1, true);
        AbstractSimpleRedisClient client = client(urlAddr);
        //client select的db和url里配置的不一致，使用client的db
        RedisConnectionAddr addr = client.getAddr(2);
        Assert.assertNotSame(urlAddr, addr);
        Assert.assertEquals("127.0.0.1", addr.getHost());
        Assert.assertEquals(6379, addr.getPort());
        Assert.assertEquals(2, addr.getDb());
        //同一个db重复获取，返回同一个addr
        Assert.assertSame(addr, client.getAddr(2));
    }

    @Test
    public void shouldUseClientDbForUdsResource() {
        RedisConnectionAddr udsAddr = new RedisConnectionAddr("/tmp/redis.sock", null, "pass", false, 1, true);
        AbstractSimpleRedisClient client = client(udsAddr);
        Assert.assertSame(udsAddr, client.getAddr(-1));
        RedisConnectionAddr addr = client.getAddr(2);
        Assert.assertNotSame(udsAddr, addr);
        Assert.assertEquals("/tmp/redis.sock", addr.getUdsPath());
        Assert.assertEquals(2, addr.getDb());
    }
}
