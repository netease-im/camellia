package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.base.proxy.ProxyDiscoveryFactory;
import com.netease.nim.camellia.redis.proxy.command.DefaultQueueFactory;
import com.netease.nim.camellia.redis.proxy.command.QueueFactory;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyBeanFactory;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;
import com.netease.nim.camellia.redis.proxy.upstream.RedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.*;
import io.netty.channel.kqueue.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * Created by caojiajun on 2021/4/2
 */
public class GlobalRedisProxyEnv {

    private static final Logger logger = LoggerFactory.getLogger(GlobalRedisProxyEnv.class);

    private static CamelliaServerProperties serverProperties;
    private static CamelliaTranspondProperties transpondProperties;

    private static final String BOSS_GROUP_NAME = "camellia-boss-group";
    private static final String WORK_GROUP_NAME = "camellia-work-group";
    private static final String UDS_BOSS_GROUP_NAME = "camellia-uds-boss-group";
    private static final String UDS_WORK_GROUP_NAME = "camellia-uds-work-group";

    private static final AtomicBoolean initOk = new AtomicBoolean(false);

    private static NettyTransportMode nettyTransportMode = NettyTransportMode.auto;

    private static int bossThread;
    private static EventLoopGroup bossGroup;
    private static EventLoopGroup udsBossGroup;
    private static int workThread;
    private static EventLoopGroup workGroup;
    private static EventLoopGroup udsWorkGroup;
    private static Class<? extends ServerChannel> serverChannelClass = NioServerSocketChannel.class;
    private static Class<? extends ServerChannel> serverUdsChannelClass = null;
    private static boolean serverTcpQuickAck = false;

    private static int port;
    private static int tlsPort;
    private static int cport;
    private static int consolePort;
    private static String udsPath;
    private static int httpPort;

    private static String cportPassword;

    private static IUpstreamClientTemplateFactory clientTemplateFactory;
    private static RedisProxyEnv redisProxyEnv;
    private static ProxyDiscoveryFactory discoveryFactory;
    private static boolean clusterModeEnable;
    private static boolean sentinelModeEnable;

    private static QueueFactory queueFactory = new DefaultQueueFactory();

    private static ProxyBeanFactory proxyBeanFactory;

    private static final DefaultProxyShutdown proxyShutdown = new DefaultProxyShutdown();

    private static final Set<Runnable> beforeStartCallbackSet = new HashSet<>();
    private static final Set<Runnable> afterStartCallbackSet = new HashSet<>();

    public static void init(CamelliaServerProperties serverProperties, CamelliaTranspondProperties transpondProperties) {
        if (initOk.get()) return;
        synchronized (GlobalRedisProxyEnv.class) {
            if (initOk.get()) return;
            GlobalRedisProxyEnv.serverProperties = serverProperties;
            GlobalRedisProxyEnv.transpondProperties = transpondProperties;
            GlobalRedisProxyEnv.nettyTransportMode = serverProperties.getNettyTransportMode();
            GlobalRedisProxyEnv.cportPassword = serverProperties.getCportPassword();
            if (GlobalRedisProxyEnv.nettyTransportMode == NettyTransportMode.auto) {
                if (isEpollAvailable()) {
                    GlobalRedisProxyEnv.nettyTransportMode = NettyTransportMode.epoll;
                } else if (isKQueueAvailable()) {
                    GlobalRedisProxyEnv.nettyTransportMode = NettyTransportMode.kqueue;
                } else if (isIOUringAvailable()) {
                    GlobalRedisProxyEnv.nettyTransportMode = NettyTransportMode.io_uring;
                } else {
                    GlobalRedisProxyEnv.nettyTransportMode = NettyTransportMode.nio;
                }
            }
            if (nettyTransportMode == NettyTransportMode.epoll && isEpollAvailable()) {
                bossThread = serverProperties.getBossThread();
                bossGroup = new MultiThreadIoEventLoopGroup(bossThread, new DefaultThreadFactory(BOSS_GROUP_NAME), EpollIoHandler.newFactory());
                udsBossGroup = bossGroup;
                workThread = serverProperties.getWorkThread();
                workGroup = new MultiThreadIoEventLoopGroup(workThread, new DefaultThreadFactory(WORK_GROUP_NAME), EpollIoHandler.newFactory());
                udsWorkGroup = workGroup;
                serverChannelClass = EpollServerSocketChannel.class;
                serverUdsChannelClass = EpollServerDomainSocketChannel.class;
                serverTcpQuickAck = serverProperties.isTcpQuickAck();
                nettyTransportMode = NettyTransportMode.epoll;
            } else if (nettyTransportMode == NettyTransportMode.kqueue && isKQueueAvailable()) {
                bossThread = serverProperties.getBossThread();
                bossGroup = new MultiThreadIoEventLoopGroup(bossThread, new DefaultThreadFactory(BOSS_GROUP_NAME), KQueueIoHandler.newFactory());
                udsBossGroup = bossGroup;
                workThread = serverProperties.getWorkThread();
                workGroup = new MultiThreadIoEventLoopGroup(workThread, new DefaultThreadFactory(WORK_GROUP_NAME), KQueueIoHandler.newFactory());
                udsWorkGroup = workGroup;
                serverChannelClass = KQueueServerSocketChannel.class;
                serverUdsChannelClass = KQueueServerDomainSocketChannel.class;
                nettyTransportMode = NettyTransportMode.kqueue;
            } else if (nettyTransportMode == NettyTransportMode.io_uring && isIOUringAvailable()) {
                bossThread = serverProperties.getBossThread();
                bossGroup = new MultiThreadIoEventLoopGroup(bossThread, new DefaultThreadFactory(BOSS_GROUP_NAME), IoUringIoHandler.newFactory());
                workThread = serverProperties.getWorkThread();
                workGroup = new MultiThreadIoEventLoopGroup(workThread, new DefaultThreadFactory(WORK_GROUP_NAME), IoUringIoHandler.newFactory());
                serverChannelClass = IoUringServerSocketChannel.class;
                nettyTransportMode = NettyTransportMode.io_uring;
            } else {
                bossThread = serverProperties.getBossThread();
                bossGroup = new MultiThreadIoEventLoopGroup(bossThread, new DefaultThreadFactory(BOSS_GROUP_NAME), NioIoHandler.newFactory());
                workThread = serverProperties.getWorkThread();
                workGroup = new MultiThreadIoEventLoopGroup(workThread, new DefaultThreadFactory(WORK_GROUP_NAME), NioIoHandler.newFactory());
                serverChannelClass = NioServerSocketChannel.class;
                nettyTransportMode = NettyTransportMode.nio;
            }
            if (udsBossGroup == null || udsWorkGroup == null) {
                if (isEpollAvailable()) {
                    udsBossGroup = new MultiThreadIoEventLoopGroup(bossThread, new DefaultThreadFactory(UDS_BOSS_GROUP_NAME), EpollIoHandler.newFactory());
                    udsWorkGroup = new MultiThreadIoEventLoopGroup(workThread, new DefaultThreadFactory(UDS_WORK_GROUP_NAME), EpollIoHandler.newFactory());
                    serverUdsChannelClass = EpollServerDomainSocketChannel.class;
                } else if (isKQueueAvailable()) {
                    udsBossGroup = new MultiThreadIoEventLoopGroup(bossThread, new DefaultThreadFactory(UDS_BOSS_GROUP_NAME), KQueueIoHandler.newFactory());
                    udsWorkGroup = new MultiThreadIoEventLoopGroup(workThread, new DefaultThreadFactory(UDS_WORK_GROUP_NAME), KQueueIoHandler.newFactory());
                    serverUdsChannelClass = KQueueServerDomainSocketChannel.class;
                }
            }
            queueFactory = (QueueFactory) serverProperties.getProxyBeanFactory()
                    .getBean(BeanInitUtils.parseClass(serverProperties.getQueueFactoryClassName()));
            proxyBeanFactory = serverProperties.getProxyBeanFactory();
            initOk.set(true);
        }
    }

    public static void setPort(int port) {
        GlobalRedisProxyEnv.port = port;
    }

    public static void setTlsPort(int tlsPort) {
        GlobalRedisProxyEnv.tlsPort = tlsPort;
    }

    public static void setCport(int cport) {
        GlobalRedisProxyEnv.cport = cport;
    }

    public static void setHttpPort(int httpPort) {
        GlobalRedisProxyEnv.httpPort = httpPort;
    }

    public static void setUdsPath(String udsPath) {
        GlobalRedisProxyEnv.udsPath = udsPath;
    }

    public static void setConsolePort(int consolePort) {
        GlobalRedisProxyEnv.consolePort = consolePort;
    }

    public static void setClientTemplateFactory(IUpstreamClientTemplateFactory clientTemplateFactory) {
        GlobalRedisProxyEnv.clientTemplateFactory = clientTemplateFactory;
        GlobalRedisProxyEnv.getProxyShutdown().updateUpstreamClientTemplateFactory(clientTemplateFactory);
    }

    public static void setRedisProxyEnv(RedisProxyEnv redisProxyEnv) {
        GlobalRedisProxyEnv.redisProxyEnv = redisProxyEnv;
    }

    public static void setDiscoveryFactory(ProxyDiscoveryFactory discoveryFactory) {
        GlobalRedisProxyEnv.discoveryFactory = discoveryFactory;
    }

    public static void setClusterModeEnable(boolean clusterModeEnable) {
        GlobalRedisProxyEnv.clusterModeEnable = clusterModeEnable;
    }

    public static void setSentinelModeEnable(boolean sentinelModeEnable) {
        GlobalRedisProxyEnv.sentinelModeEnable = sentinelModeEnable;
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

    public static int getBossThread() {
        return bossThread;
    }

    public static EventLoopGroup getBossGroup() {
        return bossGroup;
    }

    public static int getWorkThread() {
        return workThread;
    }

    public static EventLoopGroup getWorkGroup() {
        return workGroup;
    }

    public static EventLoopGroup getUdsBossGroup() {
        return udsBossGroup;
    }

    public static EventLoopGroup getUdsWorkGroup() {
        return udsWorkGroup;
    }

    public static Class<? extends ServerChannel> getServerChannelClass() {
        return serverChannelClass;
    }

    public static Class<? extends ServerChannel> getServerUdsChannelClass() {
        return serverUdsChannelClass;
    }

    public static int getPort() {
        return port;
    }

    public static int getTlsPort() {
        return tlsPort;
    }

    public static int getCport() {
        return cport;
    }

    public static String getCportPassword() {
        return cportPassword;
    }

    public static int getHttpPort() {
        return httpPort;
    }

    public static String getUdsPath() {
        return udsPath;
    }

    public static int getConsolePort() {
        return consolePort;
    }

    public static IUpstreamClientTemplateFactory getClientTemplateFactory() {
        return clientTemplateFactory;
    }

    public static ProxyDiscoveryFactory getDiscoveryFactory() {
        return discoveryFactory;
    }

    public static NettyTransportMode getNettyTransportMode() {
        return nettyTransportMode;
    }

    public static RedisProxyEnv getRedisProxyEnv() {
        return redisProxyEnv;
    }

    public static boolean isServerTcpQuickAckEnable() {
        return nettyTransportMode == NettyTransportMode.epoll && serverTcpQuickAck;
    }

    public static boolean isClusterModeEnable() {
        return clusterModeEnable;
    }

    public static boolean isSentinelModeEnable() {
        return sentinelModeEnable;
    }

    public static String proxyMode() {
        if (clusterModeEnable) {
            return "cluster";
        } else if (sentinelModeEnable) {
            return "sentinel";
        } else {
            return "standalone";
        }
    }

    public static boolean isEpollAvailable() {
        try {
            boolean available = Epoll.isAvailable();
            logger.info("epoll available = {}", available);
            return available;
        } catch (Throwable e) {
            logger.warn("epoll is unavailable, e = {}", e.toString());
            return false;
        }
    }

    public static boolean isIOUringAvailable() {
        try {
            boolean available = IoUring.isAvailable();
            logger.info("io_uring available = {}", available);
            return available;
        } catch (Throwable e) {
            logger.warn("io_uring is unavailable, e = {}", e.toString());
            return false;
        }
    }

    public static boolean isKQueueAvailable() {
        try {
            boolean available = KQueue.isAvailable();
            logger.info("kqueue available = {}", available);
            return available;
        } catch (Throwable e) {
            logger.warn("kqueue is unavailable, e = {}", e.toString());
            return false;
        }
    }

    public static QueueFactory getQueueFactory() {
        return queueFactory;
    }

    public static DefaultProxyShutdown getProxyShutdown() {
        return proxyShutdown;
    }

    public static ProxyBeanFactory getProxyBeanFactory() {
        return proxyBeanFactory;
    }

    public static CamelliaServerProperties getServerProperties() {
        return serverProperties;
    }

    public static CamelliaTranspondProperties getTranspondProperties() {
        return transpondProperties;
    }
}
