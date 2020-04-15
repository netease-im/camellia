package com.netease.nim.camellia.core.client.hub;

/**
 *
 * Created by caojiajun on 2019/5/16.
 */
public interface IProxyHub<T> {

    T chooseProxy(byte[]... key);
}
