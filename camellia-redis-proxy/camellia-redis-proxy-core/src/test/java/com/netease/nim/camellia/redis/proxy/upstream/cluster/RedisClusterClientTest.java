package com.netease.nim.camellia.redis.proxy.upstream.cluster;

import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import org.junit.Assert;
import org.junit.Test;

public class RedisClusterClientTest {

    @Test
    public void shouldRouteReadCommandToSlave() {
        //EXISTS是读命令，redis-cluster-slaves资源下应该可以分摊到slave节点
        Assert.assertFalse(RedisClusterClient.mustRouteToMaster(RedisCommand.EXISTS));
        Assert.assertFalse(RedisClusterClient.mustRouteToMaster(RedisCommand.MGET));
    }

    @Test
    public void shouldRouteWriteCommandToMaster() {
        //DEL/UNLINK/TOUCH是写命令，必须路由到master节点
        Assert.assertTrue(RedisClusterClient.mustRouteToMaster(RedisCommand.DEL));
        Assert.assertTrue(RedisClusterClient.mustRouteToMaster(RedisCommand.UNLINK));
        Assert.assertTrue(RedisClusterClient.mustRouteToMaster(RedisCommand.TOUCH));
        Assert.assertTrue(RedisClusterClient.mustRouteToMaster(RedisCommand.MSET));
    }

    @Test
    public void shouldRouteUnknownCommandToSlave() {
        Assert.assertFalse(RedisClusterClient.mustRouteToMaster(null));
    }
}
