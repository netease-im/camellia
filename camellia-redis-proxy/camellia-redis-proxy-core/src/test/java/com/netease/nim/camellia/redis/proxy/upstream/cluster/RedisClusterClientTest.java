package com.netease.nim.camellia.redis.proxy.upstream.cluster;

import com.netease.nim.camellia.redis.base.resource.RedisClusterResource;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

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
    public void newHashGetCommandsWithSideEffectsMustUseMaster() {
        Assert.assertTrue(RedisClusterClient.mustRouteToMaster(RedisCommand.HGETEX));
        Assert.assertTrue(RedisClusterClient.mustRouteToMaster(RedisCommand.HGETDEL));
        Assert.assertFalse(RedisClusterClient.mustRouteToMaster(RedisCommand.VSIM));
    }

    @Test
    public void newMultiKeyCommandsRejectCrossSlotBeforeConnecting() {
        RedisClusterResource resource = new RedisClusterResource(
                Collections.singletonList(new RedisClusterResource.Node("127.0.0.1", 6379)), null, null, 0);
        RedisClusterClient client = new RedisClusterClient(resource, 1);
        String[][] requests = {
                {"MSETEX", "2", "{a}:1", "v1", "{b}:2", "v2", "EX", "10"},
                {"JSON.MSET", "{a}:1", "$", "{}", "{b}:2", "$", "{}"},
                {"CMS.MERGE", "{a}:dest", "1", "{b}:source"},
                {"TDIGEST.MERGE", "{a}:dest", "1", "{b}:source"},
                {"SDIFFCARD", "2", "{a}:1", "{b}:2"},
                {"SUNIONCARD", "2", "{a}:1", "{b}:2"}
        };
        for (String[] args : requests) {
            Command command = new Command(Arrays.stream(args).map(Utils::stringToBytes).toArray(byte[][]::new));
            CompletableFuture<Reply> future = new CompletableFuture<>();
            client.sendCommand(0, Collections.singletonList(command), Collections.singletonList(future));
            Assert.assertTrue(args[0], future.isDone());
            Assert.assertTrue(args[0], future.join() instanceof ErrorReply);
            Assert.assertTrue(args[0], future.join().toString().contains("CROSSSLOT"));
        }
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
