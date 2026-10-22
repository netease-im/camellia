package com.netease.nim.camellia.redis.proxy.upstream.cluster;

import com.netease.nim.camellia.redis.base.resource.RedisClusterResource;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

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
        Assert.assertTrue(RedisClusterClient.mustRouteToMaster(RedisCommand.COPY));
    }

    @Test
    public void shouldRouteUnknownCommandToSlave() {
        Assert.assertFalse(RedisClusterClient.mustRouteToMaster(null));
    }

    @Test
    public void preheatAndCheckValidShouldUseDbInResourceUrl() {
        RedisClusterResource resource = new RedisClusterResource(
                Collections.singletonList(new RedisClusterResource.Node("127.0.0.1", 6379)), null, null, 2);
        RedisClusterClient client = new RedisClusterClient(resource, 1);
        Assert.assertEquals(2, client.getDefaultDb());

        RedisClusterSlotInfo slotInfo = new RedisClusterSlotInfo(resource, client);
        RedisClusterSlotInfo.Node node = new RedisClusterSlotInfo.Node("127.0.0.1", 6379, null, null, false);
        Assert.assertEquals(2, slotInfo.addrWithDb(node, client.getDefaultDb()).getDb());

        RedisClusterResource resourceWithoutDb = new RedisClusterResource(
                Collections.singletonList(new RedisClusterResource.Node("127.0.0.1", 6379)), null, null, 0);
        Assert.assertEquals(0, new RedisClusterClient(resourceWithoutDb, 1).getDefaultDb());
    }
}
