package com.netease.nim.camellia.redis.proxy.plugin.converter;

import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.util.Utils;


/**
 * Created by caojiajun on 2022/12/7
 */
public class DefaultTenancyNamespaceKeyConverter implements KeyConverter {

    @Override
    public byte[] convert(CommandContext commandContext, RedisCommand redisCommand, byte[] originalKey) {
        if (commandContext.getBid() == null || commandContext.getBgroup() == null || commandContext.getBid() <= 0) {
            return originalKey;
        }
        byte[] prefix = Utils.stringToBytes(commandContext.getBid() + "|" + commandContext.getBgroup() + "|");
        byte[] convertedKey = new byte[originalKey.length + prefix.length];
        System.arraycopy(prefix, 0, convertedKey, 0, prefix.length);
        System.arraycopy(originalKey, 0, convertedKey, prefix.length, originalKey.length);
        return convertedKey;
    }

    @Override
    public byte[] reverseConvert(CommandContext commandContext, RedisCommand redisCommand, byte[] convertedKey) {
        if (commandContext.getBid() == null || commandContext.getBgroup() == null || commandContext.getBid() <= 0) {
            return convertedKey;
        }
        byte[] prefix = Utils.stringToBytes(commandContext.getBid() + "|" + commandContext.getBgroup() + "|");
        if (convertedKey.length < prefix.length) {
            return convertedKey;
        }
        boolean prefixMatch = true;
        for (int i=0; i<prefix.length; i++) {
            if (prefix[i] != convertedKey[i]) {
                prefixMatch = false;
                break;
            }
        }
        if (!prefixMatch) {
            return convertedKey;
        }
        byte[] originalKey = new byte[convertedKey.length - prefix.length];
        System.arraycopy(convertedKey, prefix.length, originalKey, 0, originalKey.length);
        return originalKey;
    }
}
