package com.netease.nim.camellia.redis.proxy.util;


import com.netease.nim.camellia.redis.proxy.reply.ErrorReplyException;

public class ErrorHandlerUtil {

    public static String redisErrorMessage(Throwable e) {
        if (e instanceof ErrorReplyException) {
            return ((ErrorReplyException) e).getMsg();
        }
        String message = e.getMessage();
        if (message == null) {
            message = e.toString();
        }
        if (!message.startsWith("ERR")) {
            message = "ERR " + message;
        }
        return message;
    }

}
