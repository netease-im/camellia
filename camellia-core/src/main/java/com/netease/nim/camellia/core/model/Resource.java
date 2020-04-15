package com.netease.nim.camellia.core.model;

import com.netease.nim.camellia.core.util.ResourceUtil;

/**
 *
 * Created by caojiajun on 2019/11/7.
 */
public class Resource {
    private String url;

    public Resource() {
    }

    public Resource(String url) {
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
        return ResourceUtil.resourceEquals(this, o);
    }

    @Override
    public int hashCode() {
        return ResourceUtil.resourceHashCode(this);
    }

    @Override
    public String toString() {
        return url;
    }
}
