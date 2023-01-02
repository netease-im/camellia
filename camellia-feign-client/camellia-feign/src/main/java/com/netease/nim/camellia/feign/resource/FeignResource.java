package com.netease.nim.camellia.feign.resource;

import com.netease.nim.camellia.core.model.Resource;

/**
 * Created by caojiajun on 2022/3/1
 */
public class FeignResource extends Resource {

    private final String feignUrl;
    private final String protocol;

    public FeignResource(String feignUrl) {
        String url = FeignType.Feign.getPrefix() + feignUrl;
        setUrl(url);
        String[] split = feignUrl.split("//");
        this.protocol = split[0] + "//";
        this.feignUrl = feignUrl;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getFeignUrl() {
        return feignUrl;
    }
}
