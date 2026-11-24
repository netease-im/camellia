package com.netease.nim.camellia.redis.proxy.plugin.converter;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPluginResponse;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyRequest;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class NamespaceCommandConvertersTest {

    @Test
    public void randomKeyIsRejectedForActiveNamespaces() throws Exception {
        ConverterProxyPlugin plugin = plugin(new DefaultMultiTenantNamespaceKeyConverter());
        for (long bid : new long[]{1, 2}) {
            Command command = tenantCommand(bid, "default", "RANDOMKEY");
            ProxyPluginResponse response = plugin.executeRequest(new ProxyRequest(0, command, null));
            Assert.assertFalse(response.isPass());
            Assert.assertEquals("ERR RANDOMKEY is not supported by the active key converter", response.getReply().toString());
            arguments(command, "RANDOMKEY");
        }
    }

    @Test
    public void customConvertersDenyRandomKeyUnlessTheyExplicitlySupportIt() throws Exception {
        KeyConverter converter = new IdentityKeyConverter();
        Assert.assertFalse(plugin(converter).executeRequest(new ProxyRequest(0, command("RANDOMKEY"), null)).isPass());
        KeyConverter supported = new IdentityKeyConverter() {
            @Override
            public boolean isRandomKeySupported(CommandContext context) {
                return true;
            }
        };
        Assert.assertTrue(plugin(supported).executeRequest(new ProxyRequest(0, command("RANDOMKEY"), null)).isPass());
    }

    @Test
    public void randomKeyStillPassesWithoutAKeyConverterOrTenantNamespace() throws Exception {
        Assert.assertTrue(plugin(null).executeRequest(new ProxyRequest(0, command("RANDOMKEY"), null)).isPass());
        ConverterProxyPlugin plugin = plugin(new DefaultMultiTenantNamespaceKeyConverter());
        for (Command command : new Command[]{command("RANDOMKEY"), tenantCommand(0L, "default", "RANDOMKEY"),
                tenantCommand(-1L, "default", "RANDOMKEY"), tenantCommand(1L, null, "RANDOMKEY")}) {
            Assert.assertTrue(plugin.executeRequest(new ProxyRequest(0, command, null)).isPass());
        }
    }

    @Test
    public void implicitChannelsPatternIsScopedForEachTenantAndDecodedOnReply() {
        Converters converters = converters(new DefaultMultiTenantNamespaceKeyConverter());
        for (long bid : new long[]{1, 2}) {
            Command command = tenantCommand(bid, "default", "PUBSUB", "channels");
            converters.convertRequest(command);
            arguments(command, "PUBSUB", "channels", bid + "|default|*");
            BulkReply channel = bulk(bid + "|default|own");
            converters.convertReply(command, new MultiBulkReply(new BulkReply[]{channel}));
            Assert.assertEquals("own", channel.toString());
            arguments(command, "PUBSUB", "channels", "*");
        }
    }

    @Test
    public void explicitChannelPatternsAreConvertedExactlyOnce() {
        Converters converters = converters(new DefaultMultiTenantNamespaceKeyConverter());
        Command command = tenantCommand(1L, "default", "PUBSUB", "CHANNELS", "own:*");
        converters.convertRequest(command);
        arguments(command, "PUBSUB", "CHANNELS", "1|default|own:*");
        converters.convertReply(command, MultiBulkReply.EMPTY);
        arguments(command, "PUBSUB", "CHANNELS", "own:*");
    }

    @Test
    public void disabledTenantKeepsRandomKeyAndUnscopedChannels() throws Exception {
        Field confField = ProxyDynamicConf.class.getDeclaredField("conf");
        confField.setAccessible(true);
        Object previous = confField.get(null);
        Map<String, String> config = new HashMap<>(ProxyDynamicConf.getAll());
        config.put("991.namespace-test.key.converter.disable", "true");
        confField.set(null, config);
        try {
            KeyConverter keyConverter = new DefaultMultiTenantNamespaceKeyConverter();
            Assert.assertTrue(plugin(keyConverter).executeRequest(new ProxyRequest(0,
                    tenantCommand(991L, "namespace-test", "RANDOMKEY"), null)).isPass());
            Assert.assertFalse(plugin(keyConverter).executeRequest(new ProxyRequest(0,
                    tenantCommand(992L, "namespace-test", "RANDOMKEY"), null)).isPass());
            Command command = tenantCommand(991L, "namespace-test", "PUBSUB", "CHANNELS");
            converters(keyConverter).convertRequest(command);
            arguments(command, "PUBSUB", "CHANNELS", "*");
        } finally {
            confField.set(null, previous);
        }
    }

    @Test
    public void noKeyConverterKeepsOriginalChannelsRequest() {
        Command command = command("PUBSUB", "CHANNELS");
        converters(null).convertRequest(command);
        arguments(command, "PUBSUB", "CHANNELS");
    }

    @Test
    public void unrelatedAndMalformedRequestsKeepTheirArguments() throws Exception {
        ConverterProxyPlugin plugin = plugin(new DefaultMultiTenantNamespaceKeyConverter());
        for (String[] args : new String[][]{{"PING"}, {"PUBSUB"}, {"PUBSUB", "NUMPAT"},
                {"PUBSUB", "CHANNELS", "a", "extra"}}) {
            Command command = tenantCommand(1L, "default", args);
            Assert.assertTrue(plugin.executeRequest(new ProxyRequest(0, command, null)).isPass());
            arguments(command, args);
        }
    }

    private static ConverterProxyPlugin plugin(KeyConverter converter) throws Exception {
        ConverterProxyPlugin plugin = new ConverterProxyPlugin();
        Field field = ConverterProxyPlugin.class.getDeclaredField("converters");
        field.setAccessible(true);
        field.set(plugin, converters(converter));
        return plugin;
    }

    private static Converters converters(KeyConverter converter) {
        ConverterConfig config = new ConverterConfig();
        config.setKeyConverter(converter);
        return new Converters(config);
    }

    private static Command tenantCommand(Long bid, String bgroup, String... args) {
        Command command = command(args);
        command.getChannelInfo().setBid(bid);
        command.getChannelInfo().setBgroup(bgroup);
        return command;
    }

    private static Command command(String... args) {
        return new Command(Arrays.stream(args).map(Utils::stringToBytes).toArray(byte[][]::new));
    }

    private static BulkReply bulk(String value) {
        return new BulkReply(Utils.stringToBytes(value));
    }

    private static void arguments(Command command, String... expected) {
        Assert.assertEquals(Arrays.asList(expected), Arrays.stream(command.getObjects()).map(Utils::bytesToString).collect(Collectors.toList()));
    }

    private static class IdentityKeyConverter implements KeyConverter {
        @Override
        public byte[] convert(CommandContext context, RedisCommand command, byte[] key) {
            return key;
        }

        @Override
        public byte[] reverseConvert(CommandContext context, RedisCommand command, byte[] key) {
            return key;
        }
    }
}
