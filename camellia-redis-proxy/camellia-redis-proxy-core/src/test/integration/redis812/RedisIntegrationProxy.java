import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.plugin.converter.*;
import com.netease.nim.camellia.redis.proxy.util.CamelliaRedisProxyStarter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

/** Standalone test process, configured only with the runner's temporary endpoints. */
public class RedisIntegrationProxy {
    public static void main(String[] args) {
        CamelliaRedisProxyStarter.updatePort(Integer.parseInt(args[0]));
        CamelliaRedisProxyStarter.updateRouteConf(args[1]);
        Map<String, String> config = CamelliaRedisProxyStarter.getProperties().getConfig();
        config.put("tcp_server.work.thread", "2");
        config.put("tcp_upstream.upstream.work.thread", "2");
        if (!args[2].equals("off")) {
            config.put("proxy.plugin.list", "converterPlugin");
            config.put("converter.key.className", PrefixKey.class.getName());
            if (args[2].equals("values")) {
                for (String type : new String[]{"string", "hash", "set", "list", "zset"}) {
                    config.put("converter." + type + ".className", PrefixValue.class.getName());
                }
            }
        }
        CamelliaRedisProxyStarter.start();
        if (!CamelliaRedisProxyStarter.isStart()) {
            throw new IllegalStateException("Integration proxy failed to start");
        }
    }

    private static byte[] encode(String prefix, byte[] value) {
        byte[] head = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] result = Arrays.copyOf(head, head.length + value.length);
        System.arraycopy(value, 0, result, head.length, value.length);
        return result;
    }

    private static byte[] decode(String prefix, byte[] value) {
        byte[] head = prefix.getBytes(StandardCharsets.UTF_8);
        if (value.length < head.length || !Arrays.equals(head, Arrays.copyOf(value, head.length))) {
            throw new IllegalArgumentException("Unexpected integration converter prefix");
        }
        return Arrays.copyOfRange(value, head.length, value.length);
    }

    public static class PrefixKey implements KeyConverter {
        public byte[] convert(CommandContext context, RedisCommand command, byte[] key) {
            return encode("e2e:", key);
        }

        public byte[] reverseConvert(CommandContext context, RedisCommand command, byte[] key) {
            return decode("e2e:", key);
        }
    }

    public static class PrefixValue implements StringConverter, HashConverter, SetConverter, ListConverter, ZSetConverter {
        public byte[] valueConvert(CommandContext context, byte[] key, byte[] value) {
            return encode("v:", value);
        }

        public byte[] valueReverseConvert(CommandContext context, byte[] key, byte[] value) {
            return decode("v:", value);
        }

        public byte[] fieldConvert(CommandContext context, byte[] key, byte[] value) {
            return encode("f:", value);
        }

        public byte[] fieldReverseConvert(CommandContext context, byte[] key, byte[] value) {
            return decode("f:", value);
        }
    }
}
