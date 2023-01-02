package com.netease.nim.camellia.redis.proxy.util;


public class ErrorHandlerUtil {

    public static String redisErrorMessage(Throwable e) {
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
