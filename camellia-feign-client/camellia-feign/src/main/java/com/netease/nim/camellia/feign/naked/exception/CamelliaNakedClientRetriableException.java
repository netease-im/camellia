package com.netease.nim.camellia.feign.naked.exception;


public class CamelliaNakedClientRetriableException extends CamelliaNakedClientException {

    public CamelliaNakedClientRetriableException() {
    }

    public CamelliaNakedClientRetriableException(String message) {
        super(message);
    }

    public CamelliaNakedClientRetriableException(String message, Throwable cause) {
        super(message, cause);
    }

    public CamelliaNakedClientRetriableException(Throwable cause) {
        super(cause);
    }

    public CamelliaNakedClientRetriableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
