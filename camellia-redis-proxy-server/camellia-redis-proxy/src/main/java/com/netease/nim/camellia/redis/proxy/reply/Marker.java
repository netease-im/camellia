package com.netease.nim.camellia.redis.proxy.reply;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
public enum Marker {

    BulkReply('$'),
    ErrorReply('-'),
    StatusReply('+'),
    MultiBulkReply('*'),
    IntegerReply(':'),
    ;

    private final char ch;

    Marker(char ch) {
        this.ch = ch;
    }

    public char getMarker() {
        return ch;
    }

    private static final Marker[] index = new Marker[128];
    static {
        for (Marker marker : Marker.values()) {
            index[marker.ch] = marker;
        }
    }

    public static Marker byValue(byte c) {
        if (c < 0) return null;
        return index[c];
    }
}
