package com.netease.nim.camellia.redis.proxy.upstream;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.redis.base.resource.RedisResource;
import com.netease.nim.camellia.redis.base.resource.RedisResourceUtil;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.upstream.standalone.RedisStandaloneClient;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class StoreCommandRoutingTest {

    @Test
    public void storeCommandsUseWriteBackendAndReadOnlyVariantsUseReadBackend() {
        RecordingFactory factory = new RecordingFactory();
        String read = "redis://@127.0.0.1:6379";
        String write = "redis://@127.0.0.1:6380";
        UpstreamRedisClientTemplate template = template(factory,
                ResourceTableUtil.simpleRwSeparateTable(new Resource(read), new Resource(write)));
        List<Command> writes = Arrays.asList(
                command("SORT", "source", "STORE", "destination"),
                command("GEORADIUS", "source", "0", "0", "1", "km", "STORE", "destination"),
                command("GEORADIUSBYMEMBER", "source", "a", "1", "km", "STOREDIST", "destination"));
        List<Command> reads = Arrays.asList(
                command("SORT_RO", "source"),
                command("GEORADIUS_RO", "source", "0", "0", "1", "km"),
                command("GEORADIUSBYMEMBER_RO", "source", "a", "1", "km"));
        try {
            for (CompletableFuture<Reply> reply : template.sendCommand(0, writes)) {
                Assert.assertSame(StatusReply.OK, reply.join());
            }
            for (CompletableFuture<Reply> reply : template.sendCommand(0, reads)) {
                Assert.assertSame(StatusReply.OK, reply.join());
            }
            Assert.assertEquals(writes, factory.clients.get(write).received);
            Assert.assertEquals(reads, factory.clients.get(read).received);
        } finally {
            template.shutdown();
        }
    }

    @Test
    public void storeCommandsAllowSameShardAndRejectDifferentShards() {
        RecordingFactory factory = new RecordingFactory();
        UpstreamRedisClientTemplate template = template(factory, shardedTable());
        try {
            for (String name : new String[]{"SORT", "GEORADIUS", "GEORADIUSBYMEMBER"}) {
                Assert.assertSame(StatusReply.OK, template.sendCommand(0,
                        Collections.singletonList(store(name, "source", "source"))).getFirst().join());
                boolean rejected = false;
                for (int i = 0; i < 100; i++) {
                    int before = factory.clients.values().stream().mapToInt(client -> client.received.size()).sum();
                    Reply reply = template.sendCommand(0, Collections.singletonList(store(name, "source", "dest" + i))).getFirst().join();
                    if (reply instanceof ErrorReply) {
                        Assert.assertTrue(reply.toString().contains("not in same resources"));
                        Assert.assertEquals(before, factory.clients.values().stream().mapToInt(client -> client.received.size()).sum());
                        rejected = true;
                        break;
                    }
                }
                Assert.assertTrue(name, rejected);
            }
        } finally {
            template.shutdown();
        }
    }

    private static Command store(String name, String source, String destination) {
        if ("SORT".equals(name)) {
            return command(name, source, "STORE", destination);
        }
        if ("GEORADIUS".equals(name)) {
            return command(name, source, "0", "0", "1", "km", "STORE", destination);
        }
        return command(name, source, "a", "1", "km", "STOREDIST", destination);
    }

    private static UpstreamRedisClientTemplate template(RecordingFactory factory, ResourceTable table) {
        return new UpstreamRedisClientTemplate(new RedisProxyEnv.Builder().clientFactory(factory).build(), table);
    }

    private static ResourceTable shardedTable() {
        return ReadableResourceTableUtil.parseTable("{\"type\":\"sharding\",\"operation\":{\"bucketSize\":2,\"operationMap\":{"
                + "\"0\":\"redis://@127.0.0.1:6379\",\"1\":\"redis://@127.0.0.1:6380\"}}}");
    }

    private static Command command(String... args) {
        return new Command(Arrays.stream(args).map(Utils::stringToBytes).toArray(byte[][]::new));
    }

    private static class RecordingFactory extends UpstreamRedisClientFactory.Default {
        private final Map<String, RecordingClient> clients = new HashMap<>();

        @Override
        public IUpstreamClient get(String url) {
            return clients.computeIfAbsent(url, value -> new RecordingClient((RedisResource) RedisResourceUtil.parseResourceByUrl(new Resource(value))));
        }
    }

    private static class RecordingClient extends RedisStandaloneClient {
        private final List<Command> received = new ArrayList<>();

        private RecordingClient(RedisResource resource) {
            super(resource);
        }

        @Override
        public void sendCommand(int db, List<Command> commands, List<CompletableFuture<Reply>> futures) {
            received.addAll(commands);
            for (CompletableFuture<Reply> future : futures) {
                future.complete(StatusReply.OK);
            }
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }
}
