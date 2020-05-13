package com.netease.nim.camellia.redis.proxy.reply;

import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.buffer.ByteBuf;

import java.io.IOException;


/**
 * Nested replies.
 */
public class MultiBulkReply implements Reply {

    public static final MultiBulkReply EMPTY = new MultiBulkReply(new Reply[0]);

    private static final char MARKER = Marker.MultiBulkReply.getMarker();

    private final Reply[] replies;

    public MultiBulkReply(Reply[] replies) {
        this.replies = replies;
    }

    public Reply[] getReplies() {
        return replies;
    }

    @Override
    public void write(ByteBuf byteBuf) throws IOException {
        byteBuf.writeByte(MARKER);
        if (replies == null) {
            byteBuf.writeBytes(Utils.NEG_ONE_WITH_CRLF);
        } else {
            byteBuf.writeBytes(Utils.numToBytes(replies.length, true));
            for (Reply reply : replies) {
                reply.write(byteBuf);
            }
        }
    }
}
