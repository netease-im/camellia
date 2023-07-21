package com.netease.nim.camellia.http.accelerate.proxy.core.console;

import com.netease.nim.camellia.http.accelerate.proxy.core.monitor.ProxyMonitor;
import com.netease.nim.camellia.http.accelerate.proxy.core.proxy.IHttpAccelerateProxy;
import com.netease.nim.camellia.http.accelerate.proxy.core.status.ServerStartupStatus;
import com.netease.nim.camellia.http.accelerate.proxy.core.status.ServerStatus;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.ITransportServer;
import com.netease.nim.camellia.http.console.ConsoleResult;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.ITransportRouter;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream.IUpstreamRouter;
import com.netease.nim.camellia.http.console.ConsoleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ConsoleServiceAdaptor implements ConsoleService {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleServiceAdaptor.class);

    private final ITransportRouter transportRouter;
    private final IUpstreamRouter upstreamRouter;
    private final ITransportServer transportServer;
    private final IHttpAccelerateProxy proxy;

    public ConsoleServiceAdaptor(ITransportRouter transportRouter, IUpstreamRouter upstreamRouter, ITransportServer transportServer, IHttpAccelerateProxy proxy) {
        this.transportRouter = transportRouter;
        this.upstreamRouter = upstreamRouter;
        this.transportServer = transportServer;
        this.proxy = proxy;
    }

    @Override
    public ConsoleResult status() {
        boolean online = ServerStatus.getStatus() == ServerStatus.Status.ONLINE;
        if (transportServer.getStatus() == ServerStartupStatus.FAIL) {
            online = false;
        }
        if (proxy.getStatus() == ServerStartupStatus.FAIL) {
            online = false;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("online = {}", online);
        }
        if (online) {
            return ConsoleResult.success(ServerStatus.Status.ONLINE.name());
        } else {
            return ConsoleResult.error(ServerStatus.Status.OFFLINE.name());
        }
    }

    @Override
    public ConsoleResult online() {
        logger.info("online success");
        ServerStatus.setStatus(ServerStatus.Status.ONLINE);
        return ConsoleResult.success();
    }

    @Override
    public ConsoleResult offline() {
        ServerStatus.setStatus(ServerStatus.Status.OFFLINE);
        if (ServerStatus.isIdle()) {
            logger.info("offline success");
            return ConsoleResult.success("is idle");
        } else {
            logger.info("try offline, but not idle");
            return ConsoleResult.error("not idle");
        }
    }

    @Override
    public ConsoleResult check() {
        return ConsoleResult.success();
    }

    @Override
    public ConsoleResult monitor() {
        return ConsoleResult.success(ProxyMonitor.getMonitorJson().toJSONString());
    }

    @Override
    public ConsoleResult reload() {
        transportRouter.reload();
        upstreamRouter.reload();
        return ConsoleResult.success();
    }
}
