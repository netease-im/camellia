package com.netease.nim.camellia.feign.naked.exception;

/**
 *
 * Created by yuanyuanjun on 2019/4/22.
 */
public class ClientRetriableException extends ClientException {

    public ClientRetriableException() {
    }

    public ClientRetriableException(String message) {
        super(message);
    }

    public ClientRetriableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientRetriableException(Throwable cause) {
        super(cause);
    }

    public ClientRetriableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
