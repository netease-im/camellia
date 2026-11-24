package com.netease.nim.camellia.redis.proxy.plugin.converter;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ConditionalSetConvertersTest {
    @Test
    public void conditionalValuesConvertOnceAndOptionsRemainUnchanged() {
        for (String condition : new String[]{"IFEQ", "ifne"}) {
            Command command = command("SET", "key", "new", "PX", "1000", condition, "IFEQ", "GET");
            converters().convertRequest(command);
            arguments(command, "SET", "key", "v:new", "PX", "1000", condition, "v:IFEQ", "GET");
        }
    }

    @Test
    public void digestAndExpiryArgumentsAreNeverConverted() {
        for (String condition : new String[]{"IFDEQ", "IFDNE"}) {
            Command command = command("SET", "key", "new", condition, "IFEQ", "PXAT", "1000", "GET");
            converters().convertRequest(command);
            arguments(command, "SET", "key", "v:new", condition, "IFEQ", "PXAT", "1000", "GET");
        }
    }

    @Test
    public void missingConditionValueDoesNotThrow() {
        Command command = command("SET", "key", "new", "IFEQ");
        converters().convertRequest(command);
        arguments(command, "SET", "key", "v:new", "IFEQ");
    }

    private static Converters converters() {
        ConverterConfig config = new ConverterConfig();
        config.setStringConverter(new StringConverter() {
            @Override
            public byte[] valueConvert(CommandContext context, byte[] key, byte[] value) {
                Assert.assertEquals("key", Utils.bytesToString(key));
                return Utils.stringToBytes("v:" + Utils.bytesToString(value));
            }

            @Override
            public byte[] valueReverseConvert(CommandContext context, byte[] key, byte[] value) {
                throw new AssertionError("Request conversion must not decode values");
            }
        });
        return new Converters(config);
    }

    private static Command command(String... args) {
        return new Command(Arrays.stream(args).map(Utils::stringToBytes).toArray(byte[][]::new));
    }

    private static void arguments(Command command, String... expected) {
        Assert.assertEquals(Arrays.asList(expected), Arrays.stream(command.getObjects()).map(Utils::bytesToString).collect(Collectors.toList()));
    }
}
