package com.netease.nim.camellia.redis.proxy.plugin.converter;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LegacyCommandConvertersTest {

    @Test
    public void hashIncrementsPreserveNumericDeltasAndStrlenConvertsField() {
        Converters converters = converters();
        for (String name : new String[]{"HINCRBY", "HINCRBYFLOAT"}) {
            Command command = command(name, "hash", "field", "-1");
            converters.convertRequest(command);
            arguments(command, name, "k:hash", "f:field", "-1");
        }
        Command command = command("HSTRLEN", "hash", "field");
        converters.convertRequest(command);
        arguments(command, "HSTRLEN", "k:hash", "f:field");
    }

    @Test
    public void multiFieldHashCommandsStillConvertEveryField() {
        for (String name : new String[]{"HDEL", "HMGET"}) {
            Command command = command(name, "hash", "a", "b");
            converters().convertRequest(command);
            arguments(command, name, "k:hash", "f:a", "f:b");
        }
    }

    @Test
    public void setGetDecodesOldValueAndPreservesStatusAndNilReplies() {
        for (String[] options : new String[][]{{"GET"}, {"NX", "GET"}, {"GET", "XX"}}) {
            String[] args = new String[3 + options.length];
            args[0] = "SET";
            args[1] = "string";
            args[2] = "new";
            System.arraycopy(options, 0, args, 3, options.length);
            Converters converters = converters();
            Command command = command(args);
            converters.convertRequest(command);
            Assert.assertEquals("v:new", Utils.bytesToString(command.getObjects()[2]));
            BulkReply reply = bulk("v:old");
            converters.convertReply(command, reply);
            Assert.assertEquals("old", reply.toString());
            Assert.assertEquals("string", Utils.bytesToString(command.getObjects()[1]));
        }
        for (Reply reply : new Reply[]{StatusReply.OK, BulkReply.NIL_REPLY}) {
            Command command = command("SET", "string", "new");
            Converters converters = converters();
            converters.convertRequest(command);
            converters.convertReply(command, reply);
        }
        Assert.assertNull(BulkReply.NIL_REPLY.getRaw());
    }

    @Test
    public void randomFieldsDecodeFieldValuePairsWithDistinctConverters() {
        Command command = command("HRANDFIELD", "hash", "-2", "withvalues");
        Converters converters = converters();
        converters.convertRequest(command);
        MultiBulkReply reply = array(bulk("f:a"), bulk("v:A"), bulk("f:b"), bulk("v:B"));
        converters.convertReply(command, reply);
        values(reply, "a", "A", "b", "B");
    }

    @Test
    public void randomFieldsWithoutValuesAndMissingKeysRemainValid() {
        for (Reply reply : new Reply[]{bulk("f:a"), array(bulk("f:a")), MultiBulkReply.EMPTY, BulkReply.NIL_REPLY}) {
            Command command = command("HRANDFIELD", "hash", "1");
            Converters converters = converters();
            converters.convertRequest(command);
            converters.convertReply(command, reply);
            if (reply instanceof BulkReply && ((BulkReply) reply).getRaw() != null) {
                Assert.assertEquals("a", reply.toString());
            } else if (reply instanceof MultiBulkReply && ((MultiBulkReply) reply).getReplies().length > 0) {
                values((MultiBulkReply) reply, "a");
            }
        }
    }

    @Test
    public void scanNoValuesDecodesOddAndEvenNumbersOfFields() {
        for (int count : new int[]{0, 1, 2, 3}) {
            Reply[] fields = new Reply[count];
            for (int i = 0; i < count; i++) {
                fields[i] = bulk("f:" + i);
            }
            Command command = command("HSCAN", "hash", "0", "MATCH", "*", "COUNT", "100", "novalues");
            Converters converters = converters();
            converters.convertRequest(command);
            MultiBulkReply entries = array(fields);
            BulkReply cursor = bulk("0");
            converters.convertReply(command, array(cursor, entries));
            Assert.assertEquals("0", cursor.toString());
            for (int i = 0; i < count; i++) {
                Assert.assertEquals(String.valueOf(i), entries.getReplies()[i].toString());
            }
        }
    }

    @Test
    public void scanMatchPatternNamedNoValuesIsNotAnOption() {
        Command command = command("HSCAN", "hash", "0", "MATCH", "NOVALUES", "COUNT", "10");
        Converters converters = converters();
        converters.convertRequest(command);
        MultiBulkReply entries = array(bulk("f:a"), bulk("v:A"));
        converters.convertReply(command, array(bulk("0"), entries));
        values(entries, "a", "A");
    }

    @Test
    public void rankPreservesWithScoreAndOnlyConvertsTheMember() {
        for (String name : new String[]{"ZRANK", "ZREVRANK"}) {
            Command command = command(name, "zset", "WITHSCORE", "withscore");
            Converters converters = converters();
            converters.convertRequest(command);
            arguments(command, name, "k:zset", "v:WITHSCORE", "withscore");
            MultiBulkReply reply = array(new IntegerReply(0L), bulk("1.5"));
            converters.convertReply(command, reply);
            Assert.assertEquals("1.5", reply.getReplies()[1].toString());
        }
        for (String name : new String[]{"ZREM", "ZMSCORE"}) {
            Command command = command(name, "zset", "a", "WITHSCORE");
            converters().convertRequest(command);
            arguments(command, name, "k:zset", "v:a", "v:WITHSCORE");
        }
    }

    @Test
    public void storeDestinationsConvertRestoreAndParticipateInRouting() {
        for (String[] args : new String[][]{
                {"SORT", "source", "BY", "nosort", "GET", "#", "LIMIT", "0", "2", "store", "destination"},
                {"GEORADIUS", "source", "0", "0", "1", "km", "COUNT", "1", "ANY", "store", "destination"},
                {"GEORADIUSBYMEMBER", "source", "STORE", "1", "km", "STOREDIST", "destination"}}) {
            Command command = command(args);
            Assert.assertEquals(Arrays.asList("source", "destination"), keys(command));
            Assert.assertEquals(RedisCommand.Type.WRITE, command.getRedisCommand().getType());
            Assert.assertEquals(RedisCommand.CommandSupportType.RESTRICTIVE_SUPPORT, command.getRedisCommand().getSupportType());
            Converters converters = converters();
            converters.convertRequest(command);
            String[] expected = args.clone();
            expected[1] = "k:source";
            expected[expected.length - 1] = "k:destination";
            arguments(command, expected);
            Assert.assertEquals(Arrays.asList("k:source", "k:destination"), keys(command));
            converters.convertReply(command, new IntegerReply(1L));
            arguments(command, args);
            Assert.assertEquals(Arrays.asList("source", "destination"), keys(command));
        }
    }

    @Test
    public void sortPatternsAndGeoMembersNamedStoreAreNotDestinations() {
        for (String[] args : new String[][]{
                {"SORT", "source", "BY", "STORE", "GET", "STORE", "LIMIT", "0", "1"},
                {"SORT", "source", "BY", "nosort", "GET", "#"},
                {"GEORADIUSBYMEMBER", "source", "STORE", "1", "km"},
                {"GEORADIUS", "source", "0", "0", "1", "km", "WITHDIST"},
                {"SORT_RO", "source", "BY", "nosort", "GET", "#"}}) {
            Command command = command(args);
            Converters converters = converters();
            converters.convertRequest(command);
            String[] expected = args.clone();
            expected[1] = "k:source";
            arguments(command, expected);
            Assert.assertEquals(Arrays.asList("k:source"), keys(command));
            converters.convertReply(command, MultiBulkReply.EMPTY);
            arguments(command, args);
        }
    }

    @Test
    public void malformedRequestsDoNotIndexPastArguments() {
        for (String[] args : new String[][]{
                {"SORT"}, {"SORT", "source", "STORE"}, {"SORT", "source", "BY"},
                {"GEORADIUS", "source"}, {"GEORADIUSBYMEMBER", "source", "a", "1", "km", "STORE"},
                {"HINCRBY"}, {"HINCRBYFLOAT", "hash"}, {"HSTRLEN"}, {"ZRANK"}, {"ZREVRANK", "zset"}}) {
            converters().convertRequest(command(args));
        }
    }

    private static Converters converters() {
        ConverterConfig config = new ConverterConfig();
        config.setKeyConverter(new KeyConverter() {
            @Override
            public byte[] convert(CommandContext context, RedisCommand command, byte[] key) {
                return encode("k:", key);
            }

            @Override
            public byte[] reverseConvert(CommandContext context, RedisCommand command, byte[] key) {
                return decode("k:", key);
            }
        });
        Values values = new Values();
        config.setHashConverter(values);
        config.setStringConverter(values);
        config.setzSetConverter(values);
        return new Converters(config);
    }

    private static class Values implements HashConverter, StringConverter, ZSetConverter {
        @Override
        public byte[] fieldConvert(CommandContext context, byte[] key, byte[] field) {
            return encode("f:", field);
        }

        @Override
        public byte[] fieldReverseConvert(CommandContext context, byte[] key, byte[] field) {
            return decode("f:", field);
        }

        @Override
        public byte[] valueConvert(CommandContext context, byte[] key, byte[] value) {
            return encode("v:", value);
        }

        @Override
        public byte[] valueReverseConvert(CommandContext context, byte[] key, byte[] value) {
            return decode("v:", value);
        }
    }

    private static byte[] encode(String prefix, byte[] value) {
        Assert.assertNotNull(value);
        return Utils.stringToBytes(prefix + Utils.bytesToString(value));
    }

    private static byte[] decode(String prefix, byte[] value) {
        Assert.assertNotNull(value);
        String raw = Utils.bytesToString(value);
        Assert.assertTrue(raw, raw.startsWith(prefix));
        return Utils.stringToBytes(raw.substring(prefix.length()));
    }

    private static List<String> keys(Command command) {
        return command.getKeys().stream().map(Utils::bytesToString).collect(Collectors.toList());
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

    private static void values(MultiBulkReply reply, String... expected) {
        Assert.assertEquals(Arrays.asList(expected), Arrays.stream(reply.getReplies()).map(Object::toString).collect(Collectors.toList()));
    }

    private static void arguments(Command command, String... expected) {
        Assert.assertEquals(Arrays.asList(expected), Arrays.stream(command.getObjects()).map(Utils::bytesToString).collect(Collectors.toList()));
    }
}
