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
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.incubator.channel.uring.IOUring;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringServerSocketChannel;
import io.netty.incubator.channel.uring.IOUringSocketChannel;
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

    private static final AtomicBoolean initOk = new AtomicBoolean(false);

    private static NettyTransportMode nettyTransportMode = NettyTransportMode.nio;

    private static int bossThread;
    private static EventLoopGroup bossGroup;
    private static int workThread;
    private static EventLoopGroup workGroup;
    private static Class<? extends ServerChannel> serverChannelClass = NioServerSocketChannel.class;
    private static Class<? extends SocketChannel> socketChannelClass = NioSocketChannel.class;

    private static boolean serverTcpQuickAck = false;

    private static int port;
    private static int cport;
    private static int consolePort;

    private static IUpstreamClientTemplateFactory clientTemplateFactory;
    private static ProxyDiscoveryFactory discoveryFactory;

    private static QueueFactory queueFactory = new DefaultQueueFactory();

    private static ProxyShutdown proxyShutdown = new ProxyShutdown();

    private static final Set<Runnable> callbackSet = new HashSet<>();

    public static void init(CamelliaServerProperties serverProperties) {
        if (initOk.get()) return;
        synchronized (GlobalRedisProxyEnv.class) {
            if (initOk.get()) return;
            GlobalRedisProxyEnv.nettyTransportMode = serverProperties.getNettyTransportMode();
            if (nettyTransportMode == NettyTransportMode.epoll && isEpollAvailable()) {
                bossThread = serverProperties.getBossThread();
                bossGroup = new EpollEventLoopGroup(bossThread, new DefaultThreadFactory(BOSS_GROUP_NAME));
                workThread = serverProperties.getWorkThread();
                workGroup = new EpollEventLoopGroup(workThread, new DefaultThreadFactory(WORK_GROUP_NAME));
                serverChannelClass = EpollServerSocketChannel.class;
                socketChannelClass = EpollSocketChannel.class;
                serverTcpQuickAck = serverProperties.isTcpQuickAck();
                nettyTransportMode = NettyTransportMode.epoll;
            } else if (nettyTransportMode == NettyTransportMode.kqueue && isKQueueAvailable()) {
                bossThread = serverProperties.getBossThread();
                bossGroup = new KQueueEventLoopGroup(bossThread, new DefaultThreadFactory(BOSS_GROUP_NAME));
                workThread = serverProperties.getWorkThread();
                workGroup = new KQueueEventLoopGroup(workThread, new DefaultThreadFactory(WORK_GROUP_NAME));
                serverChannelClass = KQueueServerSocketChannel.class;
                socketChannelClass = KQueueSocketChannel.class;
                nettyTransportMode = NettyTransportMode.kqueue;
            } else if (nettyTransportMode == NettyTransportMode.io_uring && isIOUringAvailable()) {
                bossThread = serverProperties.getBossThread();
                bossGroup = new IOUringEventLoopGroup(bossThread, new DefaultThreadFactory(BOSS_GROUP_NAME));
                workThread = serverProperties.getWorkThread();
                workGroup = new IOUringEventLoopGroup(workThread, new DefaultThreadFactory(WORK_GROUP_NAME));
                serverChannelClass = IOUringServerSocketChannel.class;
                socketChannelClass = IOUringSocketChannel.class;
                nettyTransportMode = NettyTransportMode.io_uring;
            } else {
                bossThread = serverProperties.getBossThread();
                bossGroup = new NioEventLoopGroup(bossThread, new DefaultThreadFactory(BOSS_GROUP_NAME));
                workThread = serverProperties.getWorkThread();
                workGroup = new NioEventLoopGroup(workThread, new DefaultThreadFactory(WORK_GROUP_NAME));
                serverChannelClass = NioServerSocketChannel.class;
                socketChannelClass = NioSocketChannel.class;
                nettyTransportMode = NettyTransportMode.nio;
            }
            queueFactory = (QueueFactory) serverProperties.getProxyBeanFactory()
                    .getBean(BeanInitUtils.parseClass(serverProperties.getQueueFactoryClassName()));
            initOk.set(true);
        }
    }

    public static void setPort(int port) {
        GlobalRedisProxyEnv.port = port;
    }

    public static void setCport(int cport) {
        GlobalRedisProxyEnv.cport = cport;
    }

    public static void setConsolePort(int consolePort) {
        GlobalRedisProxyEnv.consolePort = consolePort;
    }

    public static void setClientTemplateFactory(IUpstreamClientTemplateFactory clientTemplateFactory) {
        GlobalRedisProxyEnv.clientTemplateFactory = clientTemplateFactory;
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

    public static Class<? extends ServerChannel> getServerChannelClass() {
        return serverChannelClass;
    }

    public static Class<? extends SocketChannel> getSocketChannelClass() {
        return socketChannelClass;
    }

    public static int getPort() {
        return port;
    }

    public static int getCport() {
        return cport;
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

    public static ProxyShutdown getProxyShutdown() {
        return proxyShutdown;
    }
}
