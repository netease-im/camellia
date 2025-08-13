package com.netease.nim.camellia.tools.http;

import com.alibaba.fastjson.JSONObject;

import java.nio.charset.StandardCharsets;

public class HttpResponse {
    private final int httpCode;
    private final byte[] data;

    public HttpResponse(int httpCode, byte[] data) {
        this.httpCode = httpCode;
        this.data = data;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public byte[] getData() {
        return data;
    }

    public JSONObject getJson() {
        if (data == null) {
            return null;
        }
        return JSONObject.parseObject(new String(data, StandardCharsets.UTF_8));
    }

    public String getString() {
        if (data == null) {
            return null;
        }
        return new String(data, StandardCharsets.UTF_8);
    }
}
