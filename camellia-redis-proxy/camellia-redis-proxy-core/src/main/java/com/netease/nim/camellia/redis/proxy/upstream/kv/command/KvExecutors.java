package com.netease.nim.camellia.redis.proxy.upstream.kv.command;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.netty.NettyTransportMode;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.util.MpscHashedExecutor;
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
public class KvExecutors {

    private static final Logger logger = LoggerFactory.getLogger(KvExecutors.class);

    private static volatile KvExecutors INSTANCE;

    private final MpscHashedExecutor commandExecutor;
    private final MpscHashedExecutor asyncWriteExecutor;

    private KvExecutors() {
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

        int threads1 = ProxyDynamicConf.getInt("kv.command.executor.threads", SysUtils.getCpuNum() * 4);
        int queueSize1 = ProxyDynamicConf.getInt("kv.command.executor.queue.size", 100000);
        commandExecutor = new MpscHashedExecutor("kv-command-executor", threads1, queueSize1, new MpscHashedExecutor.AbortPolicy(), workThreadInitCallback);
        logger.info("KvCommandExecutor init success, nettyWorkThreads = {}, threads = {}, queueSize = {}", nettyWorkThreads, threads1, queueSize1);

        int threads2 = ProxyDynamicConf.getInt("kv.async.write.executor.threads", SysUtils.getCpuNum() * 4);
        int queueSize2 = ProxyDynamicConf.getInt("kv.async.write.executor.queue.size", 1000000);
        asyncWriteExecutor = new MpscHashedExecutor("kv-async-write-executor", threads2, queueSize2, new MpscHashedExecutor.AbortPolicy());
        logger.info("KvAsyncWriteExecutor init success, threads = {}, queueSize = {}", threads2, queueSize2);
    }

    public static KvExecutors getInstance() {
        if (INSTANCE == null) {
            synchronized (KvExecutors.class) {
                if (INSTANCE == null) {
                    INSTANCE = new KvExecutors();
                }
            }
        }
        return INSTANCE;
    }

    public MpscHashedExecutor getCommandExecutor() {
        return commandExecutor;
    }

    public MpscHashedExecutor getAsyncWriteExecutor() {
        return asyncWriteExecutor;
    }
}
