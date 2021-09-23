package com.netease.nim.camellia.redis.proxy.reply;

import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.buffer.ByteBuf;


public class BulkReply implements Reply {
    public static final BulkReply NIL_REPLY = new BulkReply();

    private static final char MARKER = Marker.BulkReply.getMarker();
    private int capacity;
    private byte[] raw;

    private BulkReply() {
        this.capacity = -1;
    }

    public BulkReply(byte[] bytes) {
        this.raw = bytes;
        if (bytes == null) {
            this.capacity = -1;
        } else {
            this.capacity = bytes.length;
        }
    }

    public byte[] getRaw() {
        return raw;
    }

    public void updateRaw(byte[] bytes) {
        this.raw = bytes;
        if (bytes == null) {
            this.capacity = -1;
        } else {
            this.capacity = bytes.length;
        }
    }

    @Override
    public void write(ByteBuf byteBuf) {
        byteBuf.writeByte(MARKER);
        byteBuf.writeBytes(Utils.numToBytes(capacity, true));
        if (capacity >= 0) {
            byteBuf.writeBytes(raw);
            byteBuf.writeBytes(CRLF);
        }
    }

    public String toString() {
        if (raw == null) return null;
        return new String(raw, Utils.utf8Charset);
    }
}
