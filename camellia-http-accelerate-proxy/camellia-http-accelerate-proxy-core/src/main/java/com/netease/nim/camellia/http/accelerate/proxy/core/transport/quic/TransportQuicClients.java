package com.netease.nim.camellia.http.accelerate.proxy.core.transport.quic;

import com.netease.nim.camellia.http.accelerate.proxy.core.transport.AbstractTransportClients;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.Client;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.model.DynamicAddrs;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.model.ServerAddr;
import com.netease.nim.camellia.tools.base.DynamicValueGetter;

/**
 * Created by caojiajun on 2023/7/25
 */
public class TransportQuicClients extends AbstractTransportClients {
    public TransportQuicClients(DynamicAddrs dynamicAddrs, DynamicValueGetter<Integer> connectCount) {
        super(dynamicAddrs, connectCount);
    }

    @Override
    public Client initClinet(ServerAddr addr) {
        return new QuicClient(addr);
    }
}
