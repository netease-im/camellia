package com.netease.nim.camellia.redis.toolkit.mergetask;

import java.nio.charset.StandardCharsets;

/**
 * Created by caojiajun on 2022/11/7
 */
public class CamelliaMergeTaskResultLongSerializer implements CamelliaMergeTaskResultSerializer<Long> {

    public static final CamelliaMergeTaskResultLongSerializer INSTANCE = new CamelliaMergeTaskResultLongSerializer();

    @Override
    public byte[] serialize(Long result) {
        return String.valueOf(result).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Long deserialize(byte[] data) {
        return Long.parseLong(new String(data, StandardCharsets.UTF_8));
    }
}
