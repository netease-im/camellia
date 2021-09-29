package com.netease.nim.camellia.id.gen.common;

/**
 * Created by caojiajun on 2021/9/18
 */
public class CamelliaIdGenException extends RuntimeException {

    public static final int NETWORK_ERROR = 405;
    public static final int UNKNOWN = 500;

    private int code = UNKNOWN;

    public CamelliaIdGenException(int code, String message) {
        super(message);
        this.code = code;
    }

    public CamelliaIdGenException(String message) {
        super(message);
    }

    public CamelliaIdGenException(int code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public CamelliaIdGenException(Throwable cause) {
        super(cause);
    }
    public CamelliaIdGenException(String message, Throwable cause) {
        super(message, cause);
    }

    public CamelliaIdGenException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
