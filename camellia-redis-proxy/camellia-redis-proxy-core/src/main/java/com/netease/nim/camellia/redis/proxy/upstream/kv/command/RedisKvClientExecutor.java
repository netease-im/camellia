package com.netease.nim.camellia.redis.proxy.upstream.kv.command;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.netty.NettyTransportMode;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.tools.executor.CamelliaHashedExecutor;
import com.netease.nim.camellia.tools.utils.SysUtils;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2024/4/17
 */
public class RedisKvClientExecutor {

    private static final Logger logger = LoggerFactory.getLogger(RedisKvClientExecutor.class);

    private static volatile RedisKvClientExecutor INSTANCE;

    private final CamelliaHashedExecutor executor;

    private RedisKvClientExecutor() {
        EventLoopGroup eventLoopGroup;
        int nettyWorkThreads = ProxyDynamicConf.getInt("kv.redis.netty.work.threads", SysUtils.getCpuNum() * 2);
        NettyTransportMode nettyTransportMode = GlobalRedisProxyEnv.getNettyTransportMode();
        if (nettyTransportMode == NettyTransportMode.epoll) {
            eventLoopGroup = new EpollEventLoopGroup(nettyWorkThreads, new DefaultThreadFactory("camellia-kv-redis-connection"));
        } else if (nettyTransportMode == NettyTransportMode.kqueue) {
            eventLoopGroup = new KQueueEventLoopGroup(nettyWorkThreads, new DefaultThreadFactory("camellia-kv-redis-connection"));
        } else if (nettyTransportMode == NettyTransportMode.io_uring) {
            eventLoopGroup = new IOUringEventLoopGroup(nettyWorkThreads, new DefaultThreadFactory("camellia-kv-redis-connection"));
        } else {
            eventLoopGroup = new NioEventLoopGroup(nettyWorkThreads, new DefaultThreadFactory("camellia-kv-redis-connection"));
        }

        Runnable workThreadInitCallback = () -> RedisConnectionHub.getInstance().updateEventLoop(eventLoopGroup.next());

        int threads = ProxyDynamicConf.getInt("kv.command.executor.threads", SysUtils.getCpuNum() * 4);
        int queueSize = ProxyDynamicConf.getInt("kv.command.executor.queue.size", 100000);
        executor = new CamelliaHashedExecutor("kv-command-executor", threads, queueSize, new CamelliaHashedExecutor.AbortPolicy(), workThreadInitCallback);
        logger.info("RedisKvClientExecutor init success, nettyWorkThreads = {}, threads = {}, queueSize = {}", nettyWorkThreads, threads, queueSize);
    }

    public static RedisKvClientExecutor getInstance() {
        if (INSTANCE == null) {
            synchronized (RedisKvClientExecutor.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RedisKvClientExecutor();
                }
            }
        }
        return INSTANCE;
    }

    public CamelliaHashedExecutor getExecutor() {
        return executor;
    }
}
