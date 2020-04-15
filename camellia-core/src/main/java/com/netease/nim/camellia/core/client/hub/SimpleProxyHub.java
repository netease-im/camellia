package com.netease.nim.camellia.core.client.hub;

/**
 *
 * Created by caojiajun on 2019/5/16.
 */
public class SimpleProxyHub<T> implements IProxyHub<T> {

    private T proxy;

    public SimpleProxyHub(T proxy) {
        this.proxy = proxy;
    }

    @Override
    public T chooseProxy(byte[]... key) {
        return proxy;
    }
}
