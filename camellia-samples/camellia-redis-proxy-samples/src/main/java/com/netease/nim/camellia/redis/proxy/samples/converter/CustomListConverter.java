package com.netease.nim.camellia.redis.proxy.samples.converter;

import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.plugin.converter.ListConverter;
import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 * Created by caojiajun on 2021/8/11
 */
public class CustomListConverter implements ListConverter {
    @Override
    public byte[] valueConvert(CommandContext commandContext, byte[] key, byte[] originalValue) {
        String keyStr = Utils.bytesToString(key);
        if (keyStr.equals("k1")) {
            if (originalValue == null) return null;
            String str = Utils.bytesToString(originalValue);
            return Utils.stringToBytes(str.replaceAll("abc", "***"));
        }
        return originalValue;
    }

    @Override
    public byte[] valueReverseConvert(CommandContext commandContext, byte[] key, byte[] convertedValue) {
        String keyStr = Utils.bytesToString(key);
        if (keyStr.equals("k1")) {
            if (convertedValue == null) return null;
            String str = Utils.bytesToString(convertedValue);
            return Utils.stringToBytes(str.replaceAll("\\*\\*\\*", "abc"));
        }
        return convertedValue;
    }
}
