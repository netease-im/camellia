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

public class LegacyCollectionConvertersTest {
    @Test
    public void smoveConvertsSourceMemberAndBothKeys() {
        Command command = command("SMOVE", "source", "destination", "member");
        Converters converters = converters("k:source");
        converters.convertRequest(command);
        arguments(command, "SMOVE", "k:source", "k:destination", "v:member");
        converters.convertReply(command, new IntegerReply(1L));
        arguments(command, "SMOVE", "source", "destination", "v:member");
    }

    @Test
    public void setAlgebraDecodesMembersWithFirstSourceKey() {
        for (String name : new String[]{"SUNION", "SINTER", "SDIFF"}) {
            Command command = command(name, "source", "other");
            Converters converters = converters("k:source");
            converters.convertRequest(command);
            arguments(command, name, "k:source", "k:other");
            MultiBulkReply reply = array(bulk("v:a"), bulk("v:b"));
            converters.convertReply(command, reply);
            Assert.assertEquals("a", reply.getReplies()[0].toString());
            Assert.assertEquals("b", reply.getReplies()[1].toString());
            arguments(command, name, "source", "other");
        }
    }

    @Test
    public void emptySetAlgebraResultsDoNotInvokeDecoder() {
        for (String name : new String[]{"SUNION", "SINTER", "SDIFF"}) {
            Command command = command(name, "source");
            Converters converters = converters("k:source");
            converters.convertRequest(command);
            converters.convertReply(command, MultiBulkReply.EMPTY);
        }
    }

    @Test
    public void legacyListMovesDecodeWithSourceKeyAndRestoreKeys() {
        for (String[] args : listMoves()) {
            Converters converters = converters("k:source");
            Command command = command(args);
            converters.convertRequest(command);
            BulkReply reply = bulk("v:member");
            converters.convertReply(command, reply);
            Assert.assertEquals("member", reply.toString());
            arguments(command, args);
        }
    }

    @Test
    public void legacyListMoveTimeoutsDoNotDecodeNil() {
        for (String[] args : listMoves()) {
            Converters converters = converters("k:source");
            Command command = command(args);
            converters.convertRequest(command);
            converters.convertReply(command, BulkReply.NIL_REPLY);
            Assert.assertNull(BulkReply.NIL_REPLY.getRaw());
            arguments(command, args);
        }
    }

    private static String[][] listMoves() {
        return new String[][]{
                {"LMOVE", "source", "destination", "LEFT", "RIGHT"},
                {"BLMOVE", "source", "destination", "LEFT", "RIGHT", "0.01"},
                {"RPOPLPUSH", "source", "destination"},
                {"BRPOPLPUSH", "source", "destination", "0.01"}};
    }

    @Test
    public void multiListPopsDecodeWithActualReturnedKeyBeforeKeyRestoration() {
        for (String name : new String[]{"LMPOP", "BLMPOP"}) {
            Command command = multiPop(name);
            Converters converters = converters("k:second");
            converters.convertRequest(command);
            BulkReply key = bulk("k:second");
            MultiBulkReply elements = array(bulk("v:a"), bulk("v:b"));
            converters.convertReply(command, array(key, elements));
            Assert.assertEquals("second", key.toString());
            Assert.assertEquals("a", elements.getReplies()[0].toString());
            Assert.assertEquals("b", elements.getReplies()[1].toString());
            Assert.assertEquals(Arrays.asList("first", "second"),
                    command.getKeys().stream().map(Utils::bytesToString).collect(Collectors.toList()));
        }
    }

    @Test
    public void multiZsetPopsDecodeNestedMembersAndPreserveScores() {
        for (String name : new String[]{"ZMPOP", "BZMPOP"}) {
            Command command = multiPop(name);
            Converters converters = converters("k:second");
            converters.convertRequest(command);
            BulkReply key = bulk("k:second");
            BulkReply score = bulk("-1.25");
            MultiBulkReply pair = array(bulk("v:a"), score);
            converters.convertReply(command, array(key, array(pair)));
            Assert.assertEquals("second", key.toString());
            Assert.assertEquals("a", pair.getReplies()[0].toString());
            Assert.assertSame(score, pair.getReplies()[1]);
            Assert.assertEquals("-1.25", score.toString());
        }
    }

    @Test
    public void multiPopTimeoutsAndEmptyResultsDoNotDecodeValues() {
        for (String name : new String[]{"LMPOP", "BLMPOP", "ZMPOP", "BZMPOP"}) {
            for (Reply reply : new Reply[]{MultiBulkReply.NIL_REPLY, MultiBulkReply.EMPTY,
                    array(bulk("k:second"), MultiBulkReply.EMPTY)}) {
                Command command = multiPop(name);
                Converters converters = converters("k:second");
                converters.convertRequest(command);
                converters.convertReply(command, reply);
            }
        }
    }

    private static Command multiPop(String name) {
        String side = name.contains("ZMPOP") ? "MIN" : "LEFT";
        if (name.startsWith("B")) {
            return command(name, "0.01", "2", "first", "second", side, "COUNT", "2");
        }
        return command(name, "2", "first", "second", side, "COUNT", "2");
    }

    @Test
    public void zsetAlgebraDecodesMembersWithFirstSourceAndPreservesScores() {
        for (String name : new String[]{"ZUNION", "ZINTER", "ZDIFF"}) {
            for (boolean withScores : new boolean[]{false, true}) {
                Command command = withScores ? command(name, "2", "source", "other", "WITHSCORES")
                        : command(name, "2", "source", "other");
                Converters converters = converters("k:source");
                converters.convertRequest(command);
                BulkReply member = bulk("v:a");
                BulkReply score = bulk("1.25");
                MultiBulkReply reply = withScores ? array(member, score) : array(member);
                converters.convertReply(command, reply);
                Assert.assertEquals("a", member.toString());
                if (withScores) {
                    Assert.assertSame(score, reply.getReplies()[1]);
                    Assert.assertEquals("1.25", score.toString());
                }
            }
        }
    }

    @Test
    public void zsetSourceNamedWithScoresIsNotAnOptionWithoutKeyConversion() {
        ConverterConfig config = new ConverterConfig();
        config.setzSetConverter(new Values("WITHSCORES"));
        Converters converters = new Converters(config);
        MultiBulkReply reply = array(bulk("v:a"));
        converters.convertReply(command("ZUNION", "1", "WITHSCORES"), reply);
        Assert.assertEquals("a", reply.getReplies()[0].toString());
    }

    private static Converters converters(String expectedKey) {
        ConverterConfig config = new ConverterConfig();
        config.setKeyConverter(new KeyConverter() {
            @Override
            public byte[] convert(CommandContext context, RedisCommand command, byte[] key) {
                return Utils.stringToBytes("k:" + Utils.bytesToString(key));
            }

            @Override
            public byte[] reverseConvert(CommandContext context, RedisCommand command, byte[] key) {
                return decode("k:", key);
            }
        });
        Values values = new Values(expectedKey);
        config.setSetConverter(values);
        config.setListConverter(values);
        config.setzSetConverter(values);
        return new Converters(config);
    }

    private static class Values implements SetConverter, ListConverter, ZSetConverter {
        private final String expectedKey;

        private Values(String expectedKey) {
            this.expectedKey = expectedKey;
        }

        @Override
        public byte[] valueConvert(CommandContext context, byte[] key, byte[] value) {
            Assert.assertEquals(expectedKey, Utils.bytesToString(key));
            Assert.assertNotNull(value);
            return Utils.stringToBytes("v:" + Utils.bytesToString(value));
        }

        @Override
        public byte[] valueReverseConvert(CommandContext context, byte[] key, byte[] value) {
            Assert.assertEquals(expectedKey, Utils.bytesToString(key));
            return decode("v:", value);
        }
    }

    private static byte[] decode(String prefix, byte[] value) {
        Assert.assertNotNull(value);
        String raw = Utils.bytesToString(value);
        Assert.assertTrue(raw, raw.startsWith(prefix));
        return Utils.stringToBytes(raw.substring(prefix.length()));
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

    private static void arguments(Command command, String... expected) {
        Assert.assertEquals(Arrays.asList(expected), Arrays.stream(command.getObjects()).map(Utils::bytesToString).collect(Collectors.toList()));
    }
}
