package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.http.console.CamelliaHttpConsoleConfig;
import com.netease.nim.camellia.http.console.CamelliaHttpConsoleServer;
import com.netease.nim.camellia.redis.proxy.command.CommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.console.ConsoleService;
import com.netease.nim.camellia.redis.proxy.console.ConsoleServiceAdaptor;
import com.netease.nim.camellia.redis.proxy.monitor.*;
import com.netease.nim.camellia.redis.proxy.monitor.model.Stats;
import com.netease.nim.camellia.redis.proxy.netty.CamelliaRedisProxyServer;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.base.resource.RedisResourceUtil;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 一个简单的可以直接启动proxy的工具方法
 * Created by caojiajun on 2021/8/3
 */
public class CamelliaRedisProxyStarter {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisProxyStarter.class);

    private static final AtomicBoolean starting = new AtomicBoolean(false);
    private static final AtomicBoolean startOk = new AtomicBoolean(false);

    private static final CamelliaServerProperties serverProperties = new CamelliaServerProperties();
    private static final CamelliaTranspondProperties transpondProperties = new CamelliaTranspondProperties();

    public static void start() {
        start(0, null);
    }

    public static void start(boolean enableConsole) {
        if (enableConsole) {
            start(Constants.Server.consolePort, new ConsoleServiceAdaptor());
        } else {
            start();
        }
    }

    public static void start(int consolePort) {
        start(consolePort, new ConsoleServiceAdaptor());
    }

    public static void start(int consolePort, ConsoleService consoleService) {
        if (starting.compareAndSet(false, true)) {
            try {
                if (startOk.get()) {
                    logger.warn("CamelliaRedisProxyServer has started");
                    return;
                }
                GlobalRedisProxyEnv.init(serverProperties);
                CommandInvoker commandInvoker = new CommandInvoker(serverProperties, transpondProperties);
                CamelliaRedisProxyServer server = new CamelliaRedisProxyServer(serverProperties, commandInvoker);
                server.start();
                logger.info("CamelliaRedisProxyServer start success");
                if (consolePort != 0 && consoleService != null) {
                    CamelliaHttpConsoleConfig config = new CamelliaHttpConsoleConfig();
                    config.setPort(consolePort);
                    config.setConsoleService(consoleService);
                    CamelliaHttpConsoleServer consoleServer = new CamelliaHttpConsoleServer(config);
                    ChannelFuture future = consoleServer.start();
                    GlobalRedisProxyEnv.setConsolePort(config.getPort());
                    GlobalRedisProxyEnv.getProxyShutdown().setConsoleChannelFuture(future);
                }
                startOk.set(true);
            } catch (Throwable e) {
                logger.error("CamelliaRedisProxyServer start error", e);
            } finally {
                starting.compareAndSet(true, false);
            }
        }
    }

    /**
     * 获取监控数据
     * @return monitor string
     */
    public static String getRedisProxyMonitorString() {
        try {
            Stats stats = ProxyMonitorCollector.getStats();
            return StatsJsonConverter.converter(stats);
        } catch (Exception e) {
            logger.error("getRedisProxyMonitorString error", e);
            return "";
        }
    }

    public static boolean isStart() {
        return startOk.get();
    }

    public static CamelliaServerProperties getServerProperties() {
        return serverProperties;
    }

    public static CamelliaTranspondProperties getTranspondProperties() {
        return transpondProperties;
    }

    public static void updatePort(int port) {
        serverProperties.setPort(port);
    }

    public static void updatePassword(String password) {
        serverProperties.setPassword(password);
    }

    public static void updateRouteConf(String conf) {
        transpondProperties.setType(CamelliaTranspondProperties.Type.LOCAL);
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(conf);
        RedisResourceUtil.checkResourceTable(resourceTable);
        CamelliaTranspondProperties.LocalProperties local = new CamelliaTranspondProperties.LocalProperties();
        local.setResourceTable(resourceTable);
        transpondProperties.setLocal(local);
    }
}
