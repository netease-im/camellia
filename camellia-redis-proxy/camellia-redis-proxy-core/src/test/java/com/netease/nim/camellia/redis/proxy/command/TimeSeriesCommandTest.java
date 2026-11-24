package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

public class TimeSeriesCommandTest {

    @Test
    public void readOnlyBlocksWhenBlockOccursInTheOptions() {
        Assert.assertFalse(command("TS.READ", "BLOCK", "0").isBlocking());
        Assert.assertFalse(command("TS.READ", "series", "0", "MAX_COUNT", "10").isBlocking());
        Assert.assertTrue(command("TS.READ", "series", "$", "BLOCK", "0", "1").isBlocking());
        Assert.assertTrue(command("TS.READ", "series", "$", "MAX_COUNT", "10", "block", "100", "2").isBlocking());
        Assert.assertFalse(command("TS.ADD", "series", "*", "1.2", "ON_DUPLICATE", "BLOCK").isBlocking());
    }

    @Test
    public void updatingReadArgumentsClearsBlockingCache() {
        Command command = command("TS.READ", "series", "$", "BLOCK", "0", "1");
        Assert.assertTrue(command.isBlocking());
        command.updateObjects(command("TS.READ", "series", "0").getObjects());
        Assert.assertFalse(command.isBlocking());
        command.updateObjects(command("TS.READ", "series", "$", "BLOCK", "100", "1").getObjects());
        Assert.assertTrue(command.isBlocking());
    }

    @Test
    public void multiKeyCommandsExcludeTimestampsValuesAndOptions() {
        assertKeys(command("TS.MADD", "a", "*", "1.1", "b", "123", "2.2"), "a", "b");
        assertKeys(command("TS.CREATERULE", "a", "b", "AGGREGATION", "AVG", "60000"), "a", "b");
        assertKeys(command("TS.DELETERULE", "a", "b"), "a", "b");
        for (String name : new String[]{"TS.NRANGE", "TS.NREVRANGE"}) {
            assertKeys(command(name, "2", "a", "b", "-", "+", "COUNT", "10"), "a", "b");
        }
    }

    @Test
    public void malformedMultiKeyArgumentsDoNotOverflowOrIndexOutsideArrays() {
        assertKeys(command("TS.MADD", "a", "0"));
        assertKeys(command("TS.CREATERULE", "a"));
        for (String count : new String[]{"0", "-1", "x", "2147483648", "9223372036854775808"}) {
            assertKeys(command("TS.NRANGE", count, "a", "-", "+"));
            assertKeys(command("TS.NREVRANGE", count, "a", "-", "+"));
        }
    }

    @Test
    public void labelQueriesDoNotTreatFiltersOrLabelsAsKeys() {
        for (Command command : new Command[]{
                command("TS.MGET", "WITHLABELS", "FILTER", "region=cn"),
                command("TS.MRANGE", "-", "+", "FILTER", "region=cn"),
                command("TS.MREVRANGE", "-", "+", "FILTER", "region=cn"),
                command("TS.QUERYINDEX", "region=cn"),
                command("TS.QUERYLABELS", "VALUES", "region", "FILTER", "type=cpu")}) {
            assertKeys(command);
            Assert.assertEquals(RedisCommand.CommandSupportType.PARTIALLY_SUPPORT_2, command.getRedisCommand().getSupportType());
        }
    }

    private static void assertKeys(Command command, String... expected) {
        Assert.assertNotNull(command.getRedisCommand());
        Assert.assertEquals(Arrays.asList(expected), command.getKeys().stream().map(Utils::bytesToString).collect(Collectors.toList()));
    }

    private static Command command(String... args) {
        return new Command(Arrays.stream(args).map(Utils::stringToBytes).toArray(byte[][]::new));
    }
}
