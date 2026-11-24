package com.netease.nim.camellia.redis.proxy.plugin.converter;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MoveCommandConvertersTest {

    @Test
    public void zaddConvertsEachMemberExactlyOnceWithOrWithoutOptions() {
        Converters converters = converters(true);
        Command single = command("ZADD", "source", "1.25", "a");
        converters.convertRequest(single);
        assertArguments(single, "ZADD", "key:source", "1.25", "value:a");
        Command multiple = command("ZADD", "source", "NX", "1.25", "a", "2.5", "b");
        converters.convertRequest(multiple);
        assertArguments(multiple, "ZADD", "key:source", "NX", "1.25", "value:a", "2.5", "value:b");
    }

    @Test
    public void bothKeysConvertAndRestoreWithoutTouchingOtherArguments() {
        Converters converters = new Converters(config());
        for (String[] args : requests()) {
            Command command = command(args);
            command.getKeys();
            converters.convertRequest(command);
            String[] expected = args.clone();
            expected[1] = "key:source";
            expected[2] = "key:destination";
            assertArguments(command, expected);
            Assert.assertEquals(Arrays.asList("key:source", "key:destination"),
                    command.getKeys().stream().map(Utils::bytesToString).collect(Collectors.toList()));
            converters.convertReply(command, StatusReply.OK);
            assertArguments(command, args);
            Assert.assertEquals(Arrays.asList("source", "destination"),
                    command.getKeys().stream().map(Utils::bytesToString).collect(Collectors.toList()));
        }
    }

    @Test
    public void listMoveRepliesDecodeAllElementsUsingSourceKey() {
        Converters converters = converters(true);
        for (String[] args : requests()) {
            Command command = command(args);
            if (command.getRedisCommand().getCommandType() != RedisCommand.CommandType.LIST) {
                continue;
            }
            converters.convertRequest(command);
            MultiBulkReply reply = array(bulk("value:a"), bulk("value:b"));
            converters.convertReply(command, reply);
            Assert.assertEquals("a", reply.getReplies()[0].toString());
            Assert.assertEquals("b", reply.getReplies()[1].toString());
            Assert.assertEquals("source", Utils.bytesToString(command.getObjects()[1]));
        }
    }

    @Test
    public void zsetMoveRepliesDecodeNestedMembersAndPreserveScores() {
        Converters converters = converters(true);
        for (String[] args : requests()) {
            Command command = command(args);
            if (command.getRedisCommand().getCommandType() != RedisCommand.CommandType.ZSET) {
                continue;
            }
            converters.convertRequest(command);
            BulkReply score1 = bulk("-1.25");
            BulkReply score2 = bulk("inf");
            MultiBulkReply pair1 = array(bulk("value:a"), score1);
            MultiBulkReply pair2 = array(bulk("value:b"), score2);
            MultiBulkReply reply = array(pair1, pair2);
            converters.convertReply(command, reply);
            Assert.assertEquals("a", pair1.getReplies()[0].toString());
            Assert.assertEquals("b", pair2.getReplies()[0].toString());
            Assert.assertSame(score1, pair1.getReplies()[1]);
            Assert.assertSame(score2, pair2.getReplies()[1]);
            Assert.assertEquals("-1.25", score1.toString());
            Assert.assertEquals("inf", score2.toString());
        }
    }

    @Test
    public void zmovememberConvertsOnlyCountedMembersWithConditionsBeforeOrAfter() {
        Converters converters = converters(true);
        Command before = command("ZMOVEMEMBER", "source", "destination", "NX", "MEMBERS", "3", "NX", "XX", "MEMBERS");
        converters.convertRequest(before);
        assertArguments(before, "ZMOVEMEMBER", "key:source", "key:destination", "NX", "MEMBERS", "3", "value:NX", "value:XX", "value:MEMBERS");
        Command after = command("ZMOVEMEMBER", "source", "destination", "MEMBERS", "2", "NX", "MEMBERS", "XX");
        converters.convertRequest(after);
        assertArguments(after, "ZMOVEMEMBER", "key:source", "key:destination", "MEMBERS", "2", "value:NX", "value:MEMBERS", "XX");
    }

    @Test
    public void destinationNamedMembersIsNotAnOption() {
        Command command = command("ZMOVEMEMBER", "source", "MEMBERS", "MEMBERS", "1", "NX");
        converters(false).convertRequest(command);
        assertArguments(command, "ZMOVEMEMBER", "source", "MEMBERS", "MEMBERS", "1", "value:NX");
    }

    @Test
    public void nullOrEmptyMoveRepliesDoNotInvokeValueConverters() {
        Converters converters = converters(true);
        for (String[] args : requests()) {
            for (Reply reply : new Reply[]{MultiBulkReply.NIL_REPLY, MultiBulkReply.EMPTY, BulkReply.NIL_REPLY}) {
                Command command = command(args);
                converters.convertRequest(command);
                converters.convertReply(command, reply);
                Assert.assertEquals("source", Utils.bytesToString(command.getObjects()[1]));
                Assert.assertEquals("destination", Utils.bytesToString(command.getObjects()[2]));
            }
        }
    }

    @Test
    public void malformedMemberCountsDoNotConvertDataOrThrow() {
        Converters converters = converters(true);
        for (String count : new String[]{"0", "-1", "x", "2147483648", "9223372036854775808", "2"}) {
            Command command = command("ZMOVEMEMBER", "source", "destination", "MEMBERS", count, "a");
            converters.convertRequest(command);
            assertArguments(command, "ZMOVEMEMBER", "key:source", "key:destination", "MEMBERS", count, "a");
        }
        converters.convertRequest(command("ZMOVEMEMBER"));
    }

    private static ConverterConfig config() {
        ConverterConfig config = new ConverterConfig();
        config.setKeyConverter(new KeyConverter() {
            @Override
            public byte[] convert(CommandContext context, RedisCommand command, byte[] key) {
                return Utils.stringToBytes("key:" + Utils.bytesToString(key));
            }

            @Override
            public byte[] reverseConvert(CommandContext context, RedisCommand command, byte[] key) {
                String value = Utils.bytesToString(key);
                Assert.assertTrue(value.startsWith("key:"));
                return Utils.stringToBytes(value.substring(4));
            }
        });
        return config;
    }

    private static Converters converters(boolean convertKeys) {
        ConverterConfig config = config();
        if (!convertKeys) {
            config.setKeyConverter(null);
        }
        config.setListConverter(new ListConverter() {
            @Override
            public byte[] valueConvert(CommandContext context, byte[] key, byte[] value) {
                throw new AssertionError("List moves have no value arguments");
            }

            @Override
            public byte[] valueReverseConvert(CommandContext context, byte[] key, byte[] value) {
                return decode(key, value, convertKeys);
            }
        });
        config.setzSetConverter(new ZSetConverter() {
            @Override
            public byte[] valueConvert(CommandContext context, byte[] key, byte[] value) {
                Assert.assertEquals(convertKeys ? "key:source" : "source", Utils.bytesToString(key));
                return Utils.stringToBytes("value:" + Utils.bytesToString(value));
            }

            @Override
            public byte[] valueReverseConvert(CommandContext context, byte[] key, byte[] value) {
                return decode(key, value, convertKeys);
            }
        });
        return new Converters(config);
    }

    private static byte[] decode(byte[] key, byte[] value, boolean convertKeys) {
        Assert.assertEquals(convertKeys ? "key:source" : "source", Utils.bytesToString(key));
        String raw = Utils.bytesToString(value);
        Assert.assertTrue(raw.startsWith("value:"));
        return Utils.stringToBytes(raw.substring(6));
    }

    private static String[][] requests() {
        return new String[][]{
                {"LMOVEM", "source", "destination", "LEFT", "RIGHT", "COUNT", "2", "BULK"},
                {"BLMOVEM", "source", "destination", "RIGHT", "LEFT", "0.5", "EXACTLY", "2", "OBO"},
                {"ZMOVE", "source", "destination", "BYSCORE", "-inf", "+inf", "COUNT", "2"},
                {"BZMOVE", "source", "destination", "MAX", "0.5", "COUNT", "2"},
                {"ZMOVEMEMBER", "source", "destination", "NX", "MEMBERS", "1", "a"}
        };
    }

    private static Command command(String... args) {
        return new Command(Arrays.stream(args).map(Utils::stringToBytes).toArray(byte[][]::new));
    }

    private static BulkReply bulk(String value) {
        return new BulkReply(Utils.stringToBytes(value));
    }

    private static MultiBulkReply array(Reply... replies) {
        return new MultiBulkReply(replies);
    }

    private static void assertArguments(Command command, String... expected) {
        Assert.assertEquals(Arrays.asList(expected), Arrays.stream(command.getObjects()).map(Utils::bytesToString).collect(Collectors.toList()));
    }
}
