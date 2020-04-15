package com.netease.nim.camellia.redis.proxy.console;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class ConsoleResult {

    private HttpResponseStatus code = HttpResponseStatus.OK;
    private String data = "success";

    public ConsoleResult(HttpResponseStatus code, String data) {
        this.code = code;
        this.data = data;
    }

    public HttpResponseStatus getCode() {
        return code;
    }

    public void setCode(HttpResponseStatus code) {
        this.code = code;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public static ConsoleResult success() {
        return new ConsoleResult(HttpResponseStatus.OK, "success");
    }

    public static ConsoleResult success(String data) {
        return new ConsoleResult(HttpResponseStatus.OK, data);
    }

    public static ConsoleResult error() {
        return new ConsoleResult(HttpResponseStatus.INTERNAL_SERVER_ERROR, "error");
    }

    public static ConsoleResult error(String data) {
        return new ConsoleResult(HttpResponseStatus.INTERNAL_SERVER_ERROR, data);
    }
}
