package com.netease.nim.camellia.feign.naked.exception;

public class CamelliaNakedClientNoRetriableException extends CamelliaNakedClientException {

    public CamelliaNakedClientNoRetriableException() {
    }

    public CamelliaNakedClientNoRetriableException(String message) {
        super(message);
    }

    public CamelliaNakedClientNoRetriableException(String message, Throwable cause) {
        super(message, cause);
    }

    public CamelliaNakedClientNoRetriableException(Throwable cause) {
        super(cause);
    }

    public CamelliaNakedClientNoRetriableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
