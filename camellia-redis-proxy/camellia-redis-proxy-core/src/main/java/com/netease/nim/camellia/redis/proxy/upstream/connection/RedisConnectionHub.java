package com.netease.nim.camellia.redis.proxy.upstream.connection;


import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.netty.NettyTransportMode;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.tools.utils.LockMap;
import com.netease.nim.camellia.tools.utils.SysUtils;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.util.*;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.FastThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2019/12/18.
 */
public class RedisConnectionHub {

    private static final Logger logger = LoggerFactory.getLogger(RedisConnectionHub.class);

    private final ConcurrentHashMap<String, RedisConnection> map = new ConcurrentHashMap<>();
    private EventLoopGroup eventLoopGroup = null;
    private EventLoopGroup eventLoopGroupBackup = null;

    private final ExecutorService redisClientAsyncInitExec = new ThreadPoolExecutor(SysUtils.getCpuNum(), SysUtils.getCpuNum(), 0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(4096), new DefaultThreadFactory("camellia-redis-connection-initialize"), new ThreadPoolExecutor.AbortPolicy());

    private final ConcurrentHashMap<EventLoop, ConcurrentHashMap<String, RedisConnection>> eventLoopMap = new ConcurrentHashMap<>();

    private final FastThreadLocal<EventLoop> eventLoopThreadLocal = new FastThreadLocal<>();

    private final ConcurrentHashMap<String, AtomicLong> failCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> failTimestampMap = new ConcurrentHashMap<>();

    private int heartbeatIntervalSeconds = Constants.Transpond.heartbeatIntervalSeconds;
    private long heartbeatTimeoutMillis = Constants.Transpond.heartbeatTimeoutMillis;
    private int connectTimeoutMillis = Constants.Transpond.connectTimeoutMillis;
    private int failCountThreshold = Constants.Transpond.failCountThreshold;
    private long failBanMillis = Constants.Transpond.failBanMillis;

    private boolean soKeepalive = Constants.Transpond.soKeepalive;
    private int soSndbuf = Constants.Transpond.soSndbuf;
    private int soRcvbuf = Constants.Transpond.soRcvbuf;
    private boolean tcpNoDelay = Constants.Transpond.tcpNoDelay;
    private boolean tcpQuickAck = Constants.Transpond.tcpQuickAck;
    private int writeBufferWaterMarkLow = Constants.Transpond.writeBufferWaterMarkLow;
    private int writeBufferWaterMarkHigh = Constants.Transpond.writeBufferWaterMarkHigh;

    private boolean closeIdleConnection = Constants.Transpond.closeIdleConnection;
    private long checkIdleConnectionThresholdSeconds = Constants.Transpond.checkIdleConnectionThresholdSeconds;
    private int closeIdleConnectionDelaySeconds = Constants.Transpond.closeIdleConnectionDelaySeconds;

    private final ConcurrentHashMap<Object, LockMap> lockMapMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<EventLoop, ConcurrentHashMap<String, AtomicBoolean>> initializerStatusMap = new ConcurrentHashMap<>();

    public static RedisConnectionHub instance = new RedisConnectionHub();
    private RedisConnectionHub() {
    }
    public static RedisConnectionHub getInstance() {
        return instance;
    }

    public void init(CamelliaTranspondProperties properties) {
        CamelliaTranspondProperties.RedisConfProperties redisConf = properties.getRedisConf();
        this.connectTimeoutMillis = redisConf.getConnectTimeoutMillis();
        this.heartbeatIntervalSeconds = redisConf.getHeartbeatIntervalSeconds();
        this.heartbeatTimeoutMillis = redisConf.getHeartbeatTimeoutMillis();
        logger.info("RedisConnection, connectTimeoutMillis = {}, heartbeatIntervalSeconds = {}, heartbeatTimeoutMillis = {}",
                this.connectTimeoutMillis, this.heartbeatIntervalSeconds, this.heartbeatTimeoutMillis);
        this.failCountThreshold = redisConf.getFailCountThreshold();
        this.failBanMillis = redisConf.getFailBanMillis();
        NettyTransportMode nettyTransportMode = GlobalRedisProxyEnv.getNettyTransportMode();
        if (nettyTransportMode == NettyTransportMode.epoll) {
            this.eventLoopGroup = new EpollEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client"));
            this.eventLoopGroupBackup = new EpollEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client-backup"));
        } else if (nettyTransportMode == NettyTransportMode.kqueue) {
            this.eventLoopGroup = new KQueueEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client"));
            this.eventLoopGroupBackup = new KQueueEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client-backup"));
        } else if (nettyTransportMode == NettyTransportMode.io_uring) {
            this.eventLoopGroup = new IOUringEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client"));
            this.eventLoopGroupBackup = new IOUringEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client-backup"));
        } else {
            this.eventLoopGroup = new NioEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client"));
            this.eventLoopGroupBackup = new NioEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client-backup"));
        }
        logger.info("RedisConnection, failCountThreshold = {}, failBanMillis = {}",
                this.failCountThreshold, this.failBanMillis);
        this.closeIdleConnection = redisConf.isCloseIdleConnection();
        this.checkIdleConnectionThresholdSeconds = redisConf.getCheckIdleConnectionThresholdSeconds();
        this.closeIdleConnectionDelaySeconds = redisConf.getCloseIdleConnectionDelaySeconds();
        logger.info("RedisConnection, closeIdleConnection = {}, checkIdleConnectionThresholdSeconds = {}, closeIdleConnectionDelaySeconds = {}",
                this.closeIdleConnection, this.checkIdleConnectionThresholdSeconds, this.closeIdleConnectionDelaySeconds);

        CamelliaTranspondProperties.NettyProperties nettyProperties = properties.getNettyProperties();
        this.soKeepalive = nettyProperties.isSoKeepalive();
        this.tcpNoDelay = nettyProperties.isTcpNoDelay();
        this.soRcvbuf = nettyProperties.getSoRcvbuf();
        this.soSndbuf = nettyProperties.getSoSndbuf();
        this.writeBufferWaterMarkLow = nettyProperties.getWriteBufferWaterMarkLow();
        this.writeBufferWaterMarkHigh = nettyProperties.getWriteBufferWaterMarkHigh();
        this.tcpQuickAck = GlobalRedisProxyEnv.getNettyTransportMode() == NettyTransportMode.epoll && properties.getNettyProperties().isTcpQuickAck();
        logger.info("RedisConnection, so_keepalive = {}, tcp_no_delay = {}, tcp_quick_ack = {}, so_rcvbuf = {}, so_sndbuf = {}, write_buffer_water_mark_Low = {}, write_buffer_water_mark_high = {}",
                this.soKeepalive, this.tcpNoDelay, this.tcpQuickAck, this.soRcvbuf,
                this.soSndbuf, this.writeBufferWaterMarkLow, this.writeBufferWaterMarkHigh);

        ProxyDynamicConf.registerCallback(this::reloadConf);
        reloadConf();
    }

    public void updateEventLoop(EventLoop eventLoop) {
        eventLoopThreadLocal.set(eventLoop);
    }

    public RedisConnection tryGet(String host, int port, String userName, String password) {
        try {
            RedisConnectionAddr addr = new RedisConnectionAddr(host, port, userName, password);
            EventLoop eventLoop = eventLoopThreadLocal.get();
            if (eventLoop != null) {
                ConcurrentHashMap<String, RedisConnection> clientMap = eventLoopMap.get(eventLoop);
                if (clientMap != null) {
                    RedisConnection client = clientMap.get(addr.getUrl());
                    if (client != null && client.isValid()) {
                        return client;
                    }
                }
            }
            String url = addr.getUrl();
            RedisConnection client = map.get(url);
            if (client != null && client.isValid()) {
                return client;
            }
            return null;
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisConnectionHub.class,
                    "try get RedisConnection error, host = " + host + ",port=" + port + ",userName=" + userName + ",password=" + password, e);
            return null;
        }
    }

    public CompletableFuture<RedisConnection> newAsync(String host, int port, String userName, String password) {
        CompletableFuture<RedisConnection> future = new CompletableFuture<>();
        try {
            redisClientAsyncInitExec.submit(() -> {
                try {
                    RedisConnection redisConnection = newClient(host, port, userName, password);
                    future.complete(redisConnection);
                } catch (Exception e) {
                    ErrorLogCollector.collect(RedisConnectionHub.class,
                            "new RedisConnection async error, host = " + host + ",port=" + port + ",userName=" + userName + ",password=" + password, e);
                    future.complete(null);
                }
            });
            return future;
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisConnectionHub.class,
                    "new RedisConnection async error, host = " + host + ",port=" + port + ",userName=" + userName + ",password=" + password, e);
            future.complete(null);
            return future;
        }
    }

    public CompletableFuture<RedisConnection> getAsync(String host, int port, String userName, String password) {
        CompletableFuture<RedisConnection> future = new CompletableFuture<>();
        try {
            redisClientAsyncInitExec.submit(() -> {
                try {
                    RedisConnection redisConnection = get(host, port, userName, password);
                    future.complete(redisConnection);
                } catch (Exception e) {
                    ErrorLogCollector.collect(RedisConnectionHub.class,
                            "get RedisConnection async error, host = " + host + ",port=" + port + ",userName=" + userName + ",password=" + password, e);
                    future.complete(null);
                }
            });
            return future;
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisConnectionHub.class,
                    "get RedisConnection async error, host = " + host + ",port=" + port + ",userName=" + userName + ",password=" + password, e);
            future.complete(null);
            return future;
        }
    }

    public RedisConnection get(String host, int port, String userName, String password) {
        try {
            RedisConnectionAddr addr = new RedisConnectionAddr(host, port, userName, password);
            return get(addr);
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisConnectionHub.class,
                    "get RedisConnection error, host = " + host + ",port=" + port + ",userName=" + userName + ",password=" + password, e);
            return null;
        }
    }

    public RedisConnection newClient(String host, int port, String userName, String password) {
        try {
            return newClient(new RedisConnectionAddr(host, port, userName, password));
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisConnectionHub.class,
                    "new RedisConnection error, host = " + host + ",port=" + port + ",userName=" + userName + ",password=" + password, e);
            return null;
        }
    }

    public RedisConnection newClient(RedisConnectionAddr addr) {
        try {
            String url = addr.getUrl();
            if (fastFail(url)) {
                return null;
            }
            EventLoop loopGroup = eventLoopGroup.next();
            if (loopGroup.inEventLoop()) {
                loopGroup = eventLoopGroupBackup.next();
            }
            RedisConnectionConfig config = new RedisConnectionConfig();
            config.setHost(addr.getHost());
            config.setPort(addr.getPort());
            config.setUserName(addr.getUserName());
            config.setPassword(addr.getPassword());
            config.setReadonly(addr.isReadonly());
            config.setDb(addr.getDb());
            config.setEventLoopGroup(loopGroup);
            config.setHeartbeatTimeoutMillis(-1);
            config.setHeartbeatIntervalSeconds(-1);
            config.setConnectTimeoutMillis(connectTimeoutMillis);
            config.setCloseIdleConnection(false);
            config.setCloseIdleConnectionDelaySeconds(closeIdleConnectionDelaySeconds);
            config.setCheckIdleConnectionThresholdSeconds(checkIdleConnectionThresholdSeconds);
            config.setSkipCommandSpendTimeMonitor(true);
            config.setTcpNoDelay(tcpNoDelay);
            config.setTcpQuickAck(tcpQuickAck);
            config.setSoKeepalive(soKeepalive);
            config.setSoRcvbuf(soRcvbuf);
            config.setSoSndbuf(soSndbuf);
            config.setWriteBufferWaterMarkLow(writeBufferWaterMarkLow);
            config.setWriteBufferWaterMarkHigh(writeBufferWaterMarkHigh);
            RedisConnection client = new RedisConnection(config);
            client.start();
            if (client.isValid()) {
                resetFail(url);//如果client初始化成功，则重置计数器和错误时间戳
                return client;
            } else {
                client.stop();
                incrFail(url);
                return null;
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisConnectionHub.class, "new RedisConnection error, addr = " + addr.getUrl(), e);
            return null;
        }
    }

    public boolean preheat(String host, int port, String userName, String password) {
        return preheat(host, port, userName, password, 0);
    }

    public boolean preheat(String host, int port, String userName, String password, int db) {
        EventLoopGroup workGroup = GlobalRedisProxyEnv.getWorkGroup();
        int workThread = GlobalRedisProxyEnv.getWorkThread();
        RedisConnectionAddr addr = new RedisConnectionAddr(host, port, userName, password, db);
        if (workGroup != null && workThread > 0) {
            logger.info("try preheat, addr = {}", PasswordMaskUtils.maskAddr(addr));
            for (int i = 0; i < GlobalRedisProxyEnv.getWorkThread(); i++) {
                EventLoop eventLoop = workGroup.next();
                updateEventLoop(eventLoop);
                RedisConnection redisConnection = get(new RedisConnectionAddr(host, port, userName, password, db));
                if (redisConnection == null) {
                    logger.error("preheat fail, addr = {}", PasswordMaskUtils.maskAddr(addr));
                    throw new CamelliaRedisException("preheat fail, addr = " + PasswordMaskUtils.maskAddr(addr));
                }
            }
            logger.info("preheat success, addr = {}", PasswordMaskUtils.maskAddr(addr));
            return true;
        }
        return false;
    }

    public RedisConnection get(RedisConnectionAddr addr) {
        try {
            RedisConnection cache = addr.getCache();
            if (cache != null && cache.isValid()) {
                return cache;
            }
            EventLoop eventLoop = eventLoopThreadLocal.get();
            if (eventLoop != null) {
                RedisConnection client = tryGetRedisClient(eventLoop, addr);
                if (client != null) {
                    addr.setCache(client);
                    return client;
                }
            }
            String url = addr.getUrl();
            RedisConnection client = map.get(url);
            if (client != null && client.isValid()) {
                return client;
            }
            if (client == null || !client.isValid()) {
                eventLoop = eventLoopGroup.next();
                if (eventLoop.inEventLoop()) {
                    eventLoop = eventLoopGroupBackup.next();
                }
                LockMap lockMap = CamelliaMapUtils.computeIfAbsent(this.lockMapMap, addr.getUrl(), k -> new LockMap());
                client = tryInitRedisClient(map, lockMap, eventLoop, addr);
            }
            if (client != null && client.isValid()) {
                map.put(url, client);
                return client;
            }
            String log = "get RedisConnection fail, url = " + url;
            ErrorLogCollector.collect(RedisConnectionHub.class, log);
            return null;
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisConnectionHub.class, "get RedisConnection error, addr = " + addr.getUrl(), e);
            return null;
        }
    }

    private RedisConnection tryGetRedisClient(EventLoop eventLoop, RedisConnectionAddr addr) {
        ConcurrentHashMap<String, RedisConnection> map = CamelliaMapUtils.computeIfAbsent(eventLoopMap, eventLoop, k -> new ConcurrentHashMap<>());
        String url = addr.getUrl();
        RedisConnection client = map.get(url);
        if (client != null && client.isValid()) {
            return client;
        }
        if (eventLoop.inEventLoop()) {
            ConcurrentHashMap<String, AtomicBoolean> statusMap = CamelliaMapUtils.computeIfAbsent(initializerStatusMap, eventLoop, k -> new ConcurrentHashMap<>());
            AtomicBoolean status = CamelliaMapUtils.computeIfAbsent(statusMap, addr.getUrl(), k -> new AtomicBoolean(false));
            if (status.compareAndSet(false, true)) {
                try {
                    redisClientAsyncInitExec.submit(() -> {
                        try {
                            LockMap lockMap = CamelliaMapUtils.computeIfAbsent(this.lockMapMap, eventLoop, k -> new LockMap());
                            tryInitRedisClient(map, lockMap, eventLoop, addr);
                        } catch (Exception e) {
                            ErrorLogCollector.collect(RedisConnectionHub.class, "tryInitRedisConnection error", e);
                        } finally {
                            status.compareAndSet(true, false);
                        }
                    });
                } catch (Exception e) {
                    ErrorLogCollector.collect(RedisConnectionHub.class, "tryInitRedisConnection submit error", e);
                    status.compareAndSet(true, false);
                }
            }
            return null;
        } else {
            LockMap lockMap = CamelliaMapUtils.computeIfAbsent(this.lockMapMap, eventLoop, k -> new LockMap());
            return tryInitRedisClient(map, lockMap, eventLoop, addr);
        }
    }

    private RedisConnection tryInitRedisClient(ConcurrentHashMap<String, RedisConnection> map, LockMap lockMap, EventLoop eventLoop, RedisConnectionAddr addr) {
        String url = addr.getUrl();
        RedisConnection client = map.get(url);
        if (client == null || !client.isValid()) {
            if (fastFail(url)) {
                return null;
            }
            synchronized (lockMap.getLockObj(url)) {
                client = map.get(url);
                if (client == null || !client.isValid()) {
                    RedisConnectionConfig config = new RedisConnectionConfig();
                    config.setHost(addr.getHost());
                    config.setPort(addr.getPort());
                    config.setUserName(addr.getUserName());
                    config.setPassword(addr.getPassword());
                    config.setReadonly(addr.isReadonly());
                    config.setDb(addr.getDb());
                    config.setEventLoopGroup(eventLoop);
                    config.setHeartbeatTimeoutMillis(heartbeatTimeoutMillis);
                    config.setHeartbeatIntervalSeconds(heartbeatIntervalSeconds);
                    config.setConnectTimeoutMillis(connectTimeoutMillis);
                    config.setCloseIdleConnection(closeIdleConnection);
                    config.setCheckIdleConnectionThresholdSeconds(checkIdleConnectionThresholdSeconds);
                    config.setCloseIdleConnectionDelaySeconds(closeIdleConnectionDelaySeconds);
                    config.setTcpNoDelay(tcpNoDelay);
                    config.setTcpQuickAck(tcpQuickAck);
                    config.setSoKeepalive(soKeepalive);
                    config.setSoRcvbuf(soRcvbuf);
                    config.setSoSndbuf(soSndbuf);
                    config.setWriteBufferWaterMarkLow(writeBufferWaterMarkLow);
                    config.setWriteBufferWaterMarkHigh(writeBufferWaterMarkHigh);
                    client = new RedisConnection(config);
                    client.start();
                    if (client.isValid()) {
                        RedisConnection oldClient = map.put(url, client);
                        if (oldClient != null) {
                            oldClient.stop();
                        }
                        resetFail(url);//如果client初始化成功，则重置计数器和错误时间戳
                    } else {
                        incrFail(url);//client初始化失败，递增错误计数器
                        client.stop();
                    }
                }
            }
        }
        if (client.isValid()) {
            return client;
        }
        return null;
    }

    private void reloadConf() {
        long failBanMillis = ProxyDynamicConf.getLong("redis.connection.fail.ban.millis", this.failBanMillis);
        if (failBanMillis != this.failBanMillis) {
            logger.info("RedisConnection failBanMillis, {} -> {}", this.failBanMillis, failBanMillis);
            this.failBanMillis = failBanMillis;
        }
        int failCountThreshold = ProxyDynamicConf.getInt("redis.connection.fail.count.threshold", this.failCountThreshold);
        if (failCountThreshold != this.failCountThreshold) {
            logger.info("RedisConnection failCountThreshold, {} -> {}", this.failCountThreshold, failCountThreshold);
            this.failCountThreshold = failCountThreshold;
        }
    }

    private boolean fastFail(String url) {
        //如果client处于不可用状态，检查不可用时长
        long failTimestamp = getFailTimestamp(url);
        if (TimeCache.currentMillis - failTimestamp < failBanMillis) {
            //如果错误时间戳在禁用时间范围内，则直接返回null
            //此时去重置一下计数器，这样确保failBanMillis到期之后failCount从0开始计算
            resetFailCount(url);
            String log = "currentTimeMillis - failTimestamp < failBanMillis[" + failBanMillis + "], immediate return null, key = " + url;
            ErrorLogCollector.collect(RedisConnectionHub.class, log);
            return true;
        }
        long failCount = getFailCount(url);
        if (failCount > failCountThreshold) {
            //如果错误次数超过了阈值，则设置当前时间为错误时间戳，并重置计数器
            //接下来的failBanMillis时间内，都会直接返回null
            setFailTimestamp(url);
            resetFailCount(url);
            String log = "failCount > failCountThreshold[" + failCountThreshold + "], immediate return null, key = " + url;
            ErrorLogCollector.collect(RedisConnectionHub.class, log);
            return true;
        }
        return false;
    }

    private long getFailTimestamp(String key) {
        AtomicLong failTimestamp = CamelliaMapUtils.computeIfAbsent(failTimestampMap, key, k -> new AtomicLong(0L));
        return failTimestamp.get();
    }

    private void setFailTimestamp(String key) {
        AtomicLong failTimestamp = CamelliaMapUtils.computeIfAbsent(failTimestampMap, key, k -> new AtomicLong(0L));
        failTimestamp.set(TimeCache.currentMillis);
    }

    private void resetFailTimestamp(String key) {
        AtomicLong failCount = CamelliaMapUtils.computeIfAbsent(failTimestampMap, key, k -> new AtomicLong(0L));
        failCount.set(0L);
    }

    private void resetFailCount(String key) {
        AtomicLong failCount = CamelliaMapUtils.computeIfAbsent(failCountMap, key, k -> new AtomicLong());
        failCount.set(0L);
    }

    private long getFailCount(String key) {
        AtomicLong failCount = CamelliaMapUtils.computeIfAbsent(failCountMap, key, k -> new AtomicLong());
        return failCount.get();
    }

    private void incrFail(String key) {
        AtomicLong failCount = CamelliaMapUtils.computeIfAbsent(failCountMap, key, k -> new AtomicLong());
        failCount.incrementAndGet();
    }

    private void resetFail(String key) {
        resetFailTimestamp(key);
        resetFailCount(key);
    }
}