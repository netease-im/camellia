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

    public static Marker byValue(byte c) {
        for (Marker marker : Marker.values()) {
            if (marker.ch == c) {
                return marker;
            }
        }
        return null;
    }
}
