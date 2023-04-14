package com.netease.nim.camellia.redis.proxy.reply;

import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

public class ErrorReply implements Reply {
    public static final ErrorReply NOT_SUPPORT = new ErrorReply("ERR proxy command not support");

    public static final ErrorReply NOT_AVAILABLE = new ErrorReply("ERR proxy not available");

    public static final ErrorReply UPSTREAM_NOT_AVAILABLE = new ErrorReply("ERR proxy upstream not available");

    public static final ErrorReply UPSTREAM_RESOURCE_NOT_AVAILABLE = new ErrorReply("ERR proxy upstream resource not available");
    public static final ErrorReply UPSTREAM_RESOURCE_NULL = new ErrorReply("ERR proxy upstream resource null");

    public static final ErrorReply UPSTREAM_CONNECTION_NOT_AVAILABLE = new ErrorReply("ERR proxy upstream connection not available");
    public static final ErrorReply UPSTREAM_CONNECTION_QUEUE_FULL = new ErrorReply("ERR proxy upstream connection queue full");
    public static final ErrorReply UPSTREAM_CONNECTION_CACHED_QUEUE_FULL = new ErrorReply("ERR proxy upstream connection cached queue full");
    public static final ErrorReply UPSTREAM_CONNECTION_STATUS_INVALID = new ErrorReply("ERR proxy upstream connection status invalid");
    public static final ErrorReply UPSTREAM_CONNECTION_NULL = new ErrorReply("ERR proxy upstream connection null");
    public static final ErrorReply UPSTREAM_BIND_CONNECTION_NULL = new ErrorReply("ERR proxy upstream bind connection null");
    public static final ErrorReply UPSTREAM_CONNECTION_REDIS_CLUSTER_NODE_NULL = new ErrorReply("ERR proxy upstream redis cluster node null");
    public static final ErrorReply UPSTREAM_CONNECTION_REDIS_NODE_NULL = new ErrorReply("ERR proxy upstream redis node null");
    public static final ErrorReply UPSTREAM_BIND_CONNECTION_CROSSSLOT = new ErrorReply("CROSSSLOT Keys in request don't hash to the same slot in bind connection");

    public static final ErrorReply TOO_BUSY = new ErrorReply("ERR proxy too busy");
    public static final ErrorReply REPEAT_OPERATION = new ErrorReply("ERR repeat operation");
    public static final ErrorReply NO_AUTH = new ErrorReply("NOAUTH Authentication required");
    public static final ErrorReply INVALID_PASSWORD = new ErrorReply("ERR invalid password");
    public static final ErrorReply WRONG_PASS = new ErrorReply("WRONGPASS invalid username-password pair or user is disabled.");
    public static final ErrorReply TOO_MANY_CONNECTS = new ErrorReply("ERR too many connects");
    public static final ErrorReply DB_INDEX_OUT_OF_RANGE = new ErrorReply("ERR DB index is out of range");

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
        return new ErrorReply("ERR wrong number of arguments for '" + command.strRaw() + "' command");
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
