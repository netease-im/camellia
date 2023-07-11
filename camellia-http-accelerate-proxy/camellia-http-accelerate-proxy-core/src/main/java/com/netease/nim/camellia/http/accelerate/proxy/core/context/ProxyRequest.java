package com.netease.nim.camellia.http.accelerate.proxy.core.context;

import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Created by caojiajun on 2023/7/10
 */
public class ProxyRequest {
    private FullHttpRequest request;
    private LogBean logBean;

    public ProxyRequest(FullHttpRequest request, LogBean logBean) {
        this.request = request;
        this.logBean = logBean;
    }

    public FullHttpRequest getRequest() {
        return request;
    }

    public void setRequest(FullHttpRequest request) {
        this.request = request;
    }

    public LogBean getLogBean() {
        return logBean;
    }

    public void setLogBean(LogBean logBean) {
        this.logBean = logBean;
    }
}
