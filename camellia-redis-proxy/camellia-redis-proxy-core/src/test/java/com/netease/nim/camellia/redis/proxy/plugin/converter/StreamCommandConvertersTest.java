package com.netease.nim.camellia.redis.proxy.plugin.converter;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

public class StreamCommandConvertersTest {
    @Test
    public void groupAndConsumerNamesAreNotOptionsAndKeysAreCachedCorrectly() {
        for (String group : new String[]{"STREAMS", "BLOCK"}) {
            Command command = command("XREADGROUP", "GROUP", group, "STREAMS", "COUNT", "1", "STREAMS", "stream", ">");
            Assert.assertFalse(command.isBlocking());
            Assert.assertEquals("stream", Utils.bytesToString(command.getKeys().getFirst()));
            Converters converters = converters();
            converters.convertRequest(command);
            Assert.assertEquals("key:stream", Utils.bytesToString(command.getKeys().getFirst()));
            Assert.assertEquals(group, Utils.bytesToString(command.getObjects()[2]));
            Assert.assertEquals("STREAMS", Utils.bytesToString(command.getObjects()[3]));
            converters.convertReply(command, MultiBulkReply.NIL_REPLY);
            Assert.assertEquals("stream", Utils.bytesToString(command.getKeys().getFirst()));
        }
    }

    @Test
    public void blockIsRecognizedOnlyAsAnOptionBeforeKeys() {
        Assert.assertFalse(command("XREAD", "STREAMS", "BLOCK", "0").isBlocking());
        Assert.assertFalse(command("XREADGROUP", "GROUP", "BLOCK", "BLOCK", "STREAMS", "stream", ">").isBlocking());
        Assert.assertTrue(command("XREADGROUP", "GROUP", "STREAMS", "BLOCK", "BLOCK", "0", "STREAMS", "stream", ">").isBlocking());
        Assert.assertTrue(command("XREAD", "COUNT", "1", "BLOCK", "0", "STREAMS", "stream", "0").isBlocking());
    }

    @Test
    public void extendedOptionsDoNotHideStreamKeys() {
        Command command = command("XREADGROUP", "CLAIM", "0", "MAXCOUNT", "10", "MAXSIZE", "1000", "GROUP", "STREAMS", "consumer", "NOACK", "STREAMS", "first", "second", ">", ">");
        Assert.assertEquals(Arrays.asList("first", "second"), command.getKeys().stream().map(Utils::bytesToString).collect(Collectors.toList()));
        converters().convertRequest(command);
        Assert.assertEquals(Arrays.asList("key:first", "key:second"), command.getKeys().stream().map(Utils::bytesToString).collect(Collectors.toList()));
    }

    @Test
    public void streamReplyKeysDecodeWithoutModifyingIdsOrFields() {
        for (String name : new String[]{"XREAD", "XREADGROUP"}) {
            Command command = command(name, "STREAMS", "first", "second", "0", "0");
            Converters converters = converters();
            converters.convertRequest(command);
            MultiBulkReply messages = array(array(bulk("1-0"), array(bulk("field"), bulk("value"))));
            MultiBulkReply entry = array(bulk("key:first"), messages);
            MultiBulkReply deleted = array(bulk("key:second"), BulkReply.NIL_REPLY);
            converters.convertReply(command, array(entry, deleted));
            Assert.assertEquals("first", entry.getReplies()[0].toString());
            Assert.assertEquals("second", deleted.getReplies()[0].toString());
            Assert.assertSame(messages, entry.getReplies()[1]);
            Assert.assertSame(BulkReply.NIL_REPLY, deleted.getReplies()[1]);
        }
    }

    private static Converters converters() {
        ConverterConfig config = new ConverterConfig();
        config.setKeyConverter(new KeyConverter() {
            @Override
            public byte[] convert(CommandContext context, RedisCommand command, byte[] key) {
                return Utils.stringToBytes("key:" + Utils.bytesToString(key));
            }

            @Override
            public byte[] reverseConvert(CommandContext context, RedisCommand command, byte[] key) {
                Assert.assertNotNull(key);
                String raw = Utils.bytesToString(key);
                Assert.assertTrue(raw, raw.startsWith("key:"));
                return Utils.stringToBytes(raw.substring(4));
            }
        });
        return new Converters(config);
    }

    private static Command command(String... args) {
        return new Command(Arrays.stream(args).map(Utils::stringToBytes).toArray(byte[][]::new));
    }

    private static BulkReply bulk(String value) {
        return new BulkReply(Utils.stringToBytes(value));
    }

    private static MultiBulkReply array(Reply... values) {
        return new MultiBulkReply(values);
    }
}
