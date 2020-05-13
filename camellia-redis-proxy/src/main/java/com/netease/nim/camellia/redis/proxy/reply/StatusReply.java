package com.netease.nim.camellia.redis.proxy.reply;

import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

public class StatusReply implements Reply {

    public static final StatusReply OK = new StatusReply(RedisKeyword.OK.name());
    public static final StatusReply PONG = new StatusReply(RedisKeyword.PONG.name());

    private static final char MARKER = Marker.StatusReply.getMarker();

    private String status;
    private byte[] statusBytes;

    public StatusReply(String status) {
        if (status == null) {
            return;
        }
        this.status = status;
        this.statusBytes = status.getBytes(Utils.utf8Charset);
    }

    public String getStatus() {
        return status;
    }

    @Override
    public void write(ByteBuf byteBuf) throws IOException {
        if (status == null) {
            BulkReply.NIL_REPLY.write(byteBuf);
        } else {
            byteBuf.writeByte(MARKER);
            byteBuf.writeBytes(statusBytes);
            byteBuf.writeBytes(CRLF);
        }
    }

    public String toString() {
        return status;
    }
}
