package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils;


/**
 * Created by caojiajun on 2024/5/15
 */
public class ZSetLex {
    private final byte[] lex;
    private final boolean min;
    private final boolean max;
    private final boolean excludeLex;

    public ZSetLex(byte[] lex, boolean min, boolean max, boolean excludeLex) {
        this.lex = lex;
        this.min = min;
        this.max = max;
        this.excludeLex = excludeLex;
    }

    public static ZSetLex fromLex(byte[] arg) {
        byte[] lex = null;
        boolean excludeLex = false;
        boolean min = false;
        boolean max = false;
        if (arg.length == 1 && arg[0] == '-') {
            min = true;
        } else if (arg.length == 1 && arg[0] == '+') {
            max = true;
        } else if (arg[0] == '[') {
            lex = new byte[arg.length - 1];
            System.arraycopy(arg, 1, lex, 0, lex.length);
        } else if (arg[0] == '(') {
            excludeLex = true;
            lex = new byte[arg.length - 1];
            System.arraycopy(arg, 1, lex, 0, lex.length);
        } else {
            return null;
        }
        return new ZSetLex(lex, min, max, excludeLex);
    }

    public byte[] getLex() {
        return lex;
    }

    public boolean isExcludeLex() {
        return excludeLex;
    }

    public boolean isMin() {
        return min;
    }

    public boolean isMax() {
        return max;
    }
}
