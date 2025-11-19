package com.netease.nim.camellia.naming.core;

/**
 * Created by caojiajun on 2025/11/18
 */
public class CamelliaNamingException extends RuntimeException {

    public CamelliaNamingException() {
    }

    public CamelliaNamingException(String message) {
        super(message);
    }

    public CamelliaNamingException(String message, Throwable cause) {
        super(message, cause);
    }

    public CamelliaNamingException(Throwable cause) {
        super(cause);
    }

    public CamelliaNamingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
