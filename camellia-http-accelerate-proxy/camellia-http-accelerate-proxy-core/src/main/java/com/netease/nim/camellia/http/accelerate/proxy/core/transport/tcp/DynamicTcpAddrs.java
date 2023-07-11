package com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp;

import java.util.List;

/**
 * Created by caojiajun on 2023/7/7
 */
public interface DynamicTcpAddrs {

    List<TcpAddr> getAddrs();

}
