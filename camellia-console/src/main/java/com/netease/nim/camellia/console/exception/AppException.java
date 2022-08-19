package com.netease.nim.camellia.console.exception;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class AppException extends RuntimeException{
    private int code;
    private String msg;

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

    public AppException(int code, String msg,Throwable cause) {
        super(cause);
        this.code = code;
        this.msg = msg;
    }


    public AppException(Throwable cause) {
        super(cause);
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
}
