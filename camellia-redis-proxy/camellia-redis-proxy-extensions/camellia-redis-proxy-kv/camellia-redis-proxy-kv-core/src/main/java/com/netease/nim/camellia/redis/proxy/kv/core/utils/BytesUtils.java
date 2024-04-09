package com.netease.nim.camellia.redis.proxy.kv.core.utils;

/**
 * Created by caojiajun on 2024/4/7
 */
public class BytesUtils {

    public static final int SIZEOF_LONG = Long.SIZE / Byte.SIZE;
    public static final int SIZEOF_INT = Integer.SIZE / Byte.SIZE;

    public static byte[] toBytes(int val) {
        byte [] b = new byte[4];
        for(int i = 3; i > 0; i--) {
            b[i] = (byte) val;
            val >>>= 8;
        }
        b[0] = (byte) val;
        return b;
    }

    public static int toInt(byte[] bytes) {
        return toInt(bytes, 0, SIZEOF_INT);
    }

    public static int toInt(byte[] bytes, int offset) {
        return toInt(bytes, offset, SIZEOF_INT);
    }

    public static int toInt(byte[] bytes, int offset, final int length) {
        int n = 0;
        for(int i = offset; i < (offset + length); i++) {
            n <<= 8;
            n ^= bytes[i] & 0xFF;
        }
        return n;
    }

    public static byte[] toBytes(long val) {
        byte [] b = new byte[8];
        for (int i = 7; i > 0; i--) {
            b[i] = (byte) val;
            val >>>= 8;
        }
        b[0] = (byte) val;
        return b;
    }

    public static long toLong(byte[] bytes) {
        return toLong(bytes, 0, SIZEOF_LONG);
    }

    public static long toLong(byte[] bytes, int offset) {
        return toLong(bytes, offset, SIZEOF_LONG);
    }

    public static long toLong(byte[] bytes, int offset, final int length) {
        long l = 0;
        for(int i = offset; i < offset + length; i++) {
            l <<= 8;
            l ^= bytes[i] & 0xFF;
        }
        return l;
    }

    public static byte [] merge(final byte a, final byte [] b) {
        byte [] result = new byte[1 + b.length];
        result[0] = a;
        System.arraycopy(b, 0, result, 1, b.length);
        return result;
    }

    public static byte [] merge(final byte [] a, final byte [] b) {
        byte [] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static byte [] merge(final byte [] a, final byte [] b, final byte [] c) {
        byte [] result = new byte[a.length + b.length + c.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        System.arraycopy(c, 0, result, a.length + b.length, c.length);
        return result;
    }

    public static boolean startWith(byte[] data, byte[] prefix) {
        if (prefix.length > data.length) {
            return false;
        }
        for (int i=0; i<prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
