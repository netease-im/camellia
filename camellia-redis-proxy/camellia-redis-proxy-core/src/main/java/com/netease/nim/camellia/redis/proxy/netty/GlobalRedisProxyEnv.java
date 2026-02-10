package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.base.proxy.ProxyDiscoveryFactory;
import com.netease.nim.camellia.redis.proxy.command.QueueFactory;
import com.netease.nim.camellia.redis.proxy.conf.EventLoopGroupResult;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;
import com.netease.nim.camellia.redis.proxy.upstream.RedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.util.ConfigInitUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2021/4/2
 */
public class GlobalRedisProxyEnv {

    private static final Logger logger = LoggerFactory.getLogger(GlobalRedisProxyEnv.class);

    private static int port;
    private static int tlsPort;
    private static int cport;
    private static int consolePort;
    private static String udsPath;
    private static int httpPort;

    private static EventLoopGroupResult tcpEventLoopGroupResult;
    private static EventLoopGroupResult httpEventLoopGroupResult;
    private static EventLoopGroupResult udsEventLoopGroupResult;

    private static IUpstreamClientTemplateFactory clientTemplateFactory;
    private static RedisProxyEnv redisProxyEnv;
    private static ProxyDiscoveryFactory discoveryFactory;

    private static volatile QueueFactory queueFactory;

    private static final DefaultProxyShutdown proxyShutdown = new DefaultProxyShutdown();

    private static final Set<Runnable> beforeStartCallbackSet = new HashSet<>();
    private static final Set<Runnable> afterStartCallbackSet = new HashSet<>();

    public static int getPort() {
        return port;
    }

    public static void setPort(int port) {
        GlobalRedisProxyEnv.port = port;
    }

    public static int getTlsPort() {
        return tlsPort;
    }

    public static void setTlsPort(int tlsPort) {
        GlobalRedisProxyEnv.tlsPort = tlsPort;
    }

    public static int getCport() {
        return cport;
    }

    public static void setCport(int cport) {
        GlobalRedisProxyEnv.cport = cport;
    }

    public static int getConsolePort() {
        return consolePort;
    }

    public static void setConsolePort(int consolePort) {
        GlobalRedisProxyEnv.consolePort = consolePort;
    }

    public static String getUdsPath() {
        return udsPath;
    }

    public static void setUdsPath(String udsPath) {
        GlobalRedisProxyEnv.udsPath = udsPath;
    }

    public static int getHttpPort() {
        return httpPort;
    }

    public static void setHttpPort(int httpPort) {
        GlobalRedisProxyEnv.httpPort = httpPort;
    }

    public static ProxyDiscoveryFactory getDiscoveryFactory() {
        return discoveryFactory;
    }

    public static void setDiscoveryFactory(ProxyDiscoveryFactory discoveryFactory) {
        GlobalRedisProxyEnv.discoveryFactory = discoveryFactory;
    }

    public static RedisProxyEnv getRedisProxyEnv() {
        return redisProxyEnv;
    }

    public static void setRedisProxyEnv(RedisProxyEnv redisProxyEnv) {
        GlobalRedisProxyEnv.redisProxyEnv = redisProxyEnv;
    }

    public static IUpstreamClientTemplateFactory getClientTemplateFactory() {
        return clientTemplateFactory;
    }

    public static void setClientTemplateFactory(IUpstreamClientTemplateFactory clientTemplateFactory) {
        GlobalRedisProxyEnv.clientTemplateFactory = clientTemplateFactory;
    }

    public static EventLoopGroupResult getUdsEventLoopGroupResult() {
        return udsEventLoopGroupResult;
    }

    public static void setUdsEventLoopGroupResult(EventLoopGroupResult udsEventLoopGroupResult) {
        GlobalRedisProxyEnv.udsEventLoopGroupResult = udsEventLoopGroupResult;
    }

    public static EventLoopGroupResult getHttpEventLoopGroupResult() {
        return httpEventLoopGroupResult;
    }

    public static void setHttpEventLoopGroupResult(EventLoopGroupResult httpEventLoopGroupResult) {
        GlobalRedisProxyEnv.httpEventLoopGroupResult = httpEventLoopGroupResult;
    }

    public static EventLoopGroupResult getTcpEventLoopGroupResult() {
        return tcpEventLoopGroupResult;
    }

    public static void setTcpEventLoopGroupResult(EventLoopGroupResult tcpEventLoopGroupResult) {
        GlobalRedisProxyEnv.tcpEventLoopGroupResult = tcpEventLoopGroupResult;
    }

    public static DefaultProxyShutdown getProxyShutdown() {
        return proxyShutdown;
    }

    public static QueueFactory getQueueFactory() {
        if (queueFactory == null) {
            synchronized (QueueFactory.class) {
                if (queueFactory == null) {
                    queueFactory = ConfigInitUtil.initQueueFactory();
                }
            }
        }
        return queueFactory;
    }

    public static synchronized void addBeforeStartCallback(Runnable callback) {
        beforeStartCallbackSet.add(callback);
    }

    public static synchronized void addAfterStartCallback(Runnable callback) {
        afterStartCallbackSet.add(callback);
    }

    public static void invokeAfterStartCallback() {
        for (Runnable runnable : afterStartCallbackSet) {
            try {
                runnable.run();
            } catch (Exception e) {
                logger.error("after start callback error", e);
                throw new IllegalStateException(e);
            }
        }
    }

    public static void invokeBeforeStartCallback() {
        for (Runnable runnable : beforeStartCallbackSet) {
            try {
                runnable.run();
            } catch (Exception e) {
                logger.error("before start callback error", e);
                throw new IllegalStateException(e);
            }
        }
    }

}
