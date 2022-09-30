package com.netease.nim.camellia.redis.proxy.cluster;

/**
 * Created by caojiajun on 2022/9/29
 */
public interface ProxyNodeChangeListener {

    void addNode(ProxyNode node);

    void removeNode(ProxyNode node);
}
