package com.netease.nim.camellia.codec;

import io.netty.buffer.ByteBuf;

public class Varint {

    public static void writeInt(int value, Pack pk) {
        while ((value & 0xFFFFFF80) != 0L) {
            pk.putByte((byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        pk.putByte((byte) (value & 0x7F));
    }

    public static void writeInt(int value, ByteBuf cb) {
        while ((value & 0xFFFFFF80) != 0L) {
            cb.writeByte((byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        cb.writeByte((byte) (value & 0x7F));
    }

    public static int readInt(Unpack up) {
        int value = 0;
        int i = 0;
        int b;
        while (((b = up.popByte()) & 0x80) != 0) {
            value |= (b & 0x7F) << i;
            i += 7;
            if (i > 21) {
                throw new IllegalArgumentException("Variable length quantity is too long");
            }
        }
        return value | (b << i);
    }

    public static int readInt(byte[] bytes) {
        int value = 0;
        int i = 0;
        int idx = 0;
        while ((bytes[idx] & 0x80) != 0) {
            value |= (bytes[idx] & 0x7F) << i;
            i += 7;
            idx++;
            if (i > 21) {
                throw new IllegalArgumentException("Variable length quantity is too long");
            }
        }
        return value | (bytes[idx] << i);
    }

    public static int[] getIntAndLen(ByteBuf cb) {
        int[] value = new int[2];
        int i = 0;
        int b;
        cb.markReaderIndex();
        int idx = cb.readerIndex();
        int moveidx = idx;
        while (((b = cb.getByte(moveidx)) & 0x80) != 0) {
            value[1] |= (b & 0x7F) << i;
            i += 7;
            moveidx += 1;
            if (i > 21) {
                throw new IllegalArgumentException("Variable length quantity is too long");
            }
        }
        value[1] = value[1] | (b << i);
        value[0] = moveidx - idx + 1;
        cb.resetReaderIndex();
        return value;
    }

    public static int getInt(ByteBuf cb) {
        int value = 0;
        int i = 0;
        int b;
        int idx = cb.readerIndex();
        while (((b = cb.getByte(idx)) & 0x80) != 0) {
            value |= (b & 0x7F) << i;
            i += 7;
            idx += 1;
            if (i > 21) {
                throw new IllegalArgumentException("Variable length quantity is too long");
            }
        }
        return value | (b << i);

    }

    public static int getVarLen(int value) {
        int i = 1;
        while ((value & 0xFFFFFF80) != 0L) {
            i++;
            value >>>= 7;
        }
        return i;
    }

}
