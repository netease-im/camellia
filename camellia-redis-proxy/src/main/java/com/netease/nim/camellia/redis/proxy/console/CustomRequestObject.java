package com.netease.nim.camellia.redis.proxy.console;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.util.List;
import java.util.Map;

public class CustomRequestObject {
    private HttpVersion version = null;
    private HttpMethod method = null;
    private String host = null;
    private String remoteip = null;
    private String uri = null;
    private HttpHeaders headers = null;
    private HttpHeaders tailHeaders = null;
    private Map<String, List<String>> params = null;
    private String content = null;
    private Throwable failure = null;
    private boolean contentComplete = false;

    public static CustomRequestObject getEmpty() {
        return new CustomRequestObject();
    }

    public HttpVersion getVersion() {
        return version;
    }

    public void setVersion(HttpVersion version) {
        this.version = version;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUriNoParam() {
        if (uri == null)
            return null;
        return uri.split("\\?")[0];
    }
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

    public Map<String, List<String>> getParams() {
        return params;
    }

    public void setParams(Map<String, List<String>> params) {
        this.params = params;
    }

    public String getParam(String key) {
        if (params == null || params.isEmpty())
            return null;
        List<String> l = params.get(key);
        if (l == null || l.isEmpty())
            return null;
        return l.get(0);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Throwable getFailure() {
        return failure;
    }

    public void setFailure(Throwable failure) {
        this.failure = failure;
    }

    public boolean isContentComplete() {
        return contentComplete;
    }

    public void setContentComplete(boolean contentComplete) {
        this.contentComplete = contentComplete;
    }

    public HttpHeaders getTailHeaders() {
        return tailHeaders;
    }

    public void setTailHeaders(HttpHeaders tailHeaders) {
        this.tailHeaders = tailHeaders;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public String getRemoteip() {
        return remoteip;
    }

    public void setRemoteip(String remoteip) {
        this.remoteip = remoteip;
    }

    public boolean decodeSuccess() {
        return failure == null;
    }
}
