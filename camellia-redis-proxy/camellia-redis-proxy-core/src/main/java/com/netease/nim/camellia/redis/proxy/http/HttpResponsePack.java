package com.netease.nim.camellia.redis.proxy.http;

import io.netty.handler.codec.http.HttpResponse;

/**
 * Created by caojiajun on 2024/1/21
 */
public class HttpResponsePack {
    private final HttpResponse httpResponse;
    private final long id;

    public HttpResponsePack(HttpResponse httpResponse, long id) {
        this.httpResponse = httpResponse;
        this.id = id;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public long getId() {
        return id;
    }
}
