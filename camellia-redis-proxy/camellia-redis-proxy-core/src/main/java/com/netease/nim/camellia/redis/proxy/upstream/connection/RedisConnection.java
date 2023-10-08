package com.netease.nim.camellia.redis.proxy.upstream.connection;

import com.netease.nim.camellia.tools.executor.CamelliaScheduleExecutor;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.netty.*;
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
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2019/12/17.
 */
public class RedisConnection {

    private static final Logger logger = LoggerFactory.getLogger(RedisConnection.class);
    private static final AtomicLong id = new AtomicLong(0);

    private static final CamelliaScheduleExecutor heartBeatScheduled = new CamelliaScheduleExecutor("camellia-redis-connection-heartbeat", 1, 1024*32);
    private static final CamelliaScheduleExecutor idleCheckScheduled = new CamelliaScheduleExecutor("camellia-redis-connection-idle-check", 1, 1024*32);
    private static final ExecutorService initializeExecutor = new ThreadPoolExecutor(SysUtils.getCpuNum(), SysUtils.getCpuNum(), 0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10240), new DefaultThreadFactory("camellia-redis-connection-initialize"), new ThreadPoolExecutor.AbortPolicy());

    private final RedisConnectionConfig config;

    private final RedisConnectionAddr addr;
    private final String host;
    private final int port;
    private final String udsPath;
    private final String userName;
    private final String password;
    private final int db;

    private final EventLoop eventLoop;
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
    private volatile CamelliaScheduleExecutor.Task heartbeatTask;
    private volatile CamelliaScheduleExecutor.Task idleCheckTask;

    //status
    private volatile RedisConnectionStatus status;
    private long lastCommandTime = TimeCache.currentMillis;

    private final Queue<CompletableFuture<Reply>> queue = GlobalRedisProxyEnv.getQueueFactory().generateCommandReplyQueue();
    private final Queue<CommandPack> cachedCommands = new ConcurrentLinkedQueue<>();

    private final CommandPackRecycler commandPackRecycler;

    public RedisConnection(RedisConnectionConfig config) {
        this.config = config;
        this.host = config.getHost();
        this.port = config.getPort();
        this.udsPath = config.getUdsPath();
        this.userName = config.getUserName();
        this.password = config.getPassword();
        this.db = config.getDb();
        if (host != null && port > 0) {
            this.addr = new RedisConnectionAddr(host, port, userName, password, config.isReadonly(), config.getDb(), false);
        } else {
            this.addr = new RedisConnectionAddr(udsPath, userName, password, config.isReadonly(), config.getDb(), false);
        }
        this.eventLoop = config.getEventLoop();
        this.commandPackRecycler = new CommandPackRecycler(eventLoop);
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
            bootstrap.group(eventLoop);
            ChannelType channelType = Utils.channelType(addr);
            Class<? extends Channel> socketChannel = Utils.socketChannel(channelType, eventLoop);
            if (socketChannel == null) {
                status = RedisConnectionStatus.INVALID;
                ErrorLogCollector.collect(RedisConnection.class, "socketChannel is null, channelType = " + channelType);
                return;
            }
            if (channelType == ChannelType.tcp) {
                bootstrap.channel(socketChannel)
                        .option(ChannelOption.SO_KEEPALIVE, config.isSoKeepalive())
                        .option(ChannelOption.TCP_NODELAY, config.isTcpNoDelay())
                        .option(ChannelOption.SO_SNDBUF, config.getSoSndbuf())
                        .option(ChannelOption.SO_RCVBUF, config.getSoRcvbuf())
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                        .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(config.getWriteBufferWaterMarkLow(),
                                config.getWriteBufferWaterMarkHigh()));
            } else if (channelType == ChannelType.uds) {
                bootstrap.channel(socketChannel);
            } else {
                status = RedisConnectionStatus.INVALID;
                ErrorLogCollector.collect(RedisConnection.class, "illegal channelType = " + channelType);
                return;
            }
            bootstrap.handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) {
                    ChannelPipeline pipeline = channel.pipeline();
                    if (config.getProxyUpstreamTlsProvider() != null && config.getResource() != null) {
                        pipeline.addLast(config.getProxyUpstreamTlsProvider().createSslHandler(config.getResource()));
                    }
                    pipeline.addLast(new ReplyDecoder());
                    pipeline.addLast(new ReplyAggregateDecoder());
                    pipeline.addLast(new ReplyHandler(queue, connectionName, config.isTcpQuickAck()));
                    pipeline.addLast(new CommandPackEncoder(RedisConnection.this, commandPackRecycler, queue));
                }
            });

            if (config.isTcpQuickAck() && channelType == ChannelType.tcp) {
                bootstrap.option(EpollChannelOption.TCP_QUICKACK, Boolean.TRUE);
            }
            if (logger.isInfoEnabled()) {
                logger.info("{} try connect...", connectionName);
            }
            ChannelFuture future;
            if (host != null && port > 0) {
                future = bootstrap.connect(host, port);
            } else {
                future = bootstrap.connect(new DomainSocketAddress(udsPath));
            }
            this.channel = future.channel();
            this.channel.closeFuture().addListener(f -> {
                Throwable cause = f.cause();
                if (cause != null) {
                    logger.warn("{} disconnect", connectionName, cause);
                } else {
                    logger.warn("{} disconnect", connectionName);
                }
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
                    try {
                        if (config.getUpstreamClient() != null) {
                            config.getUpstreamClient().renew();
                        }
                    } catch (Exception e) {
                        logger.error("upstream client renew fail by {}", connectionName, e);
                    }
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
        if (stop.get()) {
            return;
        }
        if (idleCheckTask != null && closeIdleConnection) {
            return;
        }
        synchronized (lock) {
            if (idleCheckTask == null) {
                lastCommandTime = TimeCache.currentMillis;
                closeIdleConnection = true;
                idleCheckTask = idleCheckScheduled.scheduleAtFixedRate(this::checkIdle,
                        checkIdleThresholdSeconds, checkIdleThresholdSeconds, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * 关闭空闲检测
     */
    public void stopIdleCheck() {
        if (idleCheckTask == null && !closeIdleConnection) {
            return;
        }
        synchronized (lock) {
            lastCommandTime = TimeCache.currentMillis;
            closeIdleConnection = false;
            stopIdleCheckTask();
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
     * 清空queue
     */
    public void clearQueue() {
        queue.clear();
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
        if (status == RedisConnectionStatus.INVALID) {
            String log = connectionName + " is " + status + ", command return NOT_AVAILABLE";
            for (CompletableFuture<Reply> future : completableFutureList) {
                future.complete(ErrorReply.UPSTREAM_CONNECTION_STATUS_INVALID);
                ErrorLogCollector.collect(RedisConnection.class, log);
            }
            return;
        }
        CommandPack pack = commandPackRecycler.newInstance(commands, completableFutureList, time());
        if (logger.isDebugEnabled()) {
            List<String> commandNames = new ArrayList<>();
            for (Command command : commands) {
                commandNames.add(command.getName());
            }
            logger.debug("{} receive commands, commands.size = {}, commands = {}", connectionName, commands.size(), commandNames);
        }
        if (status == RedisConnectionStatus.VALID || status == RedisConnectionStatus.CLOSING) {
            channel.writeAndFlush(pack);
        } else if (status == RedisConnectionStatus.INITIALIZE) {
            synchronized (cachedCommands) {
                if (status == RedisConnectionStatus.VALID || status == RedisConnectionStatus.CLOSING) {
                    channel.writeAndFlush(pack);
                } else if (status == RedisConnectionStatus.INITIALIZE) {
                    boolean success = cachedCommands.offer(pack);
                    if (!success) {
                        String log = connectionName + ", cachedCommands queue is full, command return NOT_AVAILABLE";
                        for (CompletableFuture<Reply> future : completableFutureList) {
                            future.complete(ErrorReply.UPSTREAM_CONNECTION_CACHED_QUEUE_FULL);
                            ErrorLogCollector.collect(RedisConnection.class, log);
                        }
                    }
                } else {
                    String log = connectionName + " is " + status + ", command return NOT_AVAILABLE";
                    for (CompletableFuture<Reply> future : completableFutureList) {
                        future.complete(ErrorReply.UPSTREAM_CONNECTION_STATUS_INVALID);
                        ErrorLogCollector.collect(RedisConnection.class, log);
                    }
                }
            }
        } else {
            String log = connectionName + " is " + status + ", command return NOT_AVAILABLE";
            for (CompletableFuture<Reply> future : completableFutureList) {
                future.complete(ErrorReply.UPSTREAM_CONNECTION_STATUS_INVALID);
                ErrorLogCollector.collect(RedisConnection.class, log);
            }
        }
        if (closeIdleConnection) {
            lastCommandTime = TimeCache.currentMillis;
        }
    }

    public String getUrl() {
        return addr.getUrl();
    }

    //初始化
    private void initialize() {
        try {
            //1、auth
            auth();
            //2、ping
            ping(connectTimeoutMillis, true);
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
            int size = cachedCommands.size();
            if (logger.isWarnEnabled() && size > 0) {
                logger.warn("{} flushCachedCommands after initialize, size = {}", connectionName, size);
            }
            flushCachedCommands();
        } catch (Exception e) {
            config.getFastFailStats().incrFail(addr.getUrl());
            status = RedisConnectionStatus.INVALID;
            logger.error("{} initialize fail", connectionName, e);
            stop();
            try {
                if (config.getUpstreamClient() != null) {
                    config.getUpstreamClient().renew();
                }
            } catch (Exception ex) {
                logger.error("upstream client renew fail by {}", connectionName, ex);
            }
        }
    }

    //发送心跳
    private void heartbeat() {
        synchronized (lock) {
            try {
                if (stop.get()) {
                    stopHeartbeatTask();
                    return;
                }
                if (status != RedisConnectionStatus.VALID) {
                    return;
                }
                ping(heartbeatTimeoutMillis, false);
            } catch (Exception e) {
                logger.error("{} heartbeat error", connectionName, e);
                stop();
            }
        }
    }

    //发送ping，并检测回包
    private void ping(long timeoutMillis, boolean logEnable) {
        if (logEnable && logger.isInfoEnabled()) {
            if (logger.isInfoEnabled()) {
                logger.info("{} send `PING` command", connectionName);
            }
        }
        CompletableFuture<Reply> future = sendPing();
        try {
            Reply reply = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            String resp = Utils.checkPingReply(reply);
            if (resp != null) {
                if (logEnable && logger.isInfoEnabled()) {
                    logger.info("{} send `PING` command success, reply = {}", connectionName, resp);
                }
                return;
            }
            logger.error("{} send `PING` command fail, reply = {}", connectionName, reply);
            throw new CamelliaRedisException("ping fail");
        } catch (CamelliaRedisException e) {
            throw e;
        } catch (Exception e) {
            logger.error("{} send `PING` command timeout, timeoutMillis = {}", connectionName, timeoutMillis, e);
            throw new CamelliaRedisException(e);
        }
    }

    //关闭（是否优雅的）
    private void _stop(boolean grace) {
        RedisConnectionMonitor.removeRedisConnection(this);
        if (!stop.compareAndSet(false, true)) {
            status = RedisConnectionStatus.INVALID;
            stopHeartbeatTask();
            stopIdleCheckTask();
            return;
        }
        if (status == RedisConnectionStatus.INVALID && channel == null
                && heartbeatTask == null && idleCheckTask == null && queue.isEmpty() && cachedCommands.isEmpty()) {
            ErrorLogCollector.collect(RedisConnection.class, addr.getUrl() + " stop, grace = " + grace);
            return;
        }
        if (logger.isInfoEnabled()) {
            if (grace) {
                logger.info("{} stopping, grace = {}", connectionName, true);
            } else {
                logger.warn("{} stopping, grace = {}", connectionName, false);
            }
        }
        try {
            status = RedisConnectionStatus.INVALID;
            closeChannel();
            stopHeartbeatTask();
            stopIdleCheckTask();
            int count1 = 0;
            while (!queue.isEmpty()) {
                CompletableFuture<Reply> future = queue.poll();
                if (future != null) {
                    future.complete(ErrorReply.UPSTREAM_CONNECTION_NOT_AVAILABLE);
                    count1 ++;
                }
            }
            int count2 = 0;
            while (!cachedCommands.isEmpty()) {
                CommandPack commandPack = cachedCommands.poll();
                if (commandPack != null) {
                    for (CompletableFuture<Reply> future : commandPack.getCompletableFutureList()) {
                        future.complete(ErrorReply.UPSTREAM_CONNECTION_NOT_AVAILABLE);
                        count2 ++;
                    }
                }
            }
            if ((count1 > 0 || count2 > 0) && !grace) {
                logger.error("{} stopped, pendingCommands = {}, cachedCommands = {} return NOT_AVAILABLE", connectionName, count1, count2);
            }
        } catch (Exception e) {
            logger.error("{} stop error", connectionName, e);
        }
    }

    private void closeChannel() {
        try {
            if (channel != null) {
                channel.close();
                channel = null;
            }
        } catch (Exception e) {
            logger.error("{}, channel close error", connectionName, e);
        }
    }

    private void stopHeartbeatTask() {
        try {
            synchronized (lock) {
                if (heartbeatTask != null) {
                    heartbeatTask.cancel();
                    heartbeatTask = null;
                }
            }
        } catch (Exception e) {
            logger.error("{}, heart-beat schedule cancel error", connectionName, e);
        }
    }

    private void stopIdleCheckTask() {
        try {
            synchronized (lock) {
                if (idleCheckTask != null) {
                    idleCheckTask.cancel();
                    idleCheckTask = null;
                }
            }
        } catch (Exception e) {
            logger.error("{}, idle-check schedule cancel error", connectionName, e);
        }
    }

    //发送PING命令
    private CompletableFuture<Reply> sendPing() {
        Command command = new Command(new byte[][]{RedisCommand.PING.raw()});
        CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
        sendCommandDirect(command, completableFuture);
        return completableFuture;
    }

    //发送SELECT命令
    private void selectDB() {
        if (config.getDb() == 0) return;
        try {
            Command command = new Command(new byte[][]{RedisCommand.SELECT.raw(), Utils.stringToBytes(String.valueOf(config.getDb()))});
            CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
            if (logger.isInfoEnabled()) {
                logger.info("{} send `SELECT {}` command", connectionName, config.getDb());
            }
            sendCommandDirect(command, completableFuture);
            Reply reply = completableFuture.get(connectTimeoutMillis, TimeUnit.MILLISECONDS);
            if (reply instanceof StatusReply) {
                if (((StatusReply) reply).getStatus().equalsIgnoreCase(StatusReply.OK.getStatus())) {
                    if (logger.isInfoEnabled()) {
                        logger.info("{} `SELECT {}` success, reply = {}", connectionName, db, ((StatusReply) reply).getStatus());
                    }
                    return;
                } else {
                    logger.error("{} send `SELECT {}` fail, reply = {}", connectionName, db, ((StatusReply) reply).getStatus());
                    throw new CamelliaRedisException("select db fail");
                }
            } else if (reply instanceof ErrorReply) {
                logger.error("{} send `SELECT {}` error, reply = {}", connectionName, db, ((ErrorReply) reply).getError());
                throw new CamelliaRedisException("select db fail");
            }
            logger.error("{} send `SELECT {}` error, reply = {}", connectionName, db, reply);
            throw new CamelliaRedisException("select db fail");
        } catch (CamelliaRedisException e) {
            throw e;
        } catch (Exception e) {
            logger.error("{} send `SELECT {}` timeout, timeoutMillis = {}", connectionName, db, connectTimeoutMillis);
            throw new CamelliaRedisException("select db timeout", e);
        }
    }

    //发送READONLY命令
    private void sendReadonlyCommand() {
        try {
            if (!addr.isReadonly()) return;
            Command command = new Command(new byte[][]{RedisCommand.READONLY.raw()});
            CompletableFuture<Reply> completableFuture = new CompletableFuture<>();
            if (logger.isInfoEnabled()) {
                logger.info("{} send `READONLY` command", connectionName);
            }
            sendCommandDirect(command, completableFuture);
            Reply reply = completableFuture.get(connectTimeoutMillis, TimeUnit.MILLISECONDS);
            if (reply instanceof StatusReply) {
                if (((StatusReply) reply).getStatus().equalsIgnoreCase(StatusReply.OK.getStatus())) {
                    if (logger.isInfoEnabled()) {
                        logger.info("{} send `READONLY` command success, reply = {}", connectionName, ((StatusReply) reply).getStatus());
                    }
                    return;
                } else {
                    logger.error("{} send `READONLY` command fail, reply = {}", connectionName, ((StatusReply) reply).getStatus());
                    throw new CamelliaRedisException("send readonly command fail");
                }
            } else if (reply instanceof ErrorReply) {
                logger.error("{} send `READONLY` command error, reply = {}", connectionName, ((ErrorReply) reply).getError());
                throw new CamelliaRedisException("send readonly command error");
            }
            logger.error("{} send `READONLY` command error, reply = {}", connectionName, reply);
            throw new CamelliaRedisException("send readonly command error");
        } catch (CamelliaRedisException e) {
            throw e;
        } catch (Exception e) {
            logger.error("{} send `READONLY` command timeout, timeoutMillis = {}", connectionName, connectTimeoutMillis);
            throw new CamelliaRedisException("send readonly command timeout", e);
        }
    }

    //直接发送命令，不检查连接状态
    private void sendCommandDirect(Command command, CompletableFuture<Reply> future) {
        CommandPack pack = commandPackRecycler.newInstance(Collections.singletonList(command), Collections.singletonList(future), time());
        channel.writeAndFlush(pack);
    }

    //发送AUTH命令，并检查回包
    private void auth() {
        if (password != null) {
            logger.info("{} need password, send `AUTH` command", connectionName);
            CompletableFuture<Reply> future = new CompletableFuture<>();
            if (userName == null) {
                sendCommandDirect(new Command(new byte[][]{RedisCommand.AUTH.raw(), Utils.stringToBytes(password)}), future);
            } else {
                sendCommandDirect(new Command(new byte[][]{RedisCommand.AUTH.raw(), Utils.stringToBytes(userName), Utils.stringToBytes(password)}), future);
            }
            Reply reply;
            try {
                reply = future.get(connectTimeoutMillis, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logger.error("{} send `AUTH` command timeout, timeoutMillis = {}", connectionName, connectTimeoutMillis);
                throw new CamelliaRedisException("auth fail");
            }
            if (reply instanceof StatusReply) {
                if (((StatusReply) reply).getStatus().equalsIgnoreCase(StatusReply.OK.getStatus())) {
                    if (logger.isInfoEnabled()) {
                        logger.info("{} send `AUTH` command success", connectionName);
                    }
                    return;
                }
            } else if (reply instanceof ErrorReply) {
                logger.error("{} send `AUTH` command error, reply = {}", connectionName, ((ErrorReply) reply).getError());
                throw new CamelliaRedisException("auth fail");
            }
            logger.error("{} send `AUTH` command fail, reply = {}", connectionName, reply);
            throw new CamelliaRedisException("auth fail");
        }
    }

    //开启定时检测和定时心跳
    private void startSchedule() {
        if (heartbeatIntervalSeconds > 0 && heartbeatTimeoutMillis > 0) {
            synchronized (lock) {
                if (heartbeatTask == null) {
                    //默认60s发送一个心跳，心跳超时时间10s，如果超时了，则关闭当前连接
                    heartbeatTask = heartBeatScheduled.scheduleAtFixedRate(this::heartbeat,
                            heartbeatIntervalSeconds, heartbeatIntervalSeconds, TimeUnit.SECONDS);
                }
            }
        }
        if (closeIdleConnection && checkIdleThresholdSeconds > 0 && closeIdleConnectionDelaySeconds > 0) {
            synchronized (lock) {
                if (idleCheckTask == null) {
                    idleCheckTask = idleCheckScheduled.scheduleAtFixedRate(this::checkIdle,
                            checkIdleThresholdSeconds, checkIdleThresholdSeconds, TimeUnit.SECONDS);
                }
            }
        }
    }

    //刷新缓存的命令
    private void flushCachedCommands() {
        while (!cachedCommands.isEmpty()) {
            CommandPack commandPack = cachedCommands.poll();
            if (commandPack == null) break;
            channel.writeAndFlush(commandPack);
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
        synchronized (lock) {
            try {
                if (stop.get()) {
                    stopIdleCheckTask();
                    return;
                }
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

    //获取当前时间（ns）
    private long time() {
        if (ProxyMonitorCollector.isUpstreamRedisSpendTimeMonitorEnable() && !config.isSkipCommandSpendTimeMonitor()) {
            return System.nanoTime();
        } else {
            return -1;
        }
    }


}