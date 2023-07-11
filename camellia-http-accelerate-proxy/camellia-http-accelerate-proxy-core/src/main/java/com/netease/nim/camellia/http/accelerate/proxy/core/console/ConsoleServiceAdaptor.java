package com.netease.nim.camellia.http.accelerate.proxy.core.console;

import com.netease.nim.camellia.http.accelerate.proxy.core.monitor.ProxyMonitor;
import com.netease.nim.camellia.http.accelerate.proxy.core.status.ServerStatus;
import com.netease.nim.camellia.http.console.ConsoleResult;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.ITransportRouter;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream.IUpstreamRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleServiceAdaptor implements ConsoleService {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleServiceAdaptor.class);

    private final ITransportRouter transportRouter;
    private final IUpstreamRouter upstreamRouter;

    public ConsoleServiceAdaptor(ITransportRouter transportRouter, IUpstreamRouter upstreamRouter) {
        this.transportRouter = transportRouter;
        this.upstreamRouter = upstreamRouter;
    }

    @Override
    public ConsoleResult status() {
        ServerStatus.Status status = ServerStatus.getStatus();
        if (logger.isDebugEnabled()) {
            logger.debug("status = {}", status.name());
        }
        boolean online = false;
        if (status == ServerStatus.Status.ONLINE) {
            online = true;
        } else if (status == ServerStatus.Status.OFFLINE) {
            online = !ServerStatus.isIdle();
        }
        if (online) {
            return ConsoleResult.success(status.name());
        } else {
            return ConsoleResult.error(status.name());
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
