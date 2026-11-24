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

public class TimeSeriesConvertersTest {

    @Test
    public void singleKeyConversionLeavesSamplesLabelsAndBlockingOptionsIntact() {
        Converters converters = converters();
        String[][] requests = {
                {"TS.CREATE", "series", "LABELS", "region", "cn"},
                {"TS.ADD", "series", "*", "1.25", "ON_DUPLICATE", "BLOCK"},
                {"TS.RANGE", "series", "-", "+", "AGGREGATION", "AVG", "1000"},
                {"TS.READ", "series", "$", "BLOCK", "0", "1", "MAX_COUNT", "5"}
        };
        for (String[] args : requests) {
            Command command = command(args);
            converters.convertRequest(command);
            String[] expected = args.clone();
            expected[1] = "key:series";
            assertArguments(command, expected);
            converters.convertReply(command, StatusReply.OK);
            assertArguments(command, args);
        }
    }

    @Test
    public void multiKeyRequestsConvertOnlyTheDeclaredKeysAndRestoreThem() {
        Converters converters = converters();
        for (String[] args : new String[][]{
                {"TS.MADD", "a", "*", "1.1", "b", "10", "2.2"},
                {"TS.NRANGE", "2", "a", "b", "-", "+", "COUNT", "10"},
                {"TS.NREVRANGE", "2", "a", "b", "-", "+"},
                {"TS.CREATERULE", "a", "b", "AGGREGATION", "AVG", "1000"},
                {"TS.DELETERULE", "a", "b"}}) {
            Command command = command(args);
            command.getKeys();
            converters.convertRequest(command);
            Assert.assertEquals(Arrays.asList("key:a", "key:b"), command.getKeys().stream().map(Utils::bytesToString).collect(Collectors.toList()));
            for (int i = 1; i < args.length; i++) {
                String expected = args[i].equals("a") || args[i].equals("b") ? "key:" + args[i] : args[i];
                Assert.assertEquals(expected, Utils.bytesToString(command.getObjects()[i]));
            }
            converters.convertReply(command, MultiBulkReply.EMPTY);
            assertArguments(command, args);
        }
    }

    @Test
    public void queryIndexRestoresReturnedKeys() {
        MultiBulkReply reply = array(bulk("key:a"), bulk("key:b"));
        converters().convertReply(command("TS.QUERYINDEX", "region=cn"), reply);
        Assert.assertEquals("a", reply.getReplies()[0].toString());
        Assert.assertEquals("b", reply.getReplies()[1].toString());
    }

    @Test
    public void labelQueriesRestoreSeriesKeysWithoutChangingLabelsOrSamples() {
        for (String name : new String[]{"TS.MGET", "TS.MRANGE", "TS.MREVRANGE"}) {
            MultiBulkReply labels = array(array(bulk("region"), bulk("key:cn")));
            MultiBulkReply samples = array(array(bulk("123"), bulk("1.25")));
            MultiBulkReply entry = array(bulk("key:series"), labels, samples);
            Command query = name.equals("TS.MGET") ? command(name, "WITHLABELS", "FILTER", "region=cn")
                    : command(name, "-", "+", "WITHLABELS", "FILTER", "region=cn");
            converters().convertReply(query, array(entry));
            Assert.assertEquals("series", entry.getReplies()[0].toString());
            Assert.assertEquals("key:cn", ((MultiBulkReply) labels.getReplies()[0]).getReplies()[1].toString());
            Assert.assertEquals("1.25", ((MultiBulkReply) samples.getReplies()[0]).getReplies()[1].toString());
        }
    }

    @Test
    public void groupNamesAndSourceLabelsRemainUnchanged() {
        for (String name : new String[]{"TS.MRANGE", "TS.MREVRANGE"}) {
            MultiBulkReply labels = array(array(bulk("__source__"), bulk("key:a,key:b")));
            MultiBulkReply entry = array(bulk("region=cn"), labels, array(array(bulk("123"), bulk("3.5"))));
            converters().convertReply(command(name, "-", "+", "FILTER", "type=cpu", "GROUPBY", "region", "REDUCE", "SUM"), array(entry));
            Assert.assertEquals("region=cn", entry.getReplies()[0].toString());
            Assert.assertEquals("key:a,key:b", ((MultiBulkReply) labels.getReplies()[0]).getReplies()[1].toString());
        }
    }

    @Test
    public void infoRestoresSourceRuleTargetsAndDebugSelfKeyOnly() {
        Command command = command("TS.INFO", "series", "DEBUG");
        Converters converters = converters();
        converters.convertRequest(command);
        MultiBulkReply rule = array(bulk("key:destination"), bulk("1000"), bulk("AVG"), bulk("0"));
        MultiBulkReply reply = array(new StatusReply("sourceKey"), bulk("key:source"), new StatusReply("rules"), array(rule),
                new StatusReply("keySelfName"), bulk("key:series"), new StatusReply("labels"), array(array(bulk("tag"), bulk("key:value"))));
        converters.convertReply(command, reply);
        Assert.assertEquals("source", reply.getReplies()[1].toString());
        Assert.assertEquals("destination", rule.getReplies()[0].toString());
        Assert.assertEquals("series", reply.getReplies()[5].toString());
        Assert.assertEquals("1000", rule.getReplies()[1].toString());
        Assert.assertEquals("series", Utils.bytesToString(command.getObjects()[1]));
    }

    @Test
    public void nullRepliesAndLabelNamesDoNotInvokeReverseKeyConversion() {
        Converters converters = converters();
        Command info = command("TS.INFO", "s");
        converters.convertRequest(info);
        converters.convertReply(info, array(bulk("sourceKey"), BulkReply.NIL_REPLY, bulk("rules"), MultiBulkReply.EMPTY));
        converters.convertReply(command("TS.QUERYINDEX", "region=cn"), MultiBulkReply.NIL_REPLY);
        converters.convertReply(command("TS.MGET", "FILTER", "region=cn"), array(MultiBulkReply.NIL_REPLY));
        BulkReply label = bulk("region");
        converters.convertReply(command("TS.QUERYLABELS", "LABELS"), array(label));
        Assert.assertEquals("region", label.toString());
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
                String value = Utils.bytesToString(key);
                Assert.assertTrue(value, value.startsWith("key:"));
                return Utils.stringToBytes(value.substring(4));
            }
        });
        return new Converters(config);
    }

    private static BulkReply bulk(String value) {
        return new BulkReply(Utils.stringToBytes(value));
    }

    private static MultiBulkReply array(Reply... values) {
        return new MultiBulkReply(values);
    }

    private static Command command(String... args) {
        return new Command(Arrays.stream(args).map(Utils::stringToBytes).toArray(byte[][]::new));
    }

    private static void assertArguments(Command command, String... args) {
        Assert.assertEquals(Arrays.asList(args), Arrays.stream(command.getObjects()).map(Utils::bytesToString).collect(Collectors.toList()));
    }
}
