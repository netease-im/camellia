package com.netease.nim.camellia.redis.proxy.reply;

import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;


public class BulkReply implements Reply<ByteBuf> {
    public static final BulkReply NIL_REPLY = new BulkReply();

    private static final char MARKER = Marker.BulkReply.getMarker();
    private final int capacity;
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

    @Override
    public void write(ByteBuf byteBuf) throws IOException {
        byteBuf.writeByte(MARKER);
        byteBuf.writeBytes(Utils.numToBytes(capacity, true));
        if (capacity >= 0) {
            ByteBuf buf = Unpooled.wrappedBuffer(raw);
            byteBuf.writeBytes(buf);
            byteBuf.writeBytes(CRLF);
        }
    }

    public String toString() {
        return new String(raw, Utils.utf8Charset);
    }
}
