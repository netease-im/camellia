package com.netease.nim.camellia.delayqueue.sdk.api;

import java.util.Objects;

/**
 * Created by caojiajun on 2022/7/13
 */
public class DelayQueueServer {

    private String url;

    public DelayQueueServer(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DelayQueueServer that = (DelayQueueServer) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}
