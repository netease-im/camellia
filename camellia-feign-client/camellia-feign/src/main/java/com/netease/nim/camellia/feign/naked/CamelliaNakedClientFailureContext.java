package com.netease.nim.camellia.feign.naked;

import com.netease.nim.camellia.core.model.Resource;

/**
 * Created by caojiajun on 2022/7/4
 */
public class CamelliaNakedClientFailureContext<R> {

    private final CamelliaNakedClient.OperationType operationType;
    private final long bid;
    private final String bgroup;
    private final R request;
    private final Object loadBalanceKey;
    private final Resource resource;
    private final Throwable exception;

    public CamelliaNakedClientFailureContext(long bid, String bgroup, CamelliaNakedClient.OperationType operationType,
                                             R request, Object loadBalanceKey, Resource resource, Throwable exception) {
        this.operationType = operationType;
        this.bid = bid;
        this.bgroup = bgroup;
        this.request = request;
        this.loadBalanceKey = loadBalanceKey;
        this.resource = resource;
        this.exception = exception;
    }

    public CamelliaNakedClient.OperationType getOperationType() {
        return operationType;
    }

    public long getBid() {
        return bid;
    }

    public String getBgroup() {
        return bgroup;
    }

    public R getRequest() {
        return request;
    }

    public Object getLoadBalanceKey() {
        return loadBalanceKey;
    }

    public Resource getResource() {
        return resource;
    }

    public Throwable getException() {
        return exception;
    }
}
