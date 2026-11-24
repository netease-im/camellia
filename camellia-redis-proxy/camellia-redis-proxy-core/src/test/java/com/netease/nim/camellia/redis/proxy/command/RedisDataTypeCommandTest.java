package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

public class RedisDataTypeCommandTest {

    @Test
    public void dottedCommandNamesPreserveUnderscoresInOperations() {
        Command command = command("TDIGEST.TRIMMED_MEAN", "digest", "0.1", "0.9");
        Assert.assertEquals(RedisCommand.TDIGEST_TRIMMED_MEAN, command.getRedisCommand());
        Assert.assertEquals("tdigest.trimmed_mean", command.getRedisCommand().strRaw());
        Assert.assertArrayEquals(Utils.stringToBytes("tdigest.trimmed_mean"), command.getRedisCommand().raw());
        assertKeys(command, "digest");
    }

    @Test
    public void arrayAndVectorArgumentsAreNotAdditionalKeys() {
        assertKeys(command("ARMSET", "array", "0", "a", "100", "b"), "array");
        assertKeys(command("VADD", "vectors", "VALUES", "2", "0.1", "0.2", "element"), "vectors");
        assertKeys(command("CMS.INCRBY", "sketch", "a", "1", "b", "2"), "sketch");
        assertKeys(command("TOPK.ADD", "top", "a", "b"), "top");
    }

    @Test
    public void msetexKeysExcludeValuesAndExpirationOptions() {
        Command command = command("MSETEX", "2", "{user}:a", "EX", "{user}:b", "NX", "PX", "1000", "NX");
        assertKeys(command, "{user}:a", "{user}:b");
        assertRestricted(command, RedisCommand.Type.WRITE);
    }

    @Test
    public void mergeIncludesDestinationAndOnlyCountedSources() {
        Command cms = command("CMS.MERGE", "dest", "2", "source1", "source2", "WEIGHTS", "1", "2");
        assertKeys(cms, "dest", "source1", "source2");
        assertRestricted(cms, RedisCommand.Type.WRITE);
        Command digest = command("TDIGEST.MERGE", "dest", "2", "source1", "source2", "COMPRESSION", "100", "OVERRIDE");
        assertKeys(digest, "dest", "source1", "source2");
        assertRestricted(digest, RedisCommand.Type.WRITE);
    }

    @Test
    public void jsonMsetKeysExcludePathsAndJsonValues() {
        Command command = command("JSON.MSET", "a", "$.path", "{\"a\":1}", "b", "$", "[]");
        assertKeys(command, "a", "b");
        assertRestricted(command, RedisCommand.Type.WRITE);
    }

    @Test
    public void setCardinalityKeysExcludeCountAndLimit() {
        for (String name : new String[]{"SDIFFCARD", "SUNIONCARD"}) {
            Command command = command(name, "2", "a", "b", "LIMIT", "10");
            assertKeys(command, "a", "b");
            assertRestricted(command, RedisCommand.Type.READ);
        }
    }

    @Test
    public void malformedCountsDoNotOverflowOrIndexOutsideArguments() {
        for (String count : new String[]{"0", "-1", "not-an-integer", "2147483648", "9223372036854775807", "9223372036854775808"}) {
            assertKeys(command("MSETEX", count, "a", "v"));
            assertKeys(command("CMS.MERGE", "dest", count, "source"));
            assertKeys(command("TDIGEST.MERGE", "dest", count, "source"));
            assertKeys(command("SDIFFCARD", count, "a"));
            assertKeys(command("SUNIONCARD", count, "a"));
        }
        assertKeys(command("MSETEX", "1", "a"));
        assertKeys(command("JSON.MSET", "a", "$"));
        assertKeys(command("CMS.MERGE"));
    }

    @Test
    public void hashGetWithSideEffectsUsesWriteRouting() {
        for (String name : new String[]{"HGETEX", "HGETDEL"}) {
            Command command = command(name, "hash", "FIELDS", "2", "a", "b");
            Assert.assertEquals(RedisCommand.Type.WRITE, command.getRedisCommand().getType());
            Assert.assertFalse(command.isBlocking());
            assertKeys(command, "hash");
        }
    }

    @Test
    public void moveCommandsUseOnlySourceAndDestinationAsKeys() {
        String[][] requests = {
                {"LMOVEM", "source", "destination", "LEFT", "RIGHT", "COUNT", "2", "BULK"},
                {"BLMOVEM", "source", "destination", "RIGHT", "LEFT", "0.5", "EXACTLY", "2", "OBO"},
                {"ZMOVE", "source", "destination", "BYSCORE", "-inf", "+inf", "COUNT", "2"},
                {"BZMOVE", "source", "destination", "MAX", "0.5", "COUNT", "2"},
                {"ZMOVEMEMBER", "source", "destination", "MEMBERS", "2", "NX", "MEMBERS", "XX"}
        };
        for (String[] args : requests) {
            Command command = command(args);
            assertKeys(command, "source", "destination");
            assertRestricted(command, RedisCommand.Type.WRITE);
        }
    }

    @Test
    public void blockingMovesRemainBlockingWithZeroOrFiniteTimeout() {
        for (String timeout : new String[]{"0", "0.5"}) {
            Assert.assertTrue(command("BLMOVEM", "s", "d", "LEFT", "RIGHT", timeout).isBlocking());
            Assert.assertTrue(command("BZMOVE", "s", "d", "MIN", timeout).isBlocking());
        }
        Assert.assertFalse(command("LMOVEM", "s", "d", "LEFT", "RIGHT").isBlocking());
        Assert.assertFalse(command("ZMOVE", "s", "d", "MIN").isBlocking());
        Assert.assertFalse(command("ZMOVEMEMBER", "s", "d", "MEMBERS", "1", "a").isBlocking());
    }

    private static void assertRestricted(Command command, RedisCommand.Type type) {
        Assert.assertEquals(RedisCommand.CommandSupportType.RESTRICTIVE_SUPPORT, command.getRedisCommand().getSupportType());
        Assert.assertEquals(type, command.getRedisCommand().getType());
    }

    private static void assertKeys(Command command, String... expected) {
        Assert.assertNotNull(command.getRedisCommand());
        Assert.assertEquals(Arrays.asList(expected), command.getKeys().stream().map(Utils::bytesToString).collect(Collectors.toList()));
    }

    private static Command command(String... args) {
        return new Command(Arrays.stream(args).map(Utils::stringToBytes).toArray(byte[][]::new));
    }
}
