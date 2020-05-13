package com.netease.nim.camellia.redis.proxy.util;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

public class ErrorHandlerUtil {

    public static Throwable handler(Throwable e) {
        while (true) {
            if (e instanceof ExecutionException) {
                if (e.getCause() == null) {
                    break;
                } else {
                    e = e.getCause();
                }
            } else if (e instanceof InvocationTargetException) {
                Throwable targetException = ((InvocationTargetException) e).getTargetException();
                if (targetException == null) {
                    break;
                } else {
                    e = targetException;
                }
            } else {
                break;
            }
        }
        return e;
    }

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
