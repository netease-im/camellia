package com.netease.nim.camellia.console.util;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class RespBody {
    private int httpCode;
    private String httpMessage;
    private String data;

    public int getHttpCode() {
        return httpCode;
    }

    public void setHttpCode(int httpCode) {
        this.httpCode = httpCode;
    }

    public String getHttpMessage() {
        return httpMessage;
    }

    public void setHttpMessage(String httpMessage) {
        this.httpMessage = httpMessage;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "RespBody{" +
                "httpCode=" + httpCode +
                ", httpMessage='" + httpMessage + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}
