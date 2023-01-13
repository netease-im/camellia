package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.ProxyDiscoveryFactory;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.upstream.AsyncCamelliaRedisTemplateChooser;
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

    private static final AtomicBoolean initOk = new AtomicBoolean(false);

    private static NettyIOMode nettyIOMode = NettyIOMode.nio;

    private static int bossThread;
    private static EventLoopGroup bossGroup;
    private static int workThread;
    private static EventLoopGroup workGroup;
    private static Class<? extends ServerChannel> serverChannelClass;
    private static Class<? extends SocketChannel> socketChannelClass;

    private static boolean serverTcpQuickAck = false;

    private static int port;
    private static int cport;
    private static int consolePort;

    private static AsyncCamelliaRedisTemplateChooser chooser;
    private static ProxyDiscoveryFactory discoveryFactory;

    private static final Set<Runnable> callbackSet = new HashSet<>();

    public static void init(CamelliaServerProperties serverProperties) {
        if (initOk.get()) return;
        synchronized (GlobalRedisProxyEnv.class) {
            if (initOk.get()) return;
            GlobalRedisProxyEnv.nettyIOMode = serverProperties.getNettyIOMode();
            if (nettyIOMode == NettyIOMode.epoll && isEpollAvailable()) {
                bossThread = serverProperties.getBossThread();
                bossGroup = new EpollEventLoopGroup(bossThread, new DefaultThreadFactory("camellia-boss-group"));
                workThread = serverProperties.getWorkThread();
                workGroup = new EpollEventLoopGroup(workThread, new DefaultThreadFactory("camellia-work-group"));
                serverChannelClass = EpollServerSocketChannel.class;
                socketChannelClass = EpollSocketChannel.class;
                serverTcpQuickAck = serverProperties.isTcpQuickAck();
                nettyIOMode = NettyIOMode.epoll;
            } else if (nettyIOMode == NettyIOMode.kqueue && isKQueueAvailable()) {
                bossThread = serverProperties.getBossThread();
                bossGroup = new KQueueEventLoopGroup(bossThread, new DefaultThreadFactory("camellia-boss-group"));
                workThread = serverProperties.getWorkThread();
                workGroup = new KQueueEventLoopGroup(workThread, new DefaultThreadFactory("camellia-work-group"));
                serverChannelClass = KQueueServerSocketChannel.class;
                socketChannelClass = KQueueSocketChannel.class;
                nettyIOMode = NettyIOMode.kqueue;
            } else if (nettyIOMode == NettyIOMode.io_uring && isIOUringAvailable()) {
                bossThread = serverProperties.getBossThread();
                bossGroup = new IOUringEventLoopGroup(bossThread, new DefaultThreadFactory("camellia-boss-group"));
                workThread = serverProperties.getWorkThread();
                workGroup = new IOUringEventLoopGroup(workThread, new DefaultThreadFactory("camellia-work-group"));
                serverChannelClass = IOUringServerSocketChannel.class;
                socketChannelClass = IOUringSocketChannel.class;
                nettyIOMode = NettyIOMode.io_uring;
            } else {
                bossThread = serverProperties.getBossThread();
                bossGroup = new NioEventLoopGroup(bossThread, new DefaultThreadFactory("camellia-boss-group"));
                workThread = serverProperties.getWorkThread();
                workGroup = new NioEventLoopGroup(workThread, new DefaultThreadFactory("camellia-work-group"));
                serverChannelClass = NioServerSocketChannel.class;
                socketChannelClass = NioSocketChannel.class;
                nettyIOMode = NettyIOMode.nio;
            }
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

    public static void setChooser(AsyncCamelliaRedisTemplateChooser chooser) {
        GlobalRedisProxyEnv.chooser = chooser;
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

    public static AsyncCamelliaRedisTemplateChooser getChooser() {
        return chooser;
    }

    public static ProxyDiscoveryFactory getDiscoveryFactory() {
        return discoveryFactory;
    }

    public static NettyIOMode getNettyIOMode() {
        return nettyIOMode;
    }

    public static boolean isServerTcpQuickAckEnable() {
        return nettyIOMode == NettyIOMode.epoll && serverTcpQuickAck;
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
}
