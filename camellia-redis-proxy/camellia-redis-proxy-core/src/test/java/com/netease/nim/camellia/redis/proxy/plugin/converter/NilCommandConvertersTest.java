package com.netease.nim.camellia.redis.proxy.plugin.converter;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class NilCommandConvertersTest {
    @Test
    public void missingValuesNeverReachConvertersOrMutateSharedNil() {
        Values values = new Values();
        ConverterConfig config = new ConverterConfig();
        config.setListConverter(values);
        config.setSetConverter(values);
        config.setzSetConverter(values);
        Converters converters = new Converters(config);
        try {
            for (String[] args : new String[][]{{"LPOP", "missing"}, {"RPOP", "missing"},
                    {"LINDEX", "missing", "0"}, {"SPOP", "missing"}, {"SRANDMEMBER", "missing"},
                    {"ZRANDMEMBER", "missing"}}) {
                converters.convertReply(command(args), BulkReply.NIL_REPLY);
                Assert.assertNull(args[0], BulkReply.NIL_REPLY.getRaw());
                Assert.assertEquals(-1, BulkReply.NIL_REPLY.getSize());
            }
            Assert.assertEquals(0, values.reverseCalls);
        } finally {
            // Prevent a regression in this test from affecting other tests in the JVM.
            BulkReply.NIL_REPLY.updateRaw(null);
        }
    }

    @Test
    public void blockingZsetTimeoutDoesNotDereferenceNilArray() {
        ConverterConfig config = new ConverterConfig();
        Values values = new Values();
        config.setzSetConverter(values);
        Converters converters = new Converters(config);
        for (String name : new String[]{"BZPOPMIN", "BZPOPMAX"}) {
            converters.convertReply(command(name, "missing", "0.01"), MultiBulkReply.NIL_REPLY);
        }
        Assert.assertEquals(0, values.reverseCalls);
    }

    @Test
    public void existingValuesAreStillDecoded() {
        Values values = new Values();
        ConverterConfig config = new ConverterConfig();
        config.setListConverter(values);
        config.setSetConverter(values);
        config.setzSetConverter(values);
        Converters converters = new Converters(config);
        for (String name : new String[]{"LPOP", "RPOP", "SPOP", "SRANDMEMBER", "ZRANDMEMBER"}) {
            BulkReply reply = new BulkReply(Utils.stringToBytes("encoded"));
            converters.convertReply(command(name, "key"), reply);
            Assert.assertEquals("decoded", reply.toString());
        }
        Assert.assertEquals(5, values.reverseCalls);
    }

    private static Command command(String... args) {
        return new Command(Arrays.stream(args).map(Utils::stringToBytes).toArray(byte[][]::new));
    }

    private static class Values implements ListConverter, SetConverter, ZSetConverter {
        private int reverseCalls;

        @Override
        public byte[] valueConvert(CommandContext context, byte[] key, byte[] value) {
            return value;
        }

        @Override
        public byte[] valueReverseConvert(CommandContext context, byte[] key, byte[] value) {
            reverseCalls++;
            // Deliberately return non-null even for null input to detect shared-nil corruption.
            return Utils.stringToBytes("decoded");
        }
    }
}
