package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.file;

import java.nio.ByteBuffer;

/**
 * Created by caojiajun on 2025/1/6
 */
public class ByteBufferUtil {

    public static void writeInt(int value, ByteBuffer buffer) {
        while ((value & 0xFFFFFF80) != 0L) {
            buffer.put((byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        buffer.put((byte) (value & 0x7F));
    }

    public static int readInt(ByteBuffer buffer, int idx) {
        int value = 0;
        int i = 0;
        while ((buffer.get(idx) & 0x80) != 0) {
            value |= (buffer.get(idx) & 0x7F) << i;
            i += 7;
            idx++;
            if (i > 21) {
                throw new IllegalArgumentException("Variable length quantity is too long");
            }
        }
        return value | (buffer.get(idx) << i);
    }
}
