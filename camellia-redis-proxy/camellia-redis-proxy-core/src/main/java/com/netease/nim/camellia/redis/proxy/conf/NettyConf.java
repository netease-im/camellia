package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.redis.proxy.netty.NettyTransportMode;
import com.netease.nim.camellia.tools.utils.SysUtils;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerDomainSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerDomainSocketChannel;
import io.netty.channel.uring.IoUringServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by caojiajun on 2026/2/9
 */
public class NettyConf {

    private static final Logger logger = LoggerFactory.getLogger(NettyConf.class);

    public static enum Type {
        tcp_server(true),
        uds_server(true),
        http_server(true),
        tcp_upstream(false),
        uds_upstream(false),
        ;
        private final boolean server;

        Type(boolean server) {
            this.server = server;
        }
        public boolean isServer() {
            return server;
        }
    }

    public static NettyTransportMode transportMode(Type type) {
        String mode = null;
        if (type != null) {
            mode = ProxyDynamicConf.getString(type + ".netty.transport.mode", null);
        }
        if (mode == null) {
            mode = ProxyDynamicConf.getString("netty.transport.mode", NettyTransportMode.auto.name());
        }
        NettyTransportMode transportMode;
        if (mode.equalsIgnoreCase(NettyTransportMode.auto.name())) {
            transportMode = NettyTransportMode.auto;
        } else if (mode.equalsIgnoreCase(NettyTransportMode.epoll.name())) {
            transportMode = NettyTransportMode.epoll;
        } else if (mode.equalsIgnoreCase(NettyTransportMode.kqueue.name())) {
            transportMode = NettyTransportMode.kqueue;
        } else if (mode.equalsIgnoreCase(NettyTransportMode.io_uring.name())) {
            transportMode = NettyTransportMode.io_uring;
        } else if (mode.equalsIgnoreCase(NettyTransportMode.nio.name())) {
            transportMode = NettyTransportMode.nio;
        } else {
            transportMode = NettyTransportMode.nio;
        }
        if (transportMode == NettyTransportMode.auto) {
            if (Epoll.isAvailable()) {
                transportMode = NettyTransportMode.epoll;
            } else if (KQueue.isAvailable()) {
                transportMode = NettyTransportMode.kqueue;
            } else if (IoUring.isAvailable()) {
                transportMode = NettyTransportMode.io_uring;
            } else {
                transportMode = NettyTransportMode.nio;
            }
        }
        if (transportMode == NettyTransportMode.epoll && !Epoll.isAvailable()) {
            transportMode = NettyTransportMode.nio;
        }
        if (transportMode == NettyTransportMode.kqueue && !KQueue.isAvailable()) {
            transportMode = NettyTransportMode.nio;
        }
        if (transportMode == NettyTransportMode.io_uring && !IoUring.isAvailable()) {
            transportMode = NettyTransportMode.nio;
        }
        logger.info("type = {}, config netty transport mode = {}, final netty transport mode = {}", type, mode, transportMode);
        return transportMode;
    }

    public static EventLoopGroupResult serverEventLoopGroup(Type type) {
        if (!type.isServer()) {
            throw new IllegalArgumentException("not server type");
        }
        NettyTransportMode transportMode = transportMode(type);

        EventLoopGroup bossGroup;
        EventLoopGroup workGroup;
        Class<? extends ServerChannel> serverChannelClass = null;

        String bossName = "camellia-" + type.name() + "-boss";
        String workName = "camellia-" + type.name() + "-work";

        int bossThread = bossThread(type);
        int workThread = workThread(type);

        if (transportMode == NettyTransportMode.epoll) {
            bossGroup = new MultiThreadIoEventLoopGroup(bossThread, new DefaultThreadFactory(bossName), EpollIoHandler.newFactory());
            workGroup = new MultiThreadIoEventLoopGroup(workThread, new DefaultThreadFactory(workName), EpollIoHandler.newFactory());
            if (type == Type.tcp_server) {
                serverChannelClass = EpollServerSocketChannel.class;
            } else if (type == Type.uds_server) {
                serverChannelClass = EpollServerDomainSocketChannel.class;
            } else if (type == Type.http_server) {
                serverChannelClass = EpollServerSocketChannel.class;
            }
        } else if (transportMode == NettyTransportMode.kqueue) {
            bossGroup = new MultiThreadIoEventLoopGroup(bossThread, new DefaultThreadFactory(bossName), KQueueIoHandler.newFactory());
            workGroup = new MultiThreadIoEventLoopGroup(workThread, new DefaultThreadFactory(workName), KQueueIoHandler.newFactory());
            if (type == Type.tcp_server) {
                serverChannelClass = KQueueServerSocketChannel.class;
            } else if (type == Type.uds_server) {
                serverChannelClass = KQueueServerDomainSocketChannel.class;
            } else if (type == Type.http_server) {
                serverChannelClass = KQueueServerSocketChannel.class;
            }
        } else if (transportMode == NettyTransportMode.io_uring) {
            bossGroup = new MultiThreadIoEventLoopGroup(bossThread, new DefaultThreadFactory(bossName), IoUringIoHandler.newFactory());
            workGroup = new MultiThreadIoEventLoopGroup(workThread, new DefaultThreadFactory(workName), IoUringIoHandler.newFactory());
            if (type == Type.tcp_server) {
                serverChannelClass = IoUringServerSocketChannel.class;
            } else if (type == Type.uds_server) {
                serverChannelClass = IoUringServerDomainSocketChannel.class;
            } else if (type == Type.http_server) {
                serverChannelClass = IoUringServerSocketChannel.class;
            }
        } else {
            bossGroup = new MultiThreadIoEventLoopGroup(bossThread, new DefaultThreadFactory(bossName), NioIoHandler.newFactory());
            workGroup = new MultiThreadIoEventLoopGroup(workThread, new DefaultThreadFactory(workName), NioIoHandler.newFactory());
            if (type == Type.tcp_server) {
                serverChannelClass = NioServerSocketChannel.class;
            } else if (type == Type.uds_server) {
                serverChannelClass = NioServerDomainSocketChannel.class;
            } else if (type == Type.http_server) {
                serverChannelClass = NioServerSocketChannel.class;
            }
        }
        return new EventLoopGroupResult(bossGroup, workGroup, bossThread, workThread, serverChannelClass);
    }

    public static EventLoopGroup upstreamEventLoopGroup(Type type) {
        if (type.isServer()) {
            throw new IllegalArgumentException("not upstream type");
        }
        NettyTransportMode transportMode = transportMode(type);

        String workName = "camellia-" + type.name() + "-work";

        EventLoopGroup eventLoopGroup;
        if (transportMode == NettyTransportMode.epoll) {
            eventLoopGroup = new MultiThreadIoEventLoopGroup(upstreamWorkThread(type), new DefaultThreadFactory(workName), EpollIoHandler.newFactory());
        } else if (transportMode == NettyTransportMode.kqueue) {
            eventLoopGroup = new MultiThreadIoEventLoopGroup(upstreamWorkThread(type), new DefaultThreadFactory(workName), KQueueIoHandler.newFactory());
        } else if (transportMode == NettyTransportMode.io_uring) {
            eventLoopGroup = new MultiThreadIoEventLoopGroup(upstreamWorkThread(type), new DefaultThreadFactory(workName), IoUringIoHandler.newFactory());
        } else {
            eventLoopGroup = new MultiThreadIoEventLoopGroup(upstreamWorkThread(type), new DefaultThreadFactory(workName), NioIoHandler.newFactory());
        }
        return eventLoopGroup;
    }

    public static EventLoopGroup upstreamKvEventLoopGroup(int workThread) {
        NettyTransportMode transportMode = transportMode(null);

        String workName = "camellia-kv-work";

        EventLoopGroup eventLoopGroup;
        if (transportMode == NettyTransportMode.epoll) {
            eventLoopGroup = new MultiThreadIoEventLoopGroup(workThread, new DefaultThreadFactory(workName), EpollIoHandler.newFactory());
        } else if (transportMode == NettyTransportMode.kqueue) {
            eventLoopGroup = new MultiThreadIoEventLoopGroup(workThread, new DefaultThreadFactory(workName), KQueueIoHandler.newFactory());
        } else if (transportMode == NettyTransportMode.io_uring) {
            eventLoopGroup = new MultiThreadIoEventLoopGroup(workThread, new DefaultThreadFactory(workName), IoUringIoHandler.newFactory());
        } else {
            eventLoopGroup = new MultiThreadIoEventLoopGroup(workThread, new DefaultThreadFactory(workName), NioIoHandler.newFactory());
        }
        return eventLoopGroup;
    }

    private static int bossThread(Type type) {
        return getInt(type, "boss.thread", 1);
    }

    private static int workThread(Type type) {
        int workThread = getInt(type, "work.thread", -1);
        if (workThread <= 0) {
            workThread = SysUtils.getCpuNum();
        }
        return workThread;
    }

    private static int upstreamWorkThread(Type type) {
        int workThread = getInt(type, "upstream.work.thread", -1);
        if (workThread <= 0) {
            workThread = SysUtils.getCpuHalfNum();
        }
        return workThread;
    }

    public static boolean tcpNoDelay(Type type) {
        return getBoolean(type, "tcp.no.delay", true);
    }

    public static int soBacklog(Type type) {
        return getInt(type, "so.backlog", 1024);
    }

    public static int soSndbuf(Type type) {
        return getInt(type, "so.sndbuf", 6 * 1024 * 1024);
    }

    public static int soRcvbuf(Type type) {
        return getInt(type, "so.rcvbuf", 6 * 1024 * 1024);
    }

    public static boolean soKeepalive(Type type) {
        return getBoolean(type, "so.keepalive", true);
    }

    public static int readerIdleTimeSeconds() {
        return ProxyDynamicConf.getInt("reader.idle.time.seconds", -1);
    }

    public static int writerIdleTimeSeconds() {
        return ProxyDynamicConf.getInt("writer.idle.time.seconds", -1);
    }

    public static int allIdleTimeSeconds() {
        return ProxyDynamicConf.getInt("all.idle.time.seconds", -1);
    }

    public static int writeBufferWaterMarkLow(Type type) {
        return getInt(type, "write.buffer.water.mark.low", 128 * 1024);
    }

    public static int writeBufferWaterMarkHigh(Type type) {
        return getInt(type, "write.buffer.water.mark.high", 512 * 1024);
    }

    private static Boolean getBoolean(Type type, String configKey, boolean defaultValue) {
        String value = ProxyDynamicConf.getString(type + "." + configKey, null);
        if (value == null) {
            return ProxyDynamicConf.getBoolean(configKey, defaultValue);
        }
        return Boolean.parseBoolean(value);
    }

    private static int getInt(Type type, String configKey, int defaultValue) {
        String value = ProxyDynamicConf.getString(type + "." + configKey, null);
        if (value == null) {
            return ProxyDynamicConf.getInt(configKey, defaultValue);
        }
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return ProxyDynamicConf.getInt(configKey, defaultValue);
        }
    }

}
