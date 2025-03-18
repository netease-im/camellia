package com.netease.nim.camellia.redis.proxy.reply;

/**
 * Created by caojiajun on 2025/3/18
 */
public class ErrorReplyException extends RuntimeException {

    private final String msg;

    public ErrorReplyException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}
