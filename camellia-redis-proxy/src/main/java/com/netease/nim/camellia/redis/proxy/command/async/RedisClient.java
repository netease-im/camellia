package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.monitor.RedisMonitor;
import com.netease.nim.camellia.redis.proxy.netty.ClientHandler;
import com.netease.nim.camellia.redis.proxy.netty.CommandPack;
import com.netease.nim.camellia.redis.proxy.netty.CommandPackEncoder;
import com.netease.nim.camellia.redis.proxy.netty.ReplyDecoder;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final ScheduledExecutorService heartBeatScheduled = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-redis-client-heart-beat"));
    private static final ScheduledExecutorService idleCheckScheduled = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-redis-client-idle-check"));

    private final RedisClientConfig redisClientConfig;

    private final RedisClientAddr addr;
    private final String host;
    private final int port;
    private final String password;

    private final EventLoopGroup eventLoopGroup;
    private final String clientName;
    private final Object lock = new Object();

    private final int heartbeatIntervalSeconds;
    private final long heartbeatTimeoutMillis;
    private final int connectTimeoutMillis;

    private final boolean closeIdleConnection;
    private final long checkIdleThresholdSeconds;
    private final int closeIdleConnectionDelaySeconds;

    private Channel channel;
    private ScheduledFuture<?> heartbeatScheduledFuture;
    private ScheduledFuture<?> idleCheckScheduledFuture;

    //status
    private volatile boolean valid = true;
    private volatile boolean closing = false;
    private long lastCommandTime = TimeCache.currentMillis;

    private final Queue<CompletableFuture<Reply>> queue = new LinkedBlockingQueue<>(1024*32);

    public RedisClient(RedisClientConfig config) {
        this.redisClientConfig = config;
        this.host = config.getHost();
        this.port = config.getPort();
        this.password = config.getPassword();
        this.addr = new RedisClientAddr(host, port, password);
        this.eventLoopGroup = config.getEventLoopGroup();
        this.heartbeatIntervalSeconds = config.getHeartbeatIntervalSeconds();
        this.heartbeatTimeoutMillis = config.getHeartbeatTimeoutMillis();
        this.connectTimeoutMillis = config.getConnectTimeoutMillis();
        this.closeIdleConnection = config.isCloseIdleConnection();
        this.checkIdleThresholdSeconds = config.getCheckIdleConnectionThresholdSeconds() <= 0
                ? Constants.Transpond.checkIdleConnectionThresholdSeconds : config.getCheckIdleConnectionThresholdSeconds();
        this.closeIdleConnectionDelaySeconds = config.getCloseIdleConnectionDelaySeconds() <=0
                ? Constants.Transpond.closeIdleConnectionDelaySeconds : config.getCloseIdleConnectionDelaySeconds();
        if (PasswordMaskUtils.maskEnable) {
            this.clientName = "RedisClient[" + (password == null ? "" : PasswordMaskUtils.maskStr(password.length()))
                    + "@" + host + ":" + port + "][id=" + id.incrementAndGet() + "]";
        } else {
            this.clientName = "RedisClient[" + (password == null ? "" : password)
                    + "@" + host + ":" + port + "][id=" + id.incrementAndGet() + "]";
        }
    }

    public void start() {
        try {
            RedisMonitor.addRedisClient(this);
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
                CompletableFuture<Reply> future = sendCommand(RedisCommand.AUTH.raw(), Utils.stringToBytes(password));
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
                this.heartbeatScheduledFuture = heartBeatScheduled.scheduleAtFixedRate(this::heartbeat,
                        heartbeatIntervalSeconds, heartbeatIntervalSeconds, TimeUnit.SECONDS);
            }
            if (closeIdleConnection && checkIdleThresholdSeconds > 0 && closeIdleConnectionDelaySeconds > 0) {
                this.idleCheckScheduledFuture = idleCheckScheduled.scheduleAtFixedRate(this::checkIdle,
                        checkIdleThresholdSeconds, checkIdleThresholdSeconds, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            stop();
            logger.error("{} start fail", clientName, e);
        }
    }

    private void checkIdle() {
        synchronized (this) {
            try {
                if (isIdle()) {
                    closing = true;
                    logger.info("{} will close after {} seconds because connection is idle, idle.seconds = {}",
                            clientName, closeIdleConnectionDelaySeconds, checkIdleThresholdSeconds);
                    try {
                        ExecutorUtils.newTimeout(timeout -> {
                            try {
                                if (isIdle()) {
                                    logger.info("{} will close because connection is idle", clientName);
                                    _stop(true);
                                } else {
                                    logger.warn("{} will not close because connection is not idle, will continue check idle task", clientName);
                                }
                            } catch (Exception e) {
                                logger.error("{} delay close error", clientName, e);
                            }
                        }, closeIdleConnectionDelaySeconds, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        logger.error("submit delay close task error, stop right now, client = {}", clientName, e);
                        _stop(false);
                    }
                }
            } catch (Exception e) {
                logger.error("{} idle check error", clientName, e);
            }
        }
    }

    public void startIdleCheck() {
        synchronized (this) {
            if (idleCheckScheduledFuture == null) {
                idleCheckScheduledFuture = idleCheckScheduled.scheduleAtFixedRate(this::checkIdle,
                        checkIdleThresholdSeconds, checkIdleThresholdSeconds, TimeUnit.SECONDS);
            }
        }
    }

    private void heartbeat() {
        synchronized (this) {
            try {
                if (!valid) return;
                if (closing) return;
                if (!ping(heartbeatTimeoutMillis)) {
                    stop();
                }
            } catch (Exception e) {
                logger.error("{} heartbeat error", clientName, e);
            }
        }
    }

    private boolean ping(long timeoutMillis) {
        CompletableFuture<Reply> future = sendPing();
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
                        if (Utils.bytesToString(((BulkReply) reply1).getRaw()).equalsIgnoreCase(StatusReply.PONG.getStatus())) {
                            return true;
                        }
                    }
                }
            }
            logger.error("{} ping fail, response = {}", clientName, reply);
            return false;
        } catch (Exception e) {
            logger.error("{} ping timeout, timeoutMillis = {}", clientName, heartbeatTimeoutMillis, e);
            return false;
        }
    }

    public boolean isIdle() {
        if (!queue.isEmpty()) return false;
        if (checkIdleThresholdSeconds > 0) {
            return TimeCache.currentMillis - lastCommandTime > checkIdleThresholdSeconds * 1000L;
        } else {
            return TimeCache.currentMillis - lastCommandTime > Constants.Transpond.checkIdleConnectionThresholdSeconds * 1000L;
        }
    }

    public void stop() {
        stop(false);
    }

    public void stop(boolean grace) {
        if (closing) {
            return;
        }
        _stop(grace);
    }

    private void _stop(boolean grace) {
        RedisMonitor.removeRedisClient(this);
        if (!valid && queue.isEmpty()
                && channel == null && heartbeatScheduledFuture == null && idleCheckScheduledFuture == null) {
            return;
        }
        synchronized (lock) {
            if (!valid && queue.isEmpty()
                    && channel == null && heartbeatScheduledFuture == null && idleCheckScheduledFuture == null) {
                return;
            }
            if (!grace) {
                String log = clientName + " stopping, command maybe return NOT_AVAILABLE";
                ErrorLogCollector.collect(RedisClient.class, log);
            }
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
                    if (heartbeatScheduledFuture != null) {
                        heartbeatScheduledFuture.cancel(false);
                        heartbeatScheduledFuture = null;
                    }
                } catch (Exception e) {
                    logger.error("{}, heart-beat schedule cancel error", clientName, e);
                }
                try {
                    if (idleCheckScheduledFuture != null) {
                        idleCheckScheduledFuture.cancel(false);
                        idleCheckScheduledFuture = null;
                    }
                } catch (Exception e) {
                    logger.error("{}, idle-check schedule cancel error", clientName, e);
                }
                int count = 0;
                while (!queue.isEmpty()) {
                    CompletableFuture<Reply> future = queue.poll();
                    if (future != null) {
                        future.complete(ErrorReply.NOT_AVAILABLE);
                        count ++;
                    }
                }
                if (count > 0 && !grace) {
                    String log = clientName + ", " + count + " commands return NOT_AVAILABLE";
                    ErrorLogCollector.collect(RedisClient.class, log);
                }
            } catch (Exception e) {
                logger.error("{} stop error", clientName, e);
            }
        }
    }

    public boolean isValid() {
        if (closing) return false;
        return valid;
    }

    public int queueSize() {
        return queue.size();
    }

    public RedisClientAddr getAddr() {
        return addr;
    }

    public String getClientName() {
        return clientName;
    }

    public RedisClientConfig getRedisClientConfig() {
        return redisClientConfig;
    }

    private CompletableFuture<Reply> sendPing() {
        List<Command> commands = Collections.singletonList(new Command(new byte[][]{RedisCommand.PING.raw()}));
        CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
        List<CompletableFuture<Reply>> futures = Collections.singletonList(completableFuture);
        CommandPack pack = new CommandPack(commands, futures);
        if (logger.isDebugEnabled()) {
            logger.debug("{} send ping for heart-beat", clientName);
        }
        channel.writeAndFlush(pack);
        return completableFuture;
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
        if (closeIdleConnection) {
            lastCommandTime = TimeCache.currentMillis;
        }
    }

    @Override
    public void preheat() {
        //do nothing
    }
}