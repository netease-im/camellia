package com.netease.nim.camellia.tools.circuitbreaker;

/**
 * Created by caojiajun on 2022/3/25
 */
public class CamelliaCircuitBreakerException extends RuntimeException {

    public CamelliaCircuitBreakerException() {
    }

    public CamelliaCircuitBreakerException(String message) {
        super(message);
    }

    public CamelliaCircuitBreakerException(String message, Throwable cause) {
        super(message, cause);
    }

    public CamelliaCircuitBreakerException(Throwable cause) {
        super(cause);
    }

    public CamelliaCircuitBreakerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
