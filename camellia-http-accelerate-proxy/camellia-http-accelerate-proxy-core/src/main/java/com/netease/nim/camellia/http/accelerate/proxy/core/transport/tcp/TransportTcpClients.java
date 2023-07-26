package com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp;

import com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.config.TransportServerType;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.Client;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.AbstractTransportClients;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.model.DynamicAddrs;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.model.ServerAddr;
import com.netease.nim.camellia.tools.base.DynamicValueGetter;

/**
 * Created by caojiajun on 2023/7/25
 */
public class TransportTcpClients extends AbstractTransportClients {
    public TransportTcpClients(DynamicAddrs dynamicAddrs, DynamicValueGetter<Integer> connectCount) {
        super(dynamicAddrs, connectCount);
    }

    @Override
    public Client initClinet(ServerAddr addr) {
        return new TcpClient(addr);
    }

    @Override
    public TransportServerType transportServerType() {
        return TransportServerType.tcp;
    }
}
