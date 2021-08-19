package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.command.async.converter.KeyConverter;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 * Created by caojiajun on 2021/8/19
 */
public class CustomKeyConverter implements KeyConverter {

    private static final byte[] prefix = Utils.stringToBytes("v1");

    @Override
    public byte[] convert(CommandContext commandContext, RedisCommand redisCommand, byte[] originalKey) {
        byte[] result = new byte[prefix.length + originalKey.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(originalKey, 0, result, prefix.length, originalKey.length);
        return result;
    }

    @Override
    public byte[] reverseConvert(CommandContext commandContext, RedisCommand redisCommand, byte[] convertedKey) {
        if (convertedKey.length > prefix.length) {
            for (int i=0; i<prefix.length; i++) {
                if (prefix[i] != convertedKey[i]) {
                    return convertedKey;
                }
            }
            byte[] result = new byte[convertedKey.length - prefix.length];
            System.arraycopy(convertedKey, prefix.length, result, 0, result.length);
            return result;
        }
        return convertedKey;
    }
}
