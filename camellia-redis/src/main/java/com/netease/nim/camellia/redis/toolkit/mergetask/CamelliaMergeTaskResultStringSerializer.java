package com.netease.nim.camellia.redis.toolkit.mergetask;

import java.nio.charset.StandardCharsets;

/**
 * Created by caojiajun on 2022/11/7
 */
public class CamelliaMergeTaskResultStringSerializer implements CamelliaMergeTaskResultSerializer<String> {

    public static final CamelliaMergeTaskResultStringSerializer INSTANCE = new CamelliaMergeTaskResultStringSerializer();

    @Override
    public byte[] serialize(String result) {
        return result.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String deserialize(byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }
}
