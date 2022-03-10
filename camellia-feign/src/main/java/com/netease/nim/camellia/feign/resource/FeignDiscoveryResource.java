package com.netease.nim.camellia.feign.resource;

import com.netease.nim.camellia.core.model.Resource;

/**
 * Created by caojiajun on 2022/3/1
 */
public class FeignDiscoveryResource extends Resource {

    private final String discoveryUrl;
    private final String protocol;
    private final String serviceName;

    public FeignDiscoveryResource(String discoveryUrl) {
        String url = FeignType.FeignDiscovery.getPrefix() + discoveryUrl;
        setUrl(url);
        String[] split = discoveryUrl.split("//");
        this.protocol = split[0] + "//";
        this.serviceName = split[1];
        this.discoveryUrl = discoveryUrl;
    }

    public String getDiscoveryUrl() {
        return discoveryUrl;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getServiceName() {
        return serviceName;
    }

}
