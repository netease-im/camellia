package com.netease.nim.camellia.redis.proxy.reply;

import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.buffer.ByteBuf;


public class BulkReply implements Reply {
    public static final BulkReply NIL_REPLY = new BulkReply();

    private static final char MARKER = Marker.BulkReply.getMarker();
    private int capacity;
    private byte[] raw;
    private ByteBuf buf;

    private BulkReply() {
        this.capacity = -1;
    }

    public BulkReply(ByteBuf buf) {
        this.buf = buf;
        this.capacity = buf.readableBytes();
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
        if (raw != null) {
            return raw;
        }
        if (buf == null) {
            return null;
        }
        byte[] raw = new byte[capacity];
        buf.readBytes(raw);
        buf.release();
        buf = null;
        this.raw = raw;
        return raw;
    }

    public void updateRaw(byte[] bytes) {
        this.raw = bytes;
        if (buf != null) {
            buf.release();
            buf = null;
        }
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
            if (this.buf != null) {
                byteBuf.writeBytes(this.buf);
                this.buf.release();
            } else {
                byteBuf.writeBytes(raw);
            }
            byteBuf.writeBytes(CRLF);
        }
    }

    public String toString() {
        if (getRaw() == null) return null;
        return new String(getRaw(), Utils.utf8Charset);
    }
}
