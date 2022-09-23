package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.command.AsyncCommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.monitor.*;
import com.netease.nim.camellia.redis.proxy.monitor.model.Stats;
import com.netease.nim.camellia.redis.proxy.netty.CamelliaRedisProxyServer;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.resource.RedisResourceUtil;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
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
        if (starting.compareAndSet(false, true)) {
            try {
                if (startOk.get()) {
                    logger.warn("CamelliaRedisProxyServer has started");
                    return;
                }
                EventLoopGroup bossGroup = new NioEventLoopGroup(serverProperties.getBossThread());
                EventLoopGroup workGroup = new NioEventLoopGroup(serverProperties.getWorkThread());
                GlobalRedisProxyEnv.workGroup = workGroup;
                GlobalRedisProxyEnv.bossGroup = bossGroup;
                GlobalRedisProxyEnv.workThread = serverProperties.getWorkThread();
                GlobalRedisProxyEnv.bossThread = serverProperties.getBossThread();
                AsyncCommandInvoker commandInvoker = new AsyncCommandInvoker(serverProperties, transpondProperties);
                CamelliaRedisProxyServer server = new CamelliaRedisProxyServer(serverProperties, bossGroup, workGroup, commandInvoker);
                server.start();
                logger.info("CamelliaRedisProxyServer start success");
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
