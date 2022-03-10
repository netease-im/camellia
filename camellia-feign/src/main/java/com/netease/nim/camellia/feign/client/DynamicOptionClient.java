package com.netease.nim.camellia.feign.client;

import feign.Client;
import feign.Request;
import feign.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2022/3/8
 */
public class DynamicOptionClient implements Client {

    private final Client client;
    private final DynamicOption dynamicOption;

    public DynamicOptionClient(Client client, DynamicOption dynamicOption) {
        this.client = client;
        this.dynamicOption = dynamicOption;
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        Long connectTimeout = dynamicOption.getConnectTimeout();
        if (connectTimeout == null) {
            connectTimeout = options.connectTimeout();
        }
        TimeUnit connectTimeoutUnit = dynamicOption.getConnectTimeoutUnit();
        if (connectTimeoutUnit == null) {
            connectTimeoutUnit = options.connectTimeoutUnit();
        }
        Long readTimeout = dynamicOption.getReadTimeout();
        if (readTimeout == null) {
            readTimeout = options.readTimeout();
        }
        TimeUnit readTimeoutUnit = dynamicOption.getReadTimeoutUnit();
        if (readTimeoutUnit == null) {
            readTimeoutUnit = options.readTimeoutUnit();
        }
        Boolean followRedirects = dynamicOption.isFollowRedirects();
        if (followRedirects == null) {
            followRedirects = options.isFollowRedirects();
        }
        return client.execute(request, new Request.Options(connectTimeout, connectTimeoutUnit, readTimeout, readTimeoutUnit, followRedirects));
    }
}
