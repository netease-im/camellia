package com.netease.nim.camellia.redis.toolkit.mergetask;

import java.nio.charset.StandardCharsets;

/**
 * Created by caojiajun on 2022/11/7
 */
public class CamelliaMergeTaskResultIntSerializer implements CamelliaMergeTaskResultSerializer<Integer> {

    public static final CamelliaMergeTaskResultIntSerializer INSTANCE = new CamelliaMergeTaskResultIntSerializer();

    @Override
    public byte[] serialize(Integer result) {
        return String.valueOf(result).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Integer deserialize(byte[] data) {
        return Integer.parseInt(new String(data, StandardCharsets.UTF_8));
    }
}
