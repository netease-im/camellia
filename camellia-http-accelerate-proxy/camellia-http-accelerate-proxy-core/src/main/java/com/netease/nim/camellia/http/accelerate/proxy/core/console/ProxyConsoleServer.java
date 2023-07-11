package com.netease.nim.camellia.http.accelerate.proxy.core.console;

import com.netease.nim.camellia.http.console.CamelliaHttpConsoleConfig;
import com.netease.nim.camellia.http.console.CamelliaHttpConsoleServer;
import com.netease.nim.camellia.http.accelerate.proxy.core.conf.DynamicConf;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.ITransportRouter;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream.IUpstreamRouter;

/**
 * Created by caojiajun on 2023/7/10
 */
public class ProxyConsoleServer {

    public void start(ITransportRouter transportRouter, IUpstreamRouter upstreamRouter) {
        CamelliaHttpConsoleConfig config = new CamelliaHttpConsoleConfig();
        config.setPort(DynamicConf.getInt("console.port", 11700));
        config.setConsoleService(new ConsoleServiceAdaptor(transportRouter, upstreamRouter));
        CamelliaHttpConsoleServer consoleServer = new CamelliaHttpConsoleServer(config);
        consoleServer.start();
    }

}
