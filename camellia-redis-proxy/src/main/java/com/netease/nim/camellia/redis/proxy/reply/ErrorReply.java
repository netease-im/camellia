package com.netease.nim.camellia.redis.proxy.reply;

import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

public class ErrorReply implements Reply {
    public static final ErrorReply NOT_SUPPORT = new ErrorReply("Not Support");
    public static final ErrorReply NOT_AVAILABLE = new ErrorReply("Not Available");
    public static final ErrorReply TOO_BUSY = new ErrorReply("Too Busy");
    public static final ErrorReply REPEAT_OPERATION = new ErrorReply("Repeat Operation");
    public static final ErrorReply NO_AUTH = new ErrorReply("NOAUTH Authentication required");
    public static final ErrorReply INVALID_PASSWORD = new ErrorReply("ERR invalid password");
    public static final ErrorReply WRONG_PASS = new ErrorReply("WRONGPASS invalid username-password pair or user is disabled.");
    public static final ErrorReply TOO_MANY_CONNECTS = new ErrorReply("ERR too many connects");

    public static final ErrorReply SYNTAX_ERROR = new ErrorReply(Utils.syntaxError);

    private static final char MARKER = Marker.ErrorReply.getMarker();
    private final String error;
    private final byte[] raw;

    public ErrorReply(String error) {
        this.error = error;
        this.raw = error.getBytes(Utils.utf8Charset);
    }

    public String getError() {
        return error;
    }

    public static ErrorReply argNumWrong(RedisCommand command) {
        return new ErrorReply("ERR wrong number of arguments for '" + command.name().toLowerCase() + "' command");
    }

    @Override
    public void write(ByteBuf byteBuf) throws IOException {
        byteBuf.writeByte(MARKER);
        byteBuf.writeBytes(raw);
        byteBuf.writeBytes(CRLF);
    }

    @Override
    public String toString() {
        return error;
    }
}
