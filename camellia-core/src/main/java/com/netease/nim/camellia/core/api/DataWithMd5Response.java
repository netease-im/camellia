package com.netease.nim.camellia.core.api;

/**
 * @author tasszz2k
 * @since 11/11/2022
 */
public class DataWithMd5Response<T> {
    private int code;
    private T data;
    private String md5;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}
