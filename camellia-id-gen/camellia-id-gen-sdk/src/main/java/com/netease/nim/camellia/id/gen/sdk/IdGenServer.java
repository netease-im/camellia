package com.netease.nim.camellia.id.gen.sdk;

import java.util.Objects;

/**
 * Created by caojiajun on 2021/9/29
 */
public class IdGenServer {

    private String url;

    public IdGenServer(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "IdGenServer{" +
                "url='" + url + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdGenServer that = (IdGenServer) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}
