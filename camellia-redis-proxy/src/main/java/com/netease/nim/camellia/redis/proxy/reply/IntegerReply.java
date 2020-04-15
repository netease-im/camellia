package com.netease.nim.camellia.redis.proxy.reply;

import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.buffer.ByteBuf;

import java.io.IOException;


public class IntegerReply implements Reply<Long> {

    public static final IntegerReply REPLY_1 = new IntegerReply(1L);
    public static final IntegerReply REPLY_0 = new IntegerReply(0L);

    private static final char MARKER = Marker.IntegerReply.getMarker();
    private final Long integer;

    public IntegerReply(Long integer) {
        this.integer = integer;
    }

    public Long getInteger() {
        return integer;
    }

    @Override
    public void write(ByteBuf byteBuf) throws IOException {
        if (integer == null) {
            BulkReply.NIL_REPLY.write(byteBuf);
        } else {
            byteBuf.writeByte(MARKER);
            byteBuf.writeBytes(Utils.numToBytes(integer, true));
        }
    }

    public String toString() {
        return String.valueOf(integer);
    }
}
