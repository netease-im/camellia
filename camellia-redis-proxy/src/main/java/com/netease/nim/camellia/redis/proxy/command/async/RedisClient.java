package com.netease.nim.camellia.redis.proxy.command.async;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.core.util.SysUtils;
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

import java.util.*;
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
    private static final ExecutorService disruptorShutdownExec = new ThreadPoolExecutor(SysUtils.getCpuNum(), SysUtils.getCpuNum(),
            0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000), new CamelliaThreadFactory("redis-client-disruptor-shutdown"));

    private EventLoopGroup loopGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("redis"));
    private final String host;
    private final int port;
    private final String password;
    private Channel channel;
    private volatile boolean valid = true;
    private ScheduledFuture<?> scheduledFuture;
    private final Queue<CompletableFuture<Reply>> queue = new LinkedBlockingQueue<>(100000);
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
            startCommandFlushThread();
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

    private Disruptor<CommandWrapperEvent> disruptor;
    private EventTranslatorOneArg<CommandWrapperEvent, CommandWrapper> translator;
    private void startCommandFlushThread() {
        int bufferSize = 65536;//必须是2的幂次
        disruptor = new Disruptor<>(CommandWrapperEvent::new, bufferSize,
                new CamelliaThreadFactory("redis-client-disruptor-" + clientName), ProducerType.MULTI, new SleepingWaitStrategy());
        disruptor.handleEventsWith(new CommandWrapperEventHandler(this));
        disruptor.start();
        translator = (event, sequence, commandWrapper) -> event.setCommandWrapper(commandWrapper);
    }

    private static class CommandWrapperEventHandler implements EventHandler<CommandWrapperEvent> {

        private final List<CommandWrapper> buffer = new ArrayList<>();
        private final List<Command> flushCommands = new ArrayList<>();
        private final RedisClient redisClient;
        private int commandSize = 0;

        public CommandWrapperEventHandler(RedisClient redisClient) {
            this.redisClient = redisClient;
        }

        @Override
        public void onEvent(CommandWrapperEvent event, long sequence, boolean endOfBatch) {
            try {
                if (!redisClient.valid) {
                    String log = redisClient.clientName + " is not valid, command return NOT_AVAILABLE while onEvent";
                    if (!buffer.isEmpty()) {
                        for (CommandWrapper commandWrapper : buffer) {
                            for (CompletableFuture<Reply> future : commandWrapper.getCompletableFutureList()) {
                                future.complete(ErrorReply.NOT_AVAILABLE);
                                ErrorLogCollector.collect(RedisClient.class, log);
                            }
                        }
                        buffer.clear();
                    }
                    CommandWrapper commandWrapper = event.getCommandWrapper();
                    event.setCommandWrapper(null);
                    for (CompletableFuture<Reply> future : commandWrapper.getCompletableFutureList()) {
                        future.complete(ErrorReply.NOT_AVAILABLE);
                        ErrorLogCollector.collect(RedisClient.class, log);
                    }
                    return;
                }
                CommandWrapper commandWrapper = event.getCommandWrapper();
                event.setCommandWrapper(null);
                buffer.add(commandWrapper);
                commandSize += commandWrapper.getCommands().size();
                boolean needFlush = false;
                if (endOfBatch) {
                    needFlush = true;
                } else {
                    if (commandSize >= redisClient.commandPipelineFlushThreshold) {
                        needFlush = true;
                    }
                }
                if (needFlush) {
                    boolean fail = false;
                    for (CommandWrapper wrapper : buffer) {
                        flushCommands.addAll(wrapper.getCommands());
                        for (CompletableFuture<Reply> future : wrapper.getCompletableFutureList()) {
                            if (fail) {
                                future.complete(ErrorReply.NOT_AVAILABLE);
                                continue;
                            }
                            boolean offer = redisClient.queue.offer(future);
                            if (!offer) {
                                fail = true;
                                future.complete(ErrorReply.NOT_AVAILABLE);
                                break;
                            }
                        }
                    }
                    if (fail) {
                        redisClient.stop();
                        return;
                    }
                    Channel channel = redisClient.channel;
                    ByteBufAllocator allocator = channel.alloc();
                    ByteBuf buf = CommandsEncodeUtil.encode(allocator, flushCommands);
                    channel.writeAndFlush(buf);
                    flushCommands.clear();
                    buffer.clear();
                    commandSize = 0;
                }
            } catch (Exception e) {
                logger.error("{} flush command error, redis client will stop", redisClient.clientName, e);
                redisClient.stop();
            }
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
        if (!valid && queue.isEmpty() && disruptor == null
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
                if (disruptor != null) {
                    disruptorShutdownExec.submit(() -> {
                        try {
                            disruptor.shutdown();
                        } catch (Exception e) {
                            logger.error("{} disruptor shutdown error", clientName, e);
                        }
                    });
                    disruptor = null;
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
            String log = clientName + " is not valid, command return NOT_AVAILABLE while sendCommand";
            for (CompletableFuture<Reply> future : completableFutureList) {
                future.complete(ErrorReply.NOT_AVAILABLE);
                ErrorLogCollector.collect(RedisClient.class, log);
            }
            return;
        }
        boolean success = disruptor.getRingBuffer().tryPublishEvent(translator, new CommandWrapper(commands, completableFutureList));
        if (!success) {
            String log = clientName + " disruptor full, command return NOT_AVAILABLE while sendCommand";
            for (CompletableFuture<Reply> future : completableFutureList) {
                future.complete(ErrorReply.NOT_AVAILABLE);
                ErrorLogCollector.collect(RedisClient.class, log);
            }
        }
    }
}
