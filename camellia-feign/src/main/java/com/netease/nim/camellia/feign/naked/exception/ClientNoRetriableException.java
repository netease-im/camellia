package com.netease.nim.camellia.feign.naked.exception;

/**
 *
 * Created by yuanyuanjun on 2019/4/22.
 */
public class ClientNoRetriableException extends ClientException {

    public ClientNoRetriableException() {
    }

    public ClientNoRetriableException(String message) {
        super(message);
    }

    public ClientNoRetriableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientNoRetriableException(Throwable cause) {
        super(cause);
    }

    public ClientNoRetriableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
