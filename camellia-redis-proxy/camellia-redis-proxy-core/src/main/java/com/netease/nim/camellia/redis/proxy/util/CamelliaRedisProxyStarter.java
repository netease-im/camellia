package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.http.console.CamelliaHttpConsoleConfig;
import com.netease.nim.camellia.http.console.CamelliaHttpConsoleServer;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.conf.ServerConf;
import com.netease.nim.camellia.redis.proxy.console.ConsoleService;
import com.netease.nim.camellia.redis.proxy.console.ConsoleServiceAdaptor;
import com.netease.nim.camellia.redis.proxy.monitor.*;
import com.netease.nim.camellia.redis.proxy.monitor.model.Stats;
import com.netease.nim.camellia.redis.proxy.netty.CamelliaRedisProxyServer;
import com.netease.nim.camellia.redis.proxy.conf.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.base.resource.RedisResourceUtil;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyBeanFactory;
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

    private static final CamelliaServerProperties properties = new CamelliaServerProperties();

    public static void start() {
        start(0, null, null);
    }

    public static void start(boolean enableConsole) {
        if (enableConsole) {
            start(Constants.Server.consolePort, new ConsoleServiceAdaptor(), null);
        } else {
            start();
        }
    }

    public static void start(int consolePort) {
        start(consolePort, new ConsoleServiceAdaptor(), null);
    }

    public static void start(int consolePort, ConsoleService consoleService, ProxyBeanFactory proxyBeanFactory) {
        if (starting.compareAndSet(false, true)) {
            try {
                if (startOk.get()) {
                    logger.warn("CamelliaRedisProxyServer has started");
                    return;
                }
                ServerConf.init(properties, -1, null, proxyBeanFactory);

                CamelliaRedisProxyServer server = new CamelliaRedisProxyServer();
                server.start();
                logger.info("CamelliaRedisProxyServer start success");
                if (consolePort != 0 && consoleService != null) {
                    CamelliaHttpConsoleConfig config = new CamelliaHttpConsoleConfig();
                    config.setPort(consolePort);
                    config.setConsoleService(consoleService);
                    CamelliaHttpConsoleServer consoleServer = new CamelliaHttpConsoleServer(config);
                    ChannelFuture future = consoleServer.start();
                    GlobalRedisProxyEnv.setConsolePort(config.getPort());
                    GlobalRedisProxyEnv.getProxyShutdown().setConsoleFuture(future);
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
    public static Stats getStats() {
        try {
            return ProxyMonitorCollector.getStats();
        } catch (Exception e) {
            logger.error("getStats error", e);
            return null;
        }
    }

    public static boolean isStart() {
        return startOk.get();
    }

    public static CamelliaServerProperties getProperties() {
        return properties;
    }

    public static void updatePort(int port) {
        properties.getConfig().put("port", String.valueOf(port));
    }

    public static void updatePassword(String password) {
        properties.getConfig().put("password", password);
    }

    public static void updateRouteConf(String conf) {
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(conf);
        RedisResourceUtil.checkResourceTable(resourceTable);
        properties.getConfig().put("route.conf", conf);
    }
}
