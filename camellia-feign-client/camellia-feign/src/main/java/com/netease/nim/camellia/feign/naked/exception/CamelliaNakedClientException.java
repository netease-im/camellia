package com.netease.nim.camellia.feign.naked.exception;

public class CamelliaNakedClientException extends RuntimeException {
    public CamelliaNakedClientException() {
    }

    public CamelliaNakedClientException(String message) {
        super(message);
    }

    public CamelliaNakedClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public CamelliaNakedClientException(Throwable cause) {
        super(cause);
    }

    public CamelliaNakedClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
