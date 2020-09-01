package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.netty.ClientHandler;
import com.netease.nim.camellia.redis.proxy.netty.CommandPack;
import com.netease.nim.camellia.redis.proxy.netty.CommandPackEncoder;
import com.netease.nim.camellia.redis.proxy.netty.ReplyDecoder;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

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

    private final String host;
    private final int port;
    private final String password;
    private Channel channel;
    private volatile boolean valid = true;
    private ScheduledFuture<?> scheduledFuture;
    private final int heartbeatIntervalSeconds;
    private final long heartbeatTimeoutMillis;
    private final int connectTimeoutMillis;
    private final String clientName;
    private final Object lock = new Object();
    private final EventLoopGroup eventLoopGroup;

    private final Queue<CompletableFuture<Reply>> queue = new ConcurrentLinkedQueue<>();

    public RedisClient(String host, int port, String password, EventLoopGroup eventLoopGroup,
                       int heartbeatIntervalSeconds, long heartbeatTimeoutMillis, int connectTimeoutMillis) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.eventLoopGroup = eventLoopGroup;
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        this.heartbeatTimeoutMillis = heartbeatTimeoutMillis;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.clientName = "RedisClient[" + (password == null ? "" : password) + "@" + host + ":" + port + "][id=" + id.incrementAndGet() + "]";
    }

    public void start() {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_SNDBUF, 10485760)
                    .option(ChannelOption.SO_RCVBUF, 10485760)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                    .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(128 * 1024, 512 * 1024))
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new ReplyDecoder());
                            pipeline.addLast(new ClientHandler(queue, clientName));
                            pipeline.addLast(new CommandPackEncoder(RedisClient.this, queue));
                        }
                    });
            logger.info("{} try connect...", clientName);
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            this.channel = channelFuture.channel();
            logger.info("{} connect success", clientName);
            valid = true;
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
                logger.warn("{} connect close, will stop", clientName);
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
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                if (replies.length > 0) {
                    Reply reply1 = replies[0];
                    if (reply1 instanceof BulkReply) {
                        if (SafeEncoder.encode(((BulkReply) reply1).getRaw()).equalsIgnoreCase(StatusReply.PONG.getStatus())) {
                            return true;
                        }
                    }
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
        stop(false);
    }

    public void stop(boolean grace) {
        if (!valid && queue.isEmpty()
                && channel == null && scheduledFuture == null) {
            return;
        }
        if (!grace) {
            String log = clientName + " stopping, command return NOT_AVAILABLE";
            ErrorLogCollector.collect(RedisClient.class, log);
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
                    if (scheduledFuture != null) {
                        scheduledFuture.cancel(false);
                        scheduledFuture = null;
                    }
                } catch (Exception e) {
                    logger.error("{}, heart-beat schedule cancel error", clientName, e);
                }
                while (!queue.isEmpty()) {
                    CompletableFuture<Reply> future = queue.poll();
                    if (future != null) {
                        future.complete(ErrorReply.NOT_AVAILABLE);
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
        CommandPack pack = new CommandPack(commands, completableFutureList);
        if (logger.isDebugEnabled()) {
            logger.debug("{} sendCommands, commands.size = {}", clientName, commands.size());
        }
        channel.writeAndFlush(pack);
    }
}
