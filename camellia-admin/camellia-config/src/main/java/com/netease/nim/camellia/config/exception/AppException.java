package com.netease.nim.camellia.config.exception;

/**
 *
 * Created by caojiajun on 2019/11/7.
 */
public class AppException extends RuntimeException {
    private int code;
    private String msg;
    private Object data;

    public AppException() {
    }

    public AppException(int code) {
        this.code = code;
    }

    public AppException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public AppException(int code, String msg, Object data) {
        super(msg);
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
