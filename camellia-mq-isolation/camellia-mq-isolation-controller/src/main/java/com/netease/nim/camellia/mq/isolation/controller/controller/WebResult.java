package com.netease.nim.camellia.mq.isolation.controller.controller;

/**
 *
 * Created by caojiajun on 2019/5/28.
 */
public class WebResult {
    private int code;
    private String msg;
    private Object data;

    public WebResult() {
    }

    public WebResult(int code) {
        this.code = code;
    }

    public WebResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public WebResult(int code, String msg, Object data) {
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

    public static WebResult success() {
        return new WebResult(200, "success", null);
    }

    public static WebResult success(Object data) {
        return new WebResult(200, "success", data);
    }
}
