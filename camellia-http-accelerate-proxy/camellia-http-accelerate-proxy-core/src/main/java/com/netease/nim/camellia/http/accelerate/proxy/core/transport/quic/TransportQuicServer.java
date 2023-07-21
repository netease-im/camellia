package com.netease.nim.camellia.http.accelerate.proxy.core.transport.quic;

import com.netease.nim.camellia.http.accelerate.proxy.core.status.ServerStartupStatus;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.ITransportServer;

/**
 * Created by caojiajun on 2023/7/6
 */
public class TransportQuicServer implements ITransportServer {
    @Override
    public void start() {
        //TODO
    }

    @Override
    public ServerStartupStatus getStatus() {
        return ServerStartupStatus.FAIL;
    }
}
