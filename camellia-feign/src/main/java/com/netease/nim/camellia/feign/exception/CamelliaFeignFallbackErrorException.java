package com.netease.nim.camellia.feign.exception;

/**
 * Created by caojiajun on 2022/3/25
 */
public class CamelliaFeignFallbackErrorException extends Exception {

    public CamelliaFeignFallbackErrorException() {
    }

    public CamelliaFeignFallbackErrorException(String message) {
        super(message);
    }

    public CamelliaFeignFallbackErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public CamelliaFeignFallbackErrorException(Throwable cause) {
        super(cause);
    }

    public CamelliaFeignFallbackErrorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
