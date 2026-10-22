package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.plugin.converter.ConverterConfig;
import com.netease.nim.camellia.redis.proxy.plugin.converter.Converters;
import com.netease.nim.camellia.redis.proxy.plugin.converter.KeyConverter;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class CopyCommandTest {

    @Test
    public void shouldRegisterCopyAsRestrictiveWriteCommand() {
        RedisCommand command = RedisCommand.getSupportRedisCommandByName("copy");
        Assert.assertSame(RedisCommand.COPY, command);
        Assert.assertEquals(RedisCommand.CommandSupportType.RESTRICTIVE_SUPPORT, command.getSupportType());
        Assert.assertEquals(RedisCommand.Type.WRITE, command.getType());
        Assert.assertEquals(RedisCommand.CommandType.DB, command.getCommandType());
        Assert.assertEquals(RedisCommand.CommandKeyType.COMPLEX, command.getCommandKeyType());
    }

    @Test
    public void shouldParseOnlySourceAndDestinationKeys() {
        assertKeys(command("COPY", "source", "destination"), "source", "destination");
        assertKeys(command("COPY", "source", "destination", "DB", "1"), "source", "destination");
        assertKeys(command("COPY", "source", "destination", "REPLACE"), "source", "destination");
        assertKeys(command("COPY", "source", "destination", "DB", "1", "REPLACE"), "source", "destination");
    }

    @Test
    public void shouldConvertOnlySourceAndDestinationKeys() {
        ConverterConfig config = new ConverterConfig();
        config.setKeyConverter(new PrefixKeyConverter("namespace:"));
        Converters converters = new Converters(config);
        Command command = command("COPY", "source", "destination", "DB", "1", "REPLACE");

        converters.convertRequest(command);

        List<byte[]> keys = command.getKeys();
        Assert.assertEquals(2, keys.size());
        Assert.assertArrayEquals(bytes("namespace:source"), keys.get(0));
        Assert.assertArrayEquals(bytes("namespace:destination"), keys.get(1));
        Assert.assertArrayEquals(bytes("DB"), command.getObjects()[3]);
        Assert.assertArrayEquals(bytes("1"), command.getObjects()[4]);
        Assert.assertArrayEquals(bytes("REPLACE"), command.getObjects()[5]);
    }

    private static void assertKeys(Command command, String... expectedKeys) {
        List<byte[]> keys = command.getKeys();
        Assert.assertEquals(expectedKeys.length, keys.size());
        for (int i = 0; i < expectedKeys.length; i++) {
            Assert.assertArrayEquals(bytes(expectedKeys[i]), keys.get(i));
        }
    }

    private static Command command(String... args) {
        byte[][] objects = new byte[args.length][];
        for (int i = 0; i < args.length; i++) {
            objects[i] = bytes(args[i]);
        }
        return new Command(objects);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static class PrefixKeyConverter implements KeyConverter {
        private final byte[] prefix;

        private PrefixKeyConverter(String prefix) {
            this.prefix = bytes(prefix);
        }

        @Override
        public byte[] convert(CommandContext commandContext, RedisCommand redisCommand, byte[] originalKey) {
            byte[] convertedKey = Arrays.copyOf(prefix, prefix.length + originalKey.length);
            System.arraycopy(originalKey, 0, convertedKey, prefix.length, originalKey.length);
            return convertedKey;
        }

        @Override
        public byte[] reverseConvert(CommandContext commandContext, RedisCommand redisCommand, byte[] convertedKey) {
            return Arrays.copyOfRange(convertedKey, prefix.length, convertedKey.length);
        }
    }
}
