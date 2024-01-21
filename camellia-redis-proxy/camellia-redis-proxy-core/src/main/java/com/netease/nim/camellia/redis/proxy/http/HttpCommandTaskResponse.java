package com.netease.nim.camellia.redis.proxy.http;

import io.netty.handler.codec.http.HttpResponse;

/**
 * Created by caojiajun on 2024/1/21
 */
public class HttpCommandTaskResponse {

    private HttpResponse httpResponse;
    private boolean keepalive;

    public HttpCommandTaskResponse(HttpResponse httpResponse, boolean keepalive) {
        this.httpResponse = httpResponse;
        this.keepalive = keepalive;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public void setHttpResponse(HttpResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    public boolean isKeepalive() {
        return keepalive;
    }

    public void setKeepalive(boolean keepalive) {
        this.keepalive = keepalive;
    }
}
