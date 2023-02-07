package com.netease.nim.camellia.redis.proxy.upstream.connection;

import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.netty.*;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.monitor.RedisConnectionMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.SysUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2019/12/17.
 */
public class RedisConnection implements IUpstreamClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisConnection.class);
    private static final AtomicLong id = new AtomicLong(0);

    private static final ScheduledExecutorService heartBeatScheduled = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-redis-connection-heart-beat"));
    private static final ScheduledExecutorService idleCheckScheduled = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-redis-connection-idle-check"));
    private static final ExecutorService initializeExecutor = new ThreadPoolExecutor(SysUtils.getCpuNum(), SysUtils.getCpuNum(), 0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10240), new DefaultThreadFactory("camellia-redis-connection-initialize"), new ThreadPoolExecutor.AbortPolicy());

    private final RedisConnectionConfig config;

    private final RedisConnectionAddr addr;
    private final String host;
    private final int port;
    private final String userName;
    private final String password;
    private final int db;

    private final EventLoopGroup eventLoopGroup;
    private final String connectionName;
    private final Object lock = new Object();
    private final AtomicBoolean stop = new AtomicBoolean(false);

    private final int heartbeatIntervalSeconds;
    private final long heartbeatTimeoutMillis;
    private final int connectTimeoutMillis;

    private boolean closeIdleConnection;
    private final long checkIdleThresholdSeconds;
    private final int closeIdleConnectionDelaySeconds;

    private Channel channel;
    private ScheduledFuture<?> heartbeatScheduledFuture;
    private ScheduledFuture<?> idleCheckScheduledFuture;

    //status
    private volatile RedisConnectionStatus status;
    private long lastCommandTime = TimeCache.currentMillis;

    private final Queue<CompletableFuture<Reply>> queue = new LinkedBlockingQueue<>(1024*32);
    private final Queue<CommandPack> cachedCommands = new LinkedBlockingQueue<>(1024*10);

    public RedisConnection(RedisConnectionConfig config) {
        this.config = config;
        this.host = config.getHost();
        this.port = config.getPort();
        this.userName = config.getUserName();
        this.password = config.getPassword();
        this.db = config.getDb();
        this.addr = new RedisConnectionAddr(host, port, userName, password, config.isReadonly(), config.getDb());
        this.eventLoopGroup = config.getEventLoopGroup();
        this.heartbeatIntervalSeconds = config.getHeartbeatIntervalSeconds();
        this.heartbeatTimeoutMillis = config.getHeartbeatTimeoutMillis();
        this.connectTimeoutMillis = config.getConnectTimeoutMillis();
        this.closeIdleConnection = config.isCloseIdleConnection();
        this.checkIdleThresholdSeconds = config.getCheckIdleConnectionThresholdSeconds() <= 0
                ? Constants.Transpond.checkIdleConnectionThresholdSeconds : config.getCheckIdleConnectionThresholdSeconds();
        this.closeIdleConnectionDelaySeconds = config.getCloseIdleConnectionDelaySeconds() <=0
                ? Constants.Transpond.closeIdleConnectionDelaySeconds : config.getCloseIdleConnectionDelaySeconds();
        this.connectionName = "RedisConnection[" + PasswordMaskUtils.maskAddr(addr.getUrl()) + "][id=" + id.incrementAndGet() + "]";
        this.status = RedisConnectionStatus.INITIALIZE;
    }

    /**
     * 开启
     */
    public void start() {
        try {
            if (config.getFastFailStats().fastFail(addr.getUrl())) {
                status = RedisConnectionStatus.INVALID;
                return;
            }
            RedisConnectionMonitor.addRedisConnection(this);
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(GlobalRedisProxyEnv.getSocketChannelClass())
                    .option(ChannelOption.SO_KEEPALIVE, config.isSoKeepalive())
                    .option(ChannelOption.TCP_NODELAY, config.isTcpNoDelay())
                    .option(ChannelOption.SO_SNDBUF, config.getSoSndbuf())
                    .option(ChannelOption.SO_RCVBUF, config.getSoRcvbuf())
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                    .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(config.getWriteBufferWaterMarkLow(),
                            config.getWriteBufferWaterMarkHigh()))
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new ReplyDecoder());
                            pipeline.addLast(new ReplyAggregateDecoder());
                            pipeline.addLast(new ClientHandler(queue, connectionName, config.isTcpQuickAck()));
                            pipeline.addLast(new CommandPackEncoder(RedisConnection.this, queue));
                        }
                    });
            if (config.isTcpQuickAck()) {
                bootstrap.option(EpollChannelOption.TCP_QUICKACK, Boolean.TRUE);
            }
            if (logger.isInfoEnabled()) {
                logger.info("{} try connect...", connectionName);
            }
            ChannelFuture future = bootstrap.connect(host, port);
            this.channel = future.channel();
            this.channel.closeFuture().addListener(f -> {
                logger.warn("{} connect close, will stop", connectionName);
                stop();
            });
            future.addListener((ChannelFutureListener) channelFuture -> {
                if (channelFuture.isSuccess()) {
                    if (logger.isInfoEnabled()) {
                        logger.info("{} connect success", connectionName);
                    }
                    try {
                        initializeExecutor.submit(this::initialize);
                    } catch (Exception e) {
                        status = RedisConnectionStatus.INVALID;
                        logger.error("{} submit initialize task fail", connectionName, e);
                        stop();
                    }
                } else {
                    config.getFastFailStats().incrFail(addr.getUrl());
                    status = RedisConnectionStatus.INVALID;
                    logger.error("{} connect fail", connectionName, future.cause());
                }
            });
        } catch (Exception e) {
            stop();
            logger.error("{} start fail", connectionName, e);
        }
    }

    /**
     * 开启空闲检测
     */
    public void startIdleCheck() {
        if (idleCheckScheduledFuture != null && closeIdleConnection) {
            return;
        }
        synchronized (this) {
            if (idleCheckScheduledFuture == null) {
                lastCommandTime = TimeCache.currentMillis;
                closeIdleConnection = true;
                idleCheckScheduledFuture = idleCheckScheduled.scheduleAtFixedRate(this::checkIdle,
                        checkIdleThresholdSeconds, checkIdleThresholdSeconds, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * 关闭空闲检测
     */
    public void stopIdleCheck() {
        if (idleCheckScheduledFuture == null && !closeIdleConnection) {
            return;
        }
        synchronized (this) {
            lastCommandTime = TimeCache.currentMillis;
            closeIdleConnection = false;
            if (idleCheckScheduledFuture != null) {
                idleCheckScheduledFuture.cancel(false);
                idleCheckScheduledFuture = null;
            }
        }
    }



    /**
     * 关闭
     */
    public void stop() {
        stop(false);
    }

    /**
     * 关闭
     * @param grace 如果为true，则不打印相关错误日志
     */
    public void stop(boolean grace) {
        if (status == RedisConnectionStatus.CLOSING) {
            return;
        }
        _stop(grace);
    }

    /**
     * 是否有效
     * @return true/false
     */
    @Override
    public boolean isValid() {
        return status == RedisConnectionStatus.VALID || status == RedisConnectionStatus.INITIALIZE;
    }

    /**
     * 获取status
     * @return status
     */
    public RedisConnectionStatus getStatus() {
        return status;
    }

    /**
     * 未回包的命令数量
     * @return 数量
     */
    public int queueSize() {
        return queue.size() + cachedCommands.size();
    }

    /**
     * 获取连接地址
     * @return 地址
     */
    public RedisConnectionAddr getAddr() {
        return addr;
    }

    /**
     * 获取连接名称
     * @return 名称
     */
    public String getConnectionName() {
        return connectionName;
    }

    /**
     * 获取连接配置
     * @return 连接配置
     */
    public RedisConnectionConfig getConfig() {
        return config;
    }

    /**
     * send command
     * @param args args
     * @return future
     */
    public CompletableFuture<Reply> sendCommand(byte[]... args) {
        CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
        sendCommand(Collections.singletonList(new Command(args)), Collections.singletonList(completableFuture));
        return completableFuture;
    }

    /**
     * send command
     * @param commands commands
     * @param completableFutureList future list
     */
    public void sendCommand(List<Command> commands, List<CompletableFuture<Reply>> completableFutureList) {
        if (status == RedisConnectionStatus.INVALID || status == RedisConnectionStatus.CLOSING) {
            String log = connectionName + " is " + status + ", command return NOT_AVAILABLE";
            for (CompletableFuture<Reply> future : completableFutureList) {
                future.complete(ErrorReply.NOT_AVAILABLE);
                ErrorLogCollector.collect(RedisConnection.class, log);
            }
            return;
        }
        CommandPack pack = new CommandPack(commands, completableFutureList, time());
        if (logger.isDebugEnabled()) {
            logger.debug("{} sendCommands, commands.size = {}", connectionName, commands.size());
        }
        if (status == RedisConnectionStatus.VALID) {
            writeAndFlush(pack);
        } else if (status == RedisConnectionStatus.INITIALIZE) {
            synchronized (cachedCommands) {
                if (status == RedisConnectionStatus.VALID && cachedCommands.isEmpty()) {
                    writeAndFlush(pack);
                } else if (status == RedisConnectionStatus.INVALID || status == RedisConnectionStatus.CLOSING) {
                    String log = connectionName + " is " + status + ", command return NOT_AVAILABLE";
                    for (CompletableFuture<Reply> future : completableFutureList) {
                        future.complete(ErrorReply.NOT_AVAILABLE);
                        ErrorLogCollector.collect(RedisConnection.class, log);
                    }
                } else {
                    boolean success = cachedCommands.offer(pack);
                    if (!success) {
                        String log = connectionName + ", cachedCommands queue is full, command return NOT_AVAILABLE";
                        for (CompletableFuture<Reply> future : completableFutureList) {
                            future.complete(ErrorReply.NOT_AVAILABLE);
                            ErrorLogCollector.collect(RedisConnection.class, log);
                        }
                    }
                }
            }
        }
        if (closeIdleConnection) {
            lastCommandTime = TimeCache.currentMillis;
        }
    }

    @Override
    public void preheat() {
        //do nothing
    }

    @Override
    public String getUrl() {
        return addr.getUrl();
    }

    //初始化
    private void initialize() {
        try {
            //1、auth
            auth();
            //2、ping
            ping(connectTimeoutMillis);
            //3、select db
            selectDB();
            //4、readonly
            sendReadonlyCommand();
            //5、schedule
            startSchedule();
            //6、send cached commands
            synchronized (cachedCommands) {
                int size = cachedCommands.size();
                if (logger.isInfoEnabled() && size > 0) {
                    logger.info("{} flushCachedCommands, size = {}", connectionName, size);
                }
                flushCachedCommands();
                status = RedisConnectionStatus.VALID;
                config.getFastFailStats().resetFail(addr.getUrl());
                if (logger.isInfoEnabled()) {
                    logger.info("{} initialize success", connectionName);
                }
            }
        } catch (Exception e) {
            config.getFastFailStats().incrFail(addr.getUrl());
            status = RedisConnectionStatus.INVALID;
            logger.error("{} initialize fail", connectionName, e);
            stop();
        }
    }

    //发送心跳
    private void heartbeat() {
        synchronized (this) {
            try {
                if (status != RedisConnectionStatus.VALID) {
                    return;
                }
                ping(heartbeatTimeoutMillis);
            } catch (Exception e) {
                logger.error("{} heartbeat error", connectionName, e);
            }
        }
    }

    //发送ping，并检测回包
    private void ping(long timeoutMillis) {
        CompletableFuture<Reply> future = sendPing();
        try {
            Reply reply = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            if (reply instanceof StatusReply) {
                if (((StatusReply) reply).getStatus().equalsIgnoreCase(StatusReply.PONG.getStatus())) {
                    return;
                }
            }
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                if (replies.length > 0) {
                    Reply reply1 = replies[0];
                    if (reply1 instanceof BulkReply) {
                        if (Utils.bytesToString(((BulkReply) reply1).getRaw()).equalsIgnoreCase(StatusReply.PONG.getStatus())) {
                            return;
                        }
                    }
                }
            }
            logger.error("{} ping fail, response = {}", connectionName, reply);
            throw new CamelliaRedisException("ping fail");
        } catch (Exception e) {
            logger.error("{} ping timeout, timeoutMillis = {}", connectionName, heartbeatTimeoutMillis, e);
            throw new CamelliaRedisException(e);
        }
    }

    //关闭（是否优雅的）
    private void _stop(boolean grace) {
        RedisConnectionMonitor.removeRedisConnection(this);
        if (!stop.compareAndSet(false, true)) return;
        if (!grace) {
            String log = connectionName + " stopping, command maybe return NOT_AVAILABLE";
            ErrorLogCollector.collect(RedisConnection.class, log);
        }
        try {
            status = RedisConnectionStatus.INVALID;
            try {
                if (channel != null) {
                    channel.close();
                    channel = null;
                }
            } catch (Exception e) {
                logger.error("{}, channel close error", connectionName, e);
            }
            try {
                if (heartbeatScheduledFuture != null) {
                    heartbeatScheduledFuture.cancel(false);
                    heartbeatScheduledFuture = null;
                }
            } catch (Exception e) {
                logger.error("{}, heart-beat schedule cancel error", connectionName, e);
            }
            try {
                if (idleCheckScheduledFuture != null) {
                    idleCheckScheduledFuture.cancel(false);
                    idleCheckScheduledFuture = null;
                }
            } catch (Exception e) {
                logger.error("{}, idle-check schedule cancel error", connectionName, e);
            }
            int count = 0;
            while (!queue.isEmpty()) {
                CompletableFuture<Reply> future = queue.poll();
                if (future != null) {
                    future.complete(ErrorReply.NOT_AVAILABLE);
                    count ++;
                }
            }
            while (!cachedCommands.isEmpty()) {
                CommandPack commandPack = cachedCommands.poll();
                if (commandPack != null) {
                    for (CompletableFuture<Reply> future : commandPack.getCompletableFutureList()) {
                        future.complete(ErrorReply.NOT_AVAILABLE);
                        count ++;
                    }
                }
            }
            if (count > 0 && !grace) {
                String log = connectionName + ", " + count + " commands return NOT_AVAILABLE";
                ErrorLogCollector.collect(RedisConnection.class, log);
            }
        } catch (Exception e) {
            logger.error("{} stop error", connectionName, e);
        }
    }

    //发送PING命令
    private CompletableFuture<Reply> sendPing() {
        Command command = new Command(new byte[][]{RedisCommand.PING.raw()});
        CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
        if (logger.isDebugEnabled()) {
            logger.debug("{} send ping for heart-beat", connectionName);
        }
        sendCommandDirect(command, completableFuture);
        return completableFuture;
    }

    //发送SELECT命令
    private void selectDB() {
        if (config.getDb() == 0) return;
        Command command = new Command(new byte[][]{RedisCommand.SELECT.raw(), Utils.stringToBytes(String.valueOf(config.getDb()))});
        CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
        if (logger.isInfoEnabled()) {
            logger.info("{} select db={}", connectionName, db);
        }
        sendCommandDirect(command, completableFuture);
    }

    //发送READONLY命令
    private void sendReadonlyCommand() {
        if (!addr.isReadonly()) return;
        Command command = new Command(new byte[][]{RedisCommand.READONLY.raw()});
        CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
        if (logger.isInfoEnabled()) {
            logger.info("{} send READONLY command", connectionName);
        }
        sendCommandDirect(command, completableFuture);
    }

    //直接发送命令，不检查连接状态
    private void sendCommandDirect(Command command, CompletableFuture<Reply> future) {
        CommandPack pack = new CommandPack(Collections.singletonList(command), Collections.singletonList(future), time());
        writeAndFlush(pack);
    }

    //发送AUTH命令，并检查回包
    private void auth() throws Exception {
        if (password != null) {
            logger.info("{} need password, try auth", connectionName);
            boolean authSuccess = false;
            CompletableFuture<Reply> future = new CompletableFuture<>();
            if (userName == null) {
                sendCommandDirect(new Command(new byte[][]{RedisCommand.AUTH.raw(), Utils.stringToBytes(password)}), future);
            } else {
                sendCommandDirect(new Command(new byte[][]{RedisCommand.AUTH.raw(), Utils.stringToBytes(userName), Utils.stringToBytes(password)}), future);
            }
            Reply reply = future.get(connectTimeoutMillis, TimeUnit.MILLISECONDS);
            if (reply instanceof StatusReply) {
                if (((StatusReply) reply).getStatus().equalsIgnoreCase(StatusReply.OK.getStatus())) {
                    if (logger.isInfoEnabled()) {
                        logger.info("{} auth success", connectionName);
                    }
                    authSuccess = true;
                }
            }
            if (!authSuccess) {
                throw new CamelliaRedisException("auth fail");
            }
        }
    }

    //开启定时检测和定时心跳
    private void startSchedule() {
        if (heartbeatIntervalSeconds > 0 && heartbeatTimeoutMillis > 0) {
            //默认60s发送一个心跳，心跳超时时间10s，如果超时了，则关闭当前连接
            this.heartbeatScheduledFuture = heartBeatScheduled.scheduleAtFixedRate(this::heartbeat,
                    heartbeatIntervalSeconds, heartbeatIntervalSeconds, TimeUnit.SECONDS);
        }
        if (closeIdleConnection && checkIdleThresholdSeconds > 0 && closeIdleConnectionDelaySeconds > 0) {
            this.idleCheckScheduledFuture = idleCheckScheduled.scheduleAtFixedRate(this::checkIdle,
                    checkIdleThresholdSeconds, checkIdleThresholdSeconds, TimeUnit.SECONDS);
        }
    }

    //刷新缓存的命令
    private void flushCachedCommands() {
        while (!cachedCommands.isEmpty()) {
            CommandPack commandPack = cachedCommands.poll();
            if (commandPack == null) break;
            writeAndFlush(commandPack);
        }
    }

    //是否空闲
    private boolean isIdle() {
        if (!queue.isEmpty() || !cachedCommands.isEmpty()) return false;
        if (checkIdleThresholdSeconds > 0) {
            return TimeCache.currentMillis - lastCommandTime > checkIdleThresholdSeconds * 1000L;
        } else {
            return TimeCache.currentMillis - lastCommandTime > Constants.Transpond.checkIdleConnectionThresholdSeconds * 1000L;
        }
    }

    //检查是否idle
    private void checkIdle() {
        synchronized (this) {
            try {
                if (isIdle()) {
                    status = RedisConnectionStatus.CLOSING;
                    if (logger.isInfoEnabled()) {
                        logger.info("{} will close after {} seconds because connection is idle, idle.seconds = {}", connectionName, closeIdleConnectionDelaySeconds, checkIdleThresholdSeconds);
                    }
                    try {
                        ExecutorUtils.submitDelayTask(() -> {
                            try {
                                if (isIdle()) {
                                    if (logger.isInfoEnabled()) {
                                        logger.info("{} will close because connection is idle", connectionName);
                                    }
                                    _stop(true);
                                } else {
                                    logger.warn("{} will not close because connection is not idle, will continue check idle task", connectionName);
                                }
                            } catch (Exception e) {
                                logger.error("{} delay close error", connectionName, e);
                            }
                        }, closeIdleConnectionDelaySeconds, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        logger.error("submit delay close task error, stop right now, client = {}", connectionName, e);
                        _stop(false);
                    }
                }
            } catch (Exception e) {
                logger.error("{} idle check error", connectionName, e);
            }
        }
    }

    //写入命令
    private void writeAndFlush(CommandPack commandPack) {
        channel.writeAndFlush(commandPack);
    }

    //获取当前时间（ns）
    private long time() {
        if (ProxyMonitorCollector.isUpstreamRedisSpendTimeMonitorEnable() && !config.isSkipCommandSpendTimeMonitor()) {
            return System.nanoTime();
        } else {
            return -1;
        }
    }


}