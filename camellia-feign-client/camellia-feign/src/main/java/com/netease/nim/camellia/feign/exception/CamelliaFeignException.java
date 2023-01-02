package com.netease.nim.camellia.feign.exception;

/**
 * Created by caojiajun on 2022/3/16
 */
public class CamelliaFeignException extends Exception {

    public CamelliaFeignException() {
    }

    public CamelliaFeignException(String message) {
        super(message);
    }

    public CamelliaFeignException(String message, Throwable cause) {
        super(message, cause);
    }

    public CamelliaFeignException(Throwable cause) {
        super(cause);
    }

    public CamelliaFeignException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
