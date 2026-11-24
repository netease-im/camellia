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
import java.util.stream.Collectors;

public class NewCommandConvertersTest {

    @Test
    public void countedKeyConversionPreservesOptionsAndRestoresKeys() {
        Converters converters = converters();
        for (String name : new String[]{"CMS.MERGE", "TDIGEST.MERGE"}) {
            String[] options = name.equals("CMS.MERGE")
                    ? new String[]{"WEIGHTS", "1", "2"} : new String[]{"COMPRESSION", "100", "OVERRIDE"};
            Command command = command(name, "dest", "2", "source1", "source2", options[0], options[1], options[2]);
            command.getKeys(); // Populate the cache before conversion.
            converters.convertRequest(command);
            assertArguments(command, name, "key:dest", "2", "key:source1", "key:source2", options[0], options[1], options[2]);
            Assert.assertEquals(Arrays.asList("key:dest", "key:source1", "key:source2"),
                    command.getKeys().stream().map(Utils::bytesToString).collect(Collectors.toList()));
            converters.convertReply(command, new MultiBulkReply(new Reply[0]));
            assertArguments(command, name, "dest", "2", "source1", "source2", options[0], options[1], options[2]);
        }
        for (String name : new String[]{"SDIFFCARD", "SUNIONCARD"}) {
            Command command = command(name, "2", "a", "b", "LIMIT", "10");
            converters.convertRequest(command);
            assertArguments(command, name, "2", "key:a", "key:b", "LIMIT", "10");
        }
    }

    @Test
    public void msetexConvertsOnlyCountedKeyValuePairs() {
        Command command = command("MSETEX", "2", "a", "EX", "b", "NX", "EX", "10", "NX");
        Converters converters = converters();
        converters.convertRequest(command);
        assertArguments(command, "MSETEX", "2", "key:a", "value:EX", "key:b", "value:NX", "EX", "10", "NX");
        converters.convertReply(command, new BulkReply(Utils.stringToBytes("OK")));
        assertArguments(command, "MSETEX", "2", "a", "value:EX", "b", "value:NX", "EX", "10", "NX");
    }

    @Test
    public void jsonMsetConvertsKeysWithoutChangingPathsOrDocuments() {
        Command command = command("JSON.MSET", "a", "$.a", "{\"a\":1}", "b", "$", "[]");
        converters().convertRequest(command);
        assertArguments(command, "JSON.MSET", "key:a", "$.a", "{\"a\":1}", "key:b", "$", "[]");
    }

    @Test
    public void hashExpirationConvertsFieldsAfterOptionsAndCount() {
        Converters converters = converters();
        for (String name : new String[]{"HEXPIRE", "HPEXPIRE", "HEXPIREAT", "HPEXPIREAT"}) {
            Command command = command(name, "h", "100", "NX", "FIELDS", "2", "FIELDS", "EX");
            converters.convertRequest(command);
            assertArguments(command, name, "key:h", "100", "NX", "FIELDS", "2", "field:FIELDS", "field:EX");
        }
        for (String name : new String[]{"HPERSIST", "HTTL", "HPTTL", "HEXPIRETIME", "HPEXPIRETIME"}) {
            Command command = command(name, "h", "FIELDS", "1", "FIELDS");
            converters.convertRequest(command);
            assertArguments(command, name, "key:h", "FIELDS", "1", "field:FIELDS");
        }
    }

    @Test
    public void hsetexConvertsFieldValuePairsAndNotExpirationOptions() {
        Command command = command("HSETEX", "h", "FNX", "PX", "100", "FIELDS", "2", "FIELDS", "EX", "b", "FIELDS");
        converters().convertRequest(command);
        assertArguments(command, "HSETEX", "key:h", "FNX", "PX", "100", "FIELDS", "2", "field:FIELDS", "value:EX", "field:b", "value:FIELDS");
    }

    @Test
    public void hashGetRepliesDecodeValuesAndPreserveNulls() {
        Converters converters = converters();
        for (String name : new String[]{"HGETEX", "HGETDEL"}) {
            Command command = name.equals("HGETEX")
                    ? command(name, "h", "PX", "100", "FIELDS", "2", "a", "b")
                    : command(name, "h", "FIELDS", "2", "a", "b");
            converters.convertRequest(command);
            Assert.assertEquals("field:a", Utils.bytesToString(command.getObjects()[command.getObjects().length - 2]));
            MultiBulkReply reply = new MultiBulkReply(new Reply[]{new BulkReply(Utils.stringToBytes("value:hello")), BulkReply.NIL_REPLY});
            converters.convertReply(command, reply);
            Assert.assertEquals("hello", reply.getReplies()[0].toString());
            Assert.assertNull(((BulkReply) reply.getReplies()[1]).getRaw());
            Assert.assertEquals("h", Utils.bytesToString(command.getObjects()[1]));
        }
    }

    @Test
    public void setExpirationConvertsMembersWithoutChangingOptions() {
        Converters converters = converters();
        for (String name : new String[]{"SEXPIRE", "SPEXPIRE", "SEXPIREAT", "SPEXPIREAT"}) {
            Command command = command(name, "s", "100", "GT", "MEMBERS", "2", "MEMBERS", "EX");
            converters.convertRequest(command);
            assertArguments(command, name, "key:s", "100", "GT", "MEMBERS", "2", "value:MEMBERS", "value:EX");
        }
        for (String name : new String[]{"SPERSIST", "STTL", "SPTTL", "SEXPIRETIME", "SPEXPIRETIME"}) {
            Command command = command(name, "s", "MEMBERS", "1", "MEMBERS");
            converters.convertRequest(command);
            assertArguments(command, name, "key:s", "MEMBERS", "1", "value:MEMBERS");
        }
        Command saddex = command("SADDEX", "s", "MNX", "PX", "100", "MEMBERS", "1", "EX");
        converters.convertRequest(saddex);
        assertArguments(saddex, "SADDEX", "key:s", "MNX", "PX", "100", "MEMBERS", "1", "value:EX");
    }

    @Test
    public void delexConvertsValueComparisonsButLeavesDigestsUnchanged() {
        for (String condition : new String[]{"IFEQ", "IFNE", "IFDEQ", "IFDNE"}) {
            Command command = command("DELEX", "s", condition, "v");
            converters().convertRequest(command);
            assertArguments(command, "DELEX", "key:s", condition, condition.startsWith("IFD") ? "v" : "value:v");
        }
    }

    @Test
    public void malformedCountsDoNotPartiallyConvertCountedData() {
        for (String count : new String[]{"-1", "x", "2147483648", "9223372036854775808"}) {
            Command msetex = command("MSETEX", count, "a", "v");
            converters().convertRequest(msetex);
            assertArguments(msetex, "MSETEX", count, "a", "v");
            Command hsetex = command("HSETEX", "h", "FIELDS", count, "a", "v");
            converters().convertRequest(hsetex);
            assertArguments(hsetex, "HSETEX", "key:h", "FIELDS", count, "a", "v");
            Command saddex = command("SADDEX", "s", "MEMBERS", count, "a");
            converters().convertRequest(saddex);
            assertArguments(saddex, "SADDEX", "key:s", "MEMBERS", count, "a");
        }
    }

    private static Converters converters() {
        ConverterConfig config = new ConverterConfig();
        config.setKeyConverter(new KeyConverter() {
            public byte[] convert(CommandContext context, RedisCommand command, byte[] key) {
                return prefix("key:", key);
            }
            public byte[] reverseConvert(CommandContext context, RedisCommand command, byte[] key) {
                return unprefix("key:", key);
            }
        });
        config.setStringConverter(new StringConverter() {
            public byte[] valueConvert(CommandContext context, byte[] key, byte[] value) {
                return prefix("value:", value);
            }
            public byte[] valueReverseConvert(CommandContext context, byte[] key, byte[] value) {
                return unprefix("value:", value);
            }
        });
        config.setHashConverter(new HashConverter() {
            public byte[] valueConvert(CommandContext context, byte[] key, byte[] value) {
                return prefix("value:", value);
            }
            public byte[] valueReverseConvert(CommandContext context, byte[] key, byte[] value) {
                return unprefix("value:", value);
            }
            public byte[] fieldConvert(CommandContext context, byte[] key, byte[] value) {
                return prefix("field:", value);
            }
            public byte[] fieldReverseConvert(CommandContext context, byte[] key, byte[] value) {
                return unprefix("field:", value);
            }
        });
        config.setSetConverter(new SetConverter() {
            public byte[] valueConvert(CommandContext context, byte[] key, byte[] value) {
                return prefix("value:", value);
            }
            public byte[] valueReverseConvert(CommandContext context, byte[] key, byte[] value) {
                return unprefix("value:", value);
            }
        });
        return new Converters(config);
    }

    private static byte[] prefix(String prefix, byte[] value) {
        return Utils.stringToBytes(prefix + Utils.bytesToString(value));
    }

    private static byte[] unprefix(String prefix, byte[] value) {
        String str = Utils.bytesToString(value);
        Assert.assertTrue(str.startsWith(prefix));
        return Utils.stringToBytes(str.substring(prefix.length()));
    }

    private static Command command(String... args) {
        return new Command(Arrays.stream(args).map(Utils::stringToBytes).toArray(byte[][]::new));
    }

    private static void assertArguments(Command command, String... expected) {
        Assert.assertEquals(Arrays.asList(expected), Arrays.stream(command.getObjects()).map(Utils::bytesToString).collect(Collectors.toList()));
    }
}
