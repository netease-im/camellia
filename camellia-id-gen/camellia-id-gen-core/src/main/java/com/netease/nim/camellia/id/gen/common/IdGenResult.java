package com.netease.nim.camellia.id.gen.common;

/**
 * Created by caojiajun on 2021/9/26
 */
public class IdGenResult {
    private long code;
    private Object data;
    private String msg;

    public IdGenResult(long code, Object data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }

    public long getCode() {
        return code;
    }

    public void setCode(long code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public static IdGenResult success(Object data) {
        return new IdGenResult(200, data, "success");
    }

    public static IdGenResult error(String msg) {
        return new IdGenResult(500, null, msg);
    }
}
