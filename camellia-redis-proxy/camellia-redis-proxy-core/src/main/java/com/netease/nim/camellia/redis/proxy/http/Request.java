package com.netease.nim.camellia.redis.proxy.http;

import io.netty.handler.codec.http.HttpVersion;

/**
 * Created by caojiajun on 2024/1/21
 */
public class Request {
    private HttpVersion httpVersion;
    private boolean keepAlive;
    private HttpCommandRequest httpCommandRequest;

    public HttpVersion getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public HttpCommandRequest getHttpCommandRequest() {
        return httpCommandRequest;
    }

    public void setHttpCommandRequest(HttpCommandRequest httpCommandRequest) {
        this.httpCommandRequest = httpCommandRequest;
    }
}
