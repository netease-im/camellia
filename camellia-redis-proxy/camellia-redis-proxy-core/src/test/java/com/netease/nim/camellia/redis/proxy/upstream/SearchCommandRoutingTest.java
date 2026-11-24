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

public class SearchCommandRoutingTest {

    @Test
    public void indexCommandsRejectCustomShardingWithoutSendingToAnyShard() {
        RecordingFactory factory = new RecordingFactory();
        UpstreamRedisClientTemplate template = template(factory, shardedTable());
        try {
            for (Command command : indexCommands()) {
                Reply reply = template.sendCommand(0, Collections.singletonList(command)).getFirst().join();
                Assert.assertTrue(reply instanceof ErrorReply);
                Assert.assertTrue(reply.toString().contains("not support"));
            }
            for (RecordingClient client : factory.clients.values()) {
                Assert.assertTrue(client.received.isEmpty());
            }
        } finally {
            template.shutdown();
        }
    }

    @Test
    public void indexCommandsForwardIntactToSingleUpstream() {
        RecordingFactory factory = new RecordingFactory();
        UpstreamRedisClientTemplate template = template(factory, ResourceTableUtil.simpleTable(new Resource("redis://@127.0.0.1:6379")));
        try {
            List<Command> commands = indexCommands();
            List<CompletableFuture<Reply>> replies = template.sendCommand(0, commands);
            for (CompletableFuture<Reply> reply : replies) {
                Assert.assertSame(StatusReply.OK, reply.join());
            }
            Assert.assertEquals(commands, factory.clients.get("redis://@127.0.0.1:6379").received);
        } finally {
            template.shutdown();
        }
    }

    @Test
    public void suggestionsWithTheSameDictionaryReachTheSameShard() {
        RecordingFactory factory = new RecordingFactory();
        UpstreamRedisClientTemplate template = template(factory, shardedTable());
        List<Command> commands = Arrays.asList(
                command("FT.SUGADD", "dictionary", "hello", "1.5"),
                command("FT.SUGGET", "dictionary", "he", "WITHSCORES"),
                command("FT.SUGDEL", "dictionary", "hello"),
                command("FT.SUGLEN", "dictionary"));
        try {
            for (CompletableFuture<Reply> reply : template.sendCommand(0, commands)) {
                Assert.assertSame(StatusReply.OK, reply.join());
            }
            int received = 0;
            for (RecordingClient client : factory.clients.values()) {
                if (!client.received.isEmpty()) {
                    Assert.assertEquals(commands, client.received);
                    received += client.received.size();
                }
            }
            Assert.assertEquals(4, received);
        } finally {
            template.shutdown();
        }
    }

    private static UpstreamRedisClientTemplate template(RecordingFactory factory, ResourceTable table) {
        return new UpstreamRedisClientTemplate(new RedisProxyEnv.Builder().clientFactory(factory).build(), table);
    }

    private static ResourceTable shardedTable() {
        return ReadableResourceTableUtil.parseTable("{\"type\":\"sharding\",\"operation\":{\"bucketSize\":2,\"operationMap\":{"
                + "\"0\":\"redis://@127.0.0.1:6379\",\"1\":\"redis://@127.0.0.1:6380\"}}}");
    }

    private static List<Command> indexCommands() {
        return Arrays.asList(command("FT.ALIASLIST", "index"),
                command("FT.HYBRID", "index", "SEARCH", "hello", "VSIM", "@embedding", "$vector", "PARAMS", "2", "vector", "data"));
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
