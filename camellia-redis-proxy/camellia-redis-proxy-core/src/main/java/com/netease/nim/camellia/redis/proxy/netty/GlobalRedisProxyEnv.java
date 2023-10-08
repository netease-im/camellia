package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.base.proxy.ProxyDiscoveryFactory;
import com.netease.nim.camellia.redis.proxy.command.DefaultQueueFactory;
import com.netease.nim.camellia.redis.proxy.command.QueueFactory;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.incubator.channel.uring.IOUring;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringServerSocketChannel;
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

    private static final String BOSS_GROUP_NAME = "camellia-boss-group";
    private static final String WORK_GROUP_NAME = "camellia-work-group";
    private static final String UDS_BOSS_GROUP_NAME = "camellia-uds-boss-group";
    private static final String UDS_WORK_GROUP_NAME = "camellia-uds-work-group";

    private static final AtomicBoolean initOk = new AtomicBoolean(false);

    private static NettyTransportMode nettyTransportMode = NettyTransportMode.nio;

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

    private static IUpstreamClientTemplateFactory clientTemplateFactory;
    private static ProxyDiscoveryFactory discoveryFactory;

    private static QueueFactory queueFactory = new DefaultQueueFactory();

    private static final DefaultProxyShutdown proxyShutdown = new DefaultProxyShutdown();

    private static final Set<Runnable> callbackSet = new HashSet<>();

    public static void init(CamelliaServerProperties serverProperties) {
        if (initOk.get()) return;
        synchronized (GlobalRedisProxyEnv.class) {
            if (initOk.get()) return;
            GlobalRedisProxyEnv.nettyTransportMode = serverProperties.getNettyTransportMode();
            if (nettyTransportMode == NettyTransportMode.epoll && isEpollAvailable()) {
                bossThread = serverProperties.getBossThread();
                bossGroup = new EpollEventLoopGroup(bossThread, new DefaultThreadFactory(BOSS_GROUP_NAME));
                udsBossGroup = bossGroup;
                workThread = serverProperties.getWorkThread();
                workGroup = new EpollEventLoopGroup(workThread, new DefaultThreadFactory(WORK_GROUP_NAME));
                udsWorkGroup = workGroup;
                serverChannelClass = EpollServerSocketChannel.class;
                serverUdsChannelClass = EpollServerDomainSocketChannel.class;
                serverTcpQuickAck = serverProperties.isTcpQuickAck();
                nettyTransportMode = NettyTransportMode.epoll;
            } else if (nettyTransportMode == NettyTransportMode.kqueue && isKQueueAvailable()) {
                bossThread = serverProperties.getBossThread();
                bossGroup = new KQueueEventLoopGroup(bossThread, new DefaultThreadFactory(BOSS_GROUP_NAME));
                udsBossGroup = bossGroup;
                workThread = serverProperties.getWorkThread();
                workGroup = new KQueueEventLoopGroup(workThread, new DefaultThreadFactory(WORK_GROUP_NAME));
                udsWorkGroup = workGroup;
                serverChannelClass = KQueueServerSocketChannel.class;
                serverUdsChannelClass = KQueueServerDomainSocketChannel.class;
                nettyTransportMode = NettyTransportMode.kqueue;
            } else if (nettyTransportMode == NettyTransportMode.io_uring && isIOUringAvailable()) {
                bossThread = serverProperties.getBossThread();
                bossGroup = new IOUringEventLoopGroup(bossThread, new DefaultThreadFactory(BOSS_GROUP_NAME));
                workThread = serverProperties.getWorkThread();
                workGroup = new IOUringEventLoopGroup(workThread, new DefaultThreadFactory(WORK_GROUP_NAME));
                serverChannelClass = IOUringServerSocketChannel.class;
                nettyTransportMode = NettyTransportMode.io_uring;
            } else {
                bossThread = serverProperties.getBossThread();
                bossGroup = new NioEventLoopGroup(bossThread, new DefaultThreadFactory(BOSS_GROUP_NAME));
                workThread = serverProperties.getWorkThread();
                workGroup = new NioEventLoopGroup(workThread, new DefaultThreadFactory(WORK_GROUP_NAME));
                serverChannelClass = NioServerSocketChannel.class;
                nettyTransportMode = NettyTransportMode.nio;
            }
            if (udsBossGroup == null || udsWorkGroup == null) {
                if (isEpollAvailable()) {
                    udsBossGroup = new EpollEventLoopGroup(bossThread, new DefaultThreadFactory(UDS_BOSS_GROUP_NAME));
                    udsWorkGroup = new EpollEventLoopGroup(workThread, new DefaultThreadFactory(UDS_WORK_GROUP_NAME));
                    serverUdsChannelClass = EpollServerDomainSocketChannel.class;
                } else if (isKQueueAvailable()) {
                    udsBossGroup = new KQueueEventLoopGroup(bossThread, new DefaultThreadFactory(UDS_BOSS_GROUP_NAME));
                    udsWorkGroup = new KQueueEventLoopGroup(workThread, new DefaultThreadFactory(UDS_WORK_GROUP_NAME));
                    serverUdsChannelClass = KQueueServerDomainSocketChannel.class;
                }
            }
            queueFactory = (QueueFactory) serverProperties.getProxyBeanFactory()
                    .getBean(BeanInitUtils.parseClass(serverProperties.getQueueFactoryClassName()));
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

    public static void setDiscoveryFactory(ProxyDiscoveryFactory discoveryFactory) {
        GlobalRedisProxyEnv.discoveryFactory = discoveryFactory;
    }

    public static synchronized void addStartOkCallback(Runnable callback) {
        callbackSet.add(callback);
    }

    public static void invokeStartOkCallback() {
        for (Runnable runnable : callbackSet) {
            try {
                runnable.run();
            } catch (Exception e) {
                logger.error("callback error", e);
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

    public static boolean isServerTcpQuickAckEnable() {
        return nettyTransportMode == NettyTransportMode.epoll && serverTcpQuickAck;
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
            boolean available = IOUring.isAvailable();
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
}
