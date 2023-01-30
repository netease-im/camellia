package com.netease.nim.camellia.redis.base.utils;


import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/1/30
 */
public class SafeEncoder {

    public static byte[][] encodeMany(final String... strs) {
        byte[][] many = new byte[strs.length][];
        for (int i = 0; i < strs.length; i++) {
            many[i] = encode(strs[i]);
        }
        return many;
    }

    public static byte[] encode(final String str) {
        if (str == null) {
            throw new CamelliaRedisException("value sent to redis cannot be null");
        }
        return str.getBytes(StandardCharsets.UTF_8);
    }

    public static String encode(final byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    public static Object encodeObject(Object dataToEncode) {
        if (dataToEncode instanceof byte[]) {
            return SafeEncoder.encode((byte[]) dataToEncode);
        }
        if (dataToEncode instanceof List) {
            List arrayToDecode = (List) dataToEncode;
            List returnValueArray = new ArrayList(arrayToDecode.size());
            for (Object arrayEntry : arrayToDecode) {
                // recursive call and add to list
                returnValueArray.add(encodeObject(arrayEntry));
            }
            return returnValueArray;
        }
        return dataToEncode;
    }
}
