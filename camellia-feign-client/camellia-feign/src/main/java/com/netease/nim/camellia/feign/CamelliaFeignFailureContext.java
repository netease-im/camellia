package com.netease.nim.camellia.feign;

import com.netease.nim.camellia.core.model.Resource;


/**
 * Created by caojiajun on 2022/7/5
 */
public class CamelliaFeignFailureContext {

    private final long bid;
    private final String bgroup;
    private final Class<?> apiType;
    private final byte operationType;//1表示write，2表示read，3表示未知
    private final Resource resource;
    private final Object loadBalanceKey;
    private final String method;
    private final Object[] objects;
    private final Throwable exception;

    public CamelliaFeignFailureContext(long bid, String bgroup, Class<?> apiType, byte operationType, Resource resource,
                                       Object loadBalanceKey, String method, Object[] objects, Throwable exception) {
        this.bid = bid;
        this.bgroup = bgroup;
        this.apiType = apiType;
        this.operationType = operationType;
        this.resource = resource;
        this.loadBalanceKey = loadBalanceKey;
        this.method = method;
        this.objects = objects;
        this.exception = exception;
    }

    public long getBid() {
        return bid;
    }

    public String getBgroup() {
        return bgroup;
    }

    public Class<?> getApiType() {
        return apiType;
    }

    public byte getOperationType() {
        return operationType;
    }

    public Resource getResource() {
        return resource;
    }

    public Object getLoadBalanceKey() {
        return loadBalanceKey;
    }

    public String getMethod() {
        return method;
    }

    public Object[] getObjects() {
        return objects;
    }

    public Throwable getException() {
        return exception;
    }
}
