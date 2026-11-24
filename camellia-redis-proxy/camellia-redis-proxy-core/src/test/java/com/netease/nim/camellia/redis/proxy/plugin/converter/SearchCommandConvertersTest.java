package com.netease.nim.camellia.redis.proxy.plugin.converter;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchCommandConvertersTest {

    @Test
    public void suggestionsConvertOnlyDictionaryKeyAndRestoreIt() {
        AtomicInteger callbacks = new AtomicInteger();
        Converters converters = converters(callbacks);
        String[][] requests = {
                {"FT.SUGADD", "dictionary", "hello", "1.5", "INCR", "PAYLOAD", "data"},
                {"FT.SUGGET", "dictionary", "he", "FUZZY", "WITHSCORES", "WITHPAYLOADS", "MAX", "10"},
                {"FT.SUGDEL", "dictionary", "hello"},
                {"FT.SUGLEN", "dictionary"}
        };
        for (String[] args : requests) {
            Command command = command(args);
            command.getKeys();
            byte[][] original = command.getObjects().clone();
            Assert.assertEquals("ft." + args[0].substring(3).toLowerCase(), command.getRedisCommand().strRaw());
            converters.convertRequest(command);
            Assert.assertEquals("key:dictionary", Utils.bytesToString(command.getObjects()[1]));
            Assert.assertEquals(1, command.getKeys().size());
            Assert.assertArrayEquals(command.getObjects()[1], command.getKeys().getFirst());
            for (int i = 2; i < original.length; i++) {
                Assert.assertSame(original[i], command.getObjects()[i]);
            }
            BulkReply suggestion = new BulkReply(Utils.stringToBytes("key:hello"));
            BulkReply score = new BulkReply(Utils.stringToBytes("1.5"));
            BulkReply payload = new BulkReply(new byte[]{0, (byte) 0xff, 1});
            Reply reply = new MultiBulkReply(new Reply[]{suggestion, score, payload});
            converters.convertReply(command, reply);
            Assert.assertEquals("key:hello", suggestion.toString());
            Assert.assertEquals("1.5", score.toString());
            Assert.assertArrayEquals(new byte[]{0, (byte) 0xff, 1}, payload.getRaw());
            Assert.assertArrayEquals(original[1], command.getObjects()[1]);
            Assert.assertArrayEquals(original[1], command.getKeys().getFirst());
        }
        Assert.assertEquals(8, callbacks.get());
    }

    @Test
    public void indexNamesQueriesAndBinaryVectorsAreNotKeysOrValuesToConvert() {
        AtomicInteger callbacks = new AtomicInteger();
        Converters converters = converters(callbacks);
        Command hybrid = command("FT.HYBRID", "index", "SEARCH", "@title:hello", "VSIM", "@embedding", "$vector",
                "PARAMS", "2", "vector", "binary-vector", "TIMEOUT", "100");
        hybrid.getObjects()[10] = new byte[]{0, (byte) 0xff, 2, 3};
        for (Command command : new Command[]{command("FT.ALIASLIST", "index"), hybrid}) {
            byte[][] original = command.getObjects().clone();
            converters.convertRequest(command);
            converters.convertReply(command, new MultiBulkReply(new Reply[]{new BulkReply(Utils.stringToBytes("key:document"))}));
            Assert.assertTrue(command.getKeys().isEmpty());
            Assert.assertFalse(command.isBlocking());
            for (int i = 0; i < original.length; i++) {
                Assert.assertSame(original[i], command.getObjects()[i]);
            }
        }
        Assert.assertEquals(0, callbacks.get());
    }

    @Test
    public void missingDictionaryDoesNotInvokeKeyConverter() {
        AtomicInteger callbacks = new AtomicInteger();
        Converters converters = converters(callbacks);
        for (String name : new String[]{"FT.SUGADD", "FT.SUGGET", "FT.SUGDEL", "FT.SUGLEN"}) {
            Command command = command(name);
            converters.convertRequest(command);
            Assert.assertTrue(command.getKeys().isEmpty());
        }
        Assert.assertEquals(0, callbacks.get());
    }

    private static Converters converters(AtomicInteger callbacks) {
        ConverterConfig config = new ConverterConfig();
        config.setKeyConverter(new KeyConverter() {
            @Override
            public byte[] convert(CommandContext context, RedisCommand command, byte[] key) {
                callbacks.incrementAndGet();
                return Utils.stringToBytes("key:" + Utils.bytesToString(key));
            }

            @Override
            public byte[] reverseConvert(CommandContext context, RedisCommand command, byte[] key) {
                callbacks.incrementAndGet();
                String value = Utils.bytesToString(key);
                Assert.assertTrue(value.startsWith("key:"));
                return Utils.stringToBytes(value.substring(4));
            }
        });
        config.setStringConverter(new StringConverter() {
            @Override
            public byte[] valueConvert(CommandContext context, byte[] key, byte[] value) {
                throw new AssertionError("Search arguments are not String values");
            }

            @Override
            public byte[] valueReverseConvert(CommandContext context, byte[] key, byte[] value) {
                throw new AssertionError("Search replies are not String values");
            }
        });
        return new Converters(config);
    }

    private static Command command(String... args) {
        return new Command(Arrays.stream(args).map(Utils::stringToBytes).toArray(byte[][]::new));
    }
}
