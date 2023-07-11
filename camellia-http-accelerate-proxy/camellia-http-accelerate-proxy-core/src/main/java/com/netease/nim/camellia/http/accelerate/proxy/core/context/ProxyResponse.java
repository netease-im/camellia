package com.netease.nim.camellia.http.accelerate.proxy.core.context;

import io.netty.handler.codec.http.FullHttpResponse;

/**
 * Created by caojiajun on 2023/7/10
 */
public class ProxyResponse {

    private FullHttpResponse response;
    private LogBean logBean;

    public ProxyResponse(FullHttpResponse response, LogBean logBean) {
        this.response = response;
        this.logBean = logBean;
    }

    public FullHttpResponse getResponse() {
        return response;
    }

    public void setResponse(FullHttpResponse response) {
        this.response = response;
    }

    public LogBean getLogBean() {
        return logBean;
    }

    public void setLogBean(LogBean logBean) {
        this.logBean = logBean;
    }
}
