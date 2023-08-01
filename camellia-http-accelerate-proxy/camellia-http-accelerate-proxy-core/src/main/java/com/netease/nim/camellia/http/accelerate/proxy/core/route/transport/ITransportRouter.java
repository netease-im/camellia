package com.netease.nim.camellia.http.accelerate.proxy.core.route.transport;

import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyRequest;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.ITransportClient;

/**
 * Created by caojiajun on 2023/7/7
 */
public interface ITransportRouter {

    void start();

    void reload();

    ITransportClient select(ProxyRequest request);

    ITransportClient selectBackup(ProxyRequest request);
}
