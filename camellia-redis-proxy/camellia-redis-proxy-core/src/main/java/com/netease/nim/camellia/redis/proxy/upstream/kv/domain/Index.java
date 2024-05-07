package com.netease.nim.camellia.redis.proxy.upstream.kv.domain;

import com.netease.nim.camellia.tools.utils.MD5Util;


/**
 * Created by caojiajun on 2024/4/30
 */
public class Index {

    private static final byte prefix1 = 0;
    private static final byte prefix2 = 1;

    private final boolean index;
    private final byte[] ref;
    private final byte[] raw;

    private Index(boolean index, byte[] ref, byte[] raw) {
        this.index = index;
        this.ref = ref;
        this.raw = raw;
    }

    public static Index fromRaw(byte[] raw) {
        if (raw.length < 15) {
            byte[] data = new byte[raw.length + 1];
            data[0] = prefix1;
            System.arraycopy(raw, 0, data, 1, raw.length);
            return new Index(false, data, raw);
        } else {
            byte[] md5 = MD5Util.md5(raw);
            byte[] data = new byte[17];
            data[0] = prefix2;
            System.arraycopy(md5, 0, data, 1, md5.length);
            return new Index(true, data, null);
        }
    }

    public static Index fromRef(byte[] ref) {
        byte prefix = ref[0];
        if (prefix == prefix1) {
            byte[] raw = new byte[ref.length - 1];
            System.arraycopy(ref, 1, raw, 0, raw.length);
            return new Index(false, ref, raw);
        }
        if (prefix == prefix2) {
            return new Index(true, ref, null);
        }
        throw new IllegalArgumentException("to index error");
    }

    public boolean isIndex() {
        return index;
    }

    public byte[] getRef() {
        return ref;
    }

    public byte[] getRaw() {
        return raw;
    }
}
