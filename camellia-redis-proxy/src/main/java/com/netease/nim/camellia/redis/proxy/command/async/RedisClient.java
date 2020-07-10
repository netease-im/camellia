package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.netty.ClientHandler;
import com.netease.nim.camellia.redis.proxy.netty.ReplyDecoder;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.util.CommandsEncodeUtil;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2019/12/17.
 */
public class RedisClient implements AsyncClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisClient.class);
    private static final AtomicLong id = new AtomicLong(0);

    private static final ScheduledExecutorService scheduled = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("redis-heart-beat"));

    private EventLoopGroup loopGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("redis"));
    private final String host;
    private final int port;
    private final String password;
    private Channel channel;
    private volatile boolean valid = true;
    private ScheduledFuture<?> scheduledFuture;
    private final Queue<CompletableFuture<Reply>> queue = new LinkedBlockingQueue<>(100000);
    private final LinkedBlockingQueue<CommandWrapper> commandQueue = new LinkedBlockingQueue<>(100000);
    private final int heartbeatIntervalSeconds;
    private final long heartbeatTimeoutMillis;
    private final int commandPipelineFlushThreshold;
    private final int connectTimeoutMillis;
    private final String clientName;
    private final Object lock = new Object();

    public RedisClient(String host, int port, String password,
                       int heartbeatIntervalSeconds, long heartbeatTimeoutMillis, int commandPipelineFlushThreshold, int connectTimeoutMillis) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        this.heartbeatTimeoutMillis = heartbeatTimeoutMillis;
        this.commandPipelineFlushThreshold = commandPipelineFlushThreshold;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.clientName = "RedisClient[" + (password == null ? "" : password) + "@" + host + ":" + port + "][id=" + id.incrementAndGet() + "]";
    }

    public void start() {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(loopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_SNDBUF, 1048576)
                    .option(ChannelOption.SO_RCVBUF, 1048576)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                    .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(128 * 1024, 512 * 1024))
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new ReplyDecoder());
                            pipeline.addLast(new ClientHandler(queue, clientName));
                        }
                    });
            logger.info("{} try connect...", clientName);
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            this.channel = channelFuture.channel();
            logger.info("{} connect success", clientName);
            valid = true;
            startFlushThread();
            if (password != null) {
                logger.info("{} need password, try auth", clientName);
                boolean authSuccess = false;
                CompletableFuture<Reply> future = sendCommand(RedisCommand.AUTH.raw(), SafeEncoder.encode(password));
                Reply reply = future.get(connectTimeoutMillis, TimeUnit.MILLISECONDS);
                if (reply instanceof StatusReply) {
                    if (((StatusReply) reply).getStatus().equalsIgnoreCase(StatusReply.OK.getStatus())) {
                        logger.info("{} auth success", clientName);
                        authSuccess = true;
                    }
                }
                if (!authSuccess) {
                    throw new CamelliaRedisException("auth fail");
                }
            }
            this.channel.closeFuture().addListener(future -> {
                logger.error("{} connect close, will stop", clientName);
                stop();
            });
            //建完连接先ping一下，确保连接此时是可用的
            if (!ping(connectTimeoutMillis)) {
                throw new CamelliaRedisException("ping fail");
            }
            if (heartbeatIntervalSeconds > 0 && heartbeatTimeoutMillis > 0) {
                //默认60s发送一个心跳，心跳超时时间10s，如果超时了，则关闭当前连接
                this.scheduledFuture = scheduled.scheduleAtFixedRate(() -> {
                    if (!valid) return;
                    if (!ping(heartbeatTimeoutMillis)) {
                        new Thread(this::stop, "redis-client-stop-" + clientName).start();
                    }
                }, heartbeatIntervalSeconds, heartbeatIntervalSeconds, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            stop();
            logger.error("{} start fail", clientName, e);
        }
    }

    private boolean ping(long timeoutMillis) {
        CompletableFuture<Reply> future = sendCommand(RedisCommand.PING.raw());
        try {
            Reply reply = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            if (reply instanceof StatusReply) {
                if (((StatusReply) reply).getStatus().equalsIgnoreCase(StatusReply.PONG.getStatus())) {
                    return true;
                }
            }
            logger.error("{} ping fail, response = {}", clientName, reply);
            return false;
        } catch (Exception e) {
            logger.error("{} ping timeout, timeoutMillis = {}", clientName, heartbeatTimeoutMillis);
            return false;
        }
    }

    public void stop() {
        String log = clientName + " stopping, command return NOT_AVAILABLE";
        ErrorLogCollector.collect(RedisClient.class, log);
        if (!valid && queue.isEmpty() && commandQueue.isEmpty()
                && channel == null && loopGroup == null && scheduledFuture == null) {
            return;
        }
        synchronized (lock) {
            try {
                valid = false;
                try {
                    if (channel != null) {
                        channel.close();
                        channel = null;
                    }
                } catch (Exception e) {
                    logger.error("{}, channel close error", clientName, e);
                }
                try {
                    if (loopGroup != null) {
                        loopGroup.shutdownGracefully();
                        loopGroup = null;
                    }
                } catch (Exception e) {
                    logger.error("{}, loopGroup shutdownGracefully error", clientName, e);
                }
                try {
                    if (scheduledFuture != null) {
                        scheduledFuture.cancel(false);
                        scheduledFuture = null;
                    }
                } catch (Exception e) {
                    logger.error("{}, heart-beat schedule cancel error", clientName, e);
                }
                while (!queue.isEmpty()) {
                    CompletableFuture<Reply> completableFuture = queue.poll();
                    if (completableFuture != null) {
                        completableFuture.complete(ErrorReply.NOT_AVAILABLE);
                    }
                }
                while (!commandQueue.isEmpty()) {
                    CommandWrapper wrapper = commandQueue.poll();
                    if (wrapper != null) {
                        for (CompletableFuture<Reply> future : wrapper.completableFutureList) {
                            future.complete(ErrorReply.NOT_AVAILABLE);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("{} stop error", clientName, e);
            }
        }
    }

    public boolean isValid() {
        return valid;
    }

    public String getClientName() {
        return clientName;
    }

    public CompletableFuture<Reply> sendCommand(byte[]... args) {
        CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
        sendCommand(Collections.singletonList(new Command(args)), Collections.singletonList(completableFuture));
        return completableFuture;
    }

    public void sendCommand(List<Command> commands, List<CompletableFuture<Reply>> completableFutureList) {
        if (!valid) {
            String log = clientName + " is not valid, command return NOT_AVAILABLE";
            for (CompletableFuture<Reply> future : completableFutureList) {
                future.complete(ErrorReply.NOT_AVAILABLE);
                ErrorLogCollector.collect(RedisClient.class, log);
            }
            return;
        }
        CommandWrapper wrapper = new CommandWrapper();
        wrapper.commands = commands;
        wrapper.completableFutureList = completableFutureList;

        boolean offer = commandQueue.offer(wrapper);
        if (logger.isDebugEnabled()) {
            logger.debug("{} sendCommands to commandQueue, commands.size = {}", clientName, commands.size());
        }
        if (!offer) {
            String log = clientName + ", commandQueue is full, command return NOT_AVAILABLE";
            for (CompletableFuture<Reply> future : completableFutureList) {
                future.complete(ErrorReply.NOT_AVAILABLE);
                ErrorLogCollector.collect(RedisClient.class, log);
            }
        }
    }

    private static class CommandWrapper {
        private List<Command> commands;
        private List<CompletableFuture<Reply>> completableFutureList;
    }
    private void startFlushThread() {
        new Thread(() -> {
            List<CommandWrapper> buffer = new ArrayList<>();
            List<Command> flushCommands = new ArrayList<>(commandPipelineFlushThreshold);
            while (valid) {
                try {
                    if (buffer.isEmpty()) {
                        //第一个CommandWrapper需要阻塞功能
                        CommandWrapper wrapper = commandQueue.poll(10, TimeUnit.SECONDS);
                        if (wrapper == null) {
                            continue;
                        }
                        buffer.add(wrapper);
                        continue;
                    } else {
                        //之后的不需要阻塞
                        //使用drainTo减少循环次数
                        commandQueue.drainTo(buffer, commandPipelineFlushThreshold);
                    }
                    if (!valid) continue;
                    boolean fail = false;
                    for (CommandWrapper wrapper : buffer) {
                        if (fail) {
                            for (CompletableFuture<Reply> future : wrapper.completableFutureList) {
                                future.complete(ErrorReply.NOT_AVAILABLE);
                            }
                            continue;
                        }
                        flushCommands.addAll(wrapper.commands);
                        for (CompletableFuture<Reply> future : wrapper.completableFutureList) {
                            if (fail) {
                                String log = clientName + " queue full, command return NOT_AVAILABLE";
                                ErrorLogCollector.collect(RedisClient.class, log);
                                future.complete(ErrorReply.NOT_AVAILABLE);
                                continue;
                            }
                            boolean offer = queue.offer(future);
                            if (!offer) {
                                String log = clientName + " queue full, command return NOT_AVAILABLE";
                                ErrorLogCollector.collect(RedisClient.class, log);
                                stop();
                                fail = true;
                                future.complete(ErrorReply.NOT_AVAILABLE);
                            }
                        }
                    }
                    buffer.clear();
                    if (fail) {
                        continue;
                    }
                    ByteBufAllocator allocator = channel.alloc();
                    if (logger.isDebugEnabled()) {
                        logger.debug("ByteBufAllocator = {}", allocator.getClass().getName());
                    }
                    ByteBuf buf = CommandsEncodeUtil.encode(allocator, flushCommands);
                    try {
                        channel.writeAndFlush(buf);
                    } catch (Exception e) {
                        buf.release();
                        throw e;
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("{} flush commands, commands.size = {}", clientName, flushCommands.size());
                    }
                    flushCommands.clear();
                } catch (Exception e) {
                    logger.error("{}, flush error", clientName, e);
                    stop();
                }
            }
            if (!buffer.isEmpty()) {
                String log = clientName + " stopping, command return NOT_AVAILABLE";
                for (CommandWrapper wrapper : buffer) {
                    for (CompletableFuture<Reply> future : wrapper.completableFutureList) {
                        future.complete(ErrorReply.NOT_AVAILABLE);
                        ErrorLogCollector.collect(RedisClient.class, log);
                    }
                }
            }
        }, "redis-commands-flush-" + clientName).start();
    }
}
