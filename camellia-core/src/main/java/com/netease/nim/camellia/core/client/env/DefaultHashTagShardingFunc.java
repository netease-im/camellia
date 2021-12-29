package com.netease.nim.camellia.core.client.env;

/**
 * Created by caojiajun on 2021/12/29
 */
public class DefaultHashTagShardingFunc extends AbstractSimpleShardingFunc {

    @Override
    public int shardingCode(byte[] key) {
        int s = -1;
        int e = -1;
        boolean sFound = false;
        for (int i = 0; i < key.length; i++) {
            if (key[i] == '{' && !sFound) {
                s = i;
                sFound = true;
            }
            if (key[i] == '}' && sFound) {
                e = i;
                break;
            }
        }
        int code;
        if (s > -1 && e > -1 && e != s + 1) {
            code = getCode(key, s + 1, e);
        } else {
            code = getCode(key, 0, key.length);
        }
        if (code < 0) {
            code = -code;
        }
        return code;
    }

    private int getCode(byte[] bytes, int s, int e) {
        int h = 0;
        for (int i = s; i < e; i++) {
            byte b = bytes[i];
            h = 31 * h + b;
        }
        return h;
    }
}
