package com.netease.nim.camellia.hot.key.server.console;

import com.netease.nim.camellia.hot.key.server.calculate.TopNStatsResult;
import com.netease.nim.camellia.hot.key.server.conf.ConfReloadHolder;
import com.netease.nim.camellia.hot.key.server.monitor.*;
import com.netease.nim.camellia.hot.key.server.netty.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class ConsoleServiceAdaptor implements ConsoleService {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleServiceAdaptor.class);

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
        HotKeyServerStats serverStats = HotKeyServerMonitorCollector.getHotKeyServerStats();
        return ConsoleResult.success(StatsJsonConverter.converter(serverStats));
    }

    @Override
    public ConsoleResult topN(String namespace) {
        TopNStatsResult result = TopNMonitor.getTopNStatsResult(namespace);
        if (result == null) {
            return ConsoleResult.error();
        }
        return ConsoleResult.success(TopNStatsResultJsonConverter.converter(result));
    }

    @Override
    public ConsoleResult prometheus() {
        HotKeyServerStats serverStats = HotKeyServerMonitorCollector.getHotKeyServerStats();
        return ConsoleResult.success(StatsPrometheusConverter.converter(serverStats));
    }

    @Override
    public ConsoleResult reload() {
        ConfReloadHolder.reload();
        logger.info("conf reload success");
        return ConsoleResult.success();
    }

    @Override
    public ConsoleResult custom(Map<String, List<String>> params) {
        if (logger.isDebugEnabled()) {
            logger.debug("custom, params = {}", params);
        }
        return ConsoleResult.success();
    }
}
