package com.netease.nim.camellia.core.api;

/**
 *
 * Created by caojiajun on 2019/11/7.
 */
public enum CamelliaApiCode {
    SUCCESS(200),
    NOT_MODIFY(304),
    NOT_EXISTS(404),
    PARAM_ERROR(414),
    FORBIDDEN(403),
    ;

    private final int code;

    CamelliaApiCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

