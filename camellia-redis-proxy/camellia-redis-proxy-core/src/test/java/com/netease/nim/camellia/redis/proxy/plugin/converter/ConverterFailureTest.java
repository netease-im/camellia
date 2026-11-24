package com.netease.nim.camellia.redis.proxy.plugin.converter;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

public class ConverterFailureTest {
    @Test
    public void failedKeyConversionRejectsReadAndWriteRequests() throws Exception {
        ConverterConfig config = new ConverterConfig();
        config.setKeyConverter(new FailingKey());
        ConverterProxyPlugin plugin = plugin(config);
        for (Command command : new Command[]{command("GET", "key"), command("SET", "key", "value")}) {
            rejected(plugin.executeRequest(new ProxyRequest(0, command, null)), "ERR request conversion failed");
        }
    }

    @Test
    public void failedValueConversionRejectsPartiallyConvertedRequest() throws Exception {
        ConverterConfig config = new ConverterConfig();
        config.setStringConverter(new FailingValue());
        ConverterProxyPlugin plugin = plugin(config);
        rejected(plugin.executeRequest(new ProxyRequest(0, command("SET", "key", "secret"), null)), "ERR request conversion failed");
    }

    @Test
    public void failedReplyConversionReplacesEncodedValueWithError() throws Exception {
        ConverterConfig config = new ConverterConfig();
        config.setStringConverter(new FailingValue());
        ConverterProxyPlugin plugin = plugin(config);
        rejected(plugin.executeReply(new ProxyReply(command("GET", "key"), bulk("secret"), false)), "ERR reply conversion failed");
    }

    @Test
    public void partiallyDecodedArrayIsNotReturned() throws Exception {
        ConverterConfig config = new ConverterConfig();
        config.setStringConverter(new StringConverter() {
            private int calls;

            @Override
            public byte[] valueConvert(CommandContext context, byte[] key, byte[] value) {
                return value;
            }

            @Override
            public byte[] valueReverseConvert(CommandContext context, byte[] key, byte[] value) {
                if (calls++ == 1) {
                    throw new IllegalStateException("secret diagnostic");
                }
                return Utils.stringToBytes("decoded");
            }
        });
        MultiBulkReply reply = new MultiBulkReply(new Reply[]{bulk("encoded-first"), bulk("encoded-second")});
        rejected(plugin(config).executeReply(new ProxyReply(command("MGET", "a", "b"), reply, false)), "ERR reply conversion failed");
        Assert.assertEquals("decoded", reply.getReplies()[0].toString());
    }

    @Test
    public void asynchronousPubsubConversionFailureReturnsError() throws Exception {
        ConverterConfig config = new ConverterConfig();
        config.setKeyConverter(new FailingKey());
        MultiBulkReply message = new MultiBulkReply(new Reply[]{bulk("message"), bulk("channel"), bulk("payload")});
        rejected(plugin(config).executeReply(new ProxyReply(new CommandContext(1L, "default", null), RedisCommand.SUBSCRIBE, message, false)), "ERR reply conversion failed");
    }

    @Test
    public void pluginResponsesAndSuccessfulCommandsRemainUnchanged() throws Exception {
        ConverterConfig config = new ConverterConfig();
        config.setStringConverter(new FailingValue());
        ConverterProxyPlugin plugin = plugin(config);
        Assert.assertTrue(plugin.executeReply(new ProxyReply(command("GET", "key"), bulk("value"), true)).isPass());
        Assert.assertTrue(plugin.executeRequest(new ProxyRequest(0, command("PING"), null)).isPass());
    }

    private static void rejected(ProxyPluginResponse response, String error) {
        Assert.assertFalse(response.isPass());
        Assert.assertTrue(response.getReply() instanceof ErrorReply);
        Assert.assertEquals(error, response.getReply().toString());
    }

    private static ConverterProxyPlugin plugin(ConverterConfig config) throws Exception {
        ConverterProxyPlugin plugin = new ConverterProxyPlugin();
        Field field = ConverterProxyPlugin.class.getDeclaredField("converters");
        field.setAccessible(true);
        field.set(plugin, new Converters(config));
        return plugin;
    }

    private static Command command(String... args) {
        return new Command(Arrays.stream(args).map(Utils::stringToBytes).toArray(byte[][]::new));
    }

    private static BulkReply bulk(String value) {
        return new BulkReply(Utils.stringToBytes(value));
    }

    private static class FailingKey implements KeyConverter {
        @Override
        public byte[] convert(CommandContext context, RedisCommand command, byte[] key) {
            throw new IllegalStateException("secret diagnostic");
        }

        @Override
        public byte[] reverseConvert(CommandContext context, RedisCommand command, byte[] key) {
            throw new IllegalStateException("secret diagnostic");
        }
    }

    private static class FailingValue implements StringConverter {
        @Override
        public byte[] valueConvert(CommandContext context, byte[] key, byte[] value) {
            throw new IllegalStateException("secret diagnostic");
        }

        @Override
        public byte[] valueReverseConvert(CommandContext context, byte[] key, byte[] value) {
            throw new IllegalStateException("secret diagnostic");
        }
    }
}
