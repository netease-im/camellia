package com.netease.nim.camellia.redis.proxy.upstream.connection;

import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.netty.NettyTransportMode;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.tools.utils.LockMap;
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

/**
 *
 * Created by caojiajun on 2019/12/18.
 */
public class RedisConnectionHub {

    private static final Logger logger = LoggerFactory.getLogger(RedisConnectionHub.class);

    private final AtomicBoolean init = new AtomicBoolean(false);

    private final ConcurrentHashMap<String, RedisConnection> map = new ConcurrentHashMap<>();
    private EventLoopGroup eventLoopGroup = null;

    private final ConcurrentHashMap<EventLoop, ConcurrentHashMap<String, RedisConnection>> eventLoopMap = new ConcurrentHashMap<>();

    private final FastThreadLocal<EventLoop> eventLoopThreadLocal = new FastThreadLocal<>();

    private FastFailStats fastFailStats;

    private int heartbeatIntervalSeconds = Constants.Transpond.heartbeatIntervalSeconds;
    private long heartbeatTimeoutMillis = Constants.Transpond.heartbeatTimeoutMillis;
    private int connectTimeoutMillis = Constants.Transpond.connectTimeoutMillis;

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

    public static RedisConnectionHub instance = new RedisConnectionHub();
    private RedisConnectionHub() {
    }
    public static RedisConnectionHub getInstance() {
        return instance;
    }

    public void init(CamelliaTranspondProperties properties) {
        if (!init.compareAndSet(false, true)) {
            logger.warn("RedisConnectionHub duplicate init");
            return;
        }
        CamelliaTranspondProperties.RedisConfProperties redisConf = properties.getRedisConf();
        this.connectTimeoutMillis = redisConf.getConnectTimeoutMillis();
        this.heartbeatIntervalSeconds = redisConf.getHeartbeatIntervalSeconds();
        this.heartbeatTimeoutMillis = redisConf.getHeartbeatTimeoutMillis();
        logger.info("RedisConnectionHub, connectTimeoutMillis = {}, heartbeatIntervalSeconds = {}, heartbeatTimeoutMillis = {}",
                this.connectTimeoutMillis, this.heartbeatIntervalSeconds, this.heartbeatTimeoutMillis);

        NettyTransportMode nettyTransportMode = GlobalRedisProxyEnv.getNettyTransportMode();
        if (nettyTransportMode == NettyTransportMode.epoll) {
            this.eventLoopGroup = new EpollEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-connection"));
        } else if (nettyTransportMode == NettyTransportMode.kqueue) {
            this.eventLoopGroup = new KQueueEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-connection"));
        } else if (nettyTransportMode == NettyTransportMode.io_uring) {
            this.eventLoopGroup = new IOUringEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-connection"));
        } else {
            this.eventLoopGroup = new NioEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-connection"));
        }

        fastFailStats = new FastFailStats(redisConf.getFailCountThreshold(), redisConf.getFailBanMillis());
        logger.info("RedisConnectionHub, failCountThreshold = {}, failBanMillis = {}", fastFailStats.getFailCountThreshold(), fastFailStats.getFailBanMillis());
        this.closeIdleConnection = redisConf.isCloseIdleConnection();
        this.checkIdleConnectionThresholdSeconds = redisConf.getCheckIdleConnectionThresholdSeconds();
        this.closeIdleConnectionDelaySeconds = redisConf.getCloseIdleConnectionDelaySeconds();
        logger.info("RedisConnectionHub, closeIdleConnection = {}, checkIdleConnectionThresholdSeconds = {}, closeIdleConnectionDelaySeconds = {}",
                this.closeIdleConnection, this.checkIdleConnectionThresholdSeconds, this.closeIdleConnectionDelaySeconds);

        CamelliaTranspondProperties.NettyProperties nettyProperties = properties.getNettyProperties();
        this.soKeepalive = nettyProperties.isSoKeepalive();
        this.tcpNoDelay = nettyProperties.isTcpNoDelay();
        this.soRcvbuf = nettyProperties.getSoRcvbuf();
        this.soSndbuf = nettyProperties.getSoSndbuf();
        this.writeBufferWaterMarkLow = nettyProperties.getWriteBufferWaterMarkLow();
        this.writeBufferWaterMarkHigh = nettyProperties.getWriteBufferWaterMarkHigh();
        this.tcpQuickAck = GlobalRedisProxyEnv.getNettyTransportMode() == NettyTransportMode.epoll && properties.getNettyProperties().isTcpQuickAck();
        logger.info("RedisConnectionHub, so_keepalive = {}, tcp_no_delay = {}, tcp_quick_ack = {}, so_rcvbuf = {}, so_sndbuf = {}, write_buffer_water_mark_Low = {}, write_buffer_water_mark_high = {}",
                this.soKeepalive, this.tcpNoDelay, this.tcpQuickAck, this.soRcvbuf,
                this.soSndbuf, this.writeBufferWaterMarkLow, this.writeBufferWaterMarkHigh);

        ProxyDynamicConf.registerCallback(this::reloadConf);
        reloadConf();
    }

    /**
     * 设置当前线程所在的EventLoop
     * @param eventLoop EventLoop
     */
    public void updateEventLoop(EventLoop eventLoop) {
        eventLoopThreadLocal.set(eventLoop);
    }

    /**
     * 获取一个连接，优先使用相同eventLoop的连接，如果获取不到，则走公共连接池
     * @param host host
     * @param port port
     * @param userName userName
     * @param password password
     * @return RedisConnection
     */
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

    /**
     * 获取一个连接，优先使用相同eventLoop的连接，如果获取不到，则走公共连接池
     * @param addr addr
     * @return RedisConnection
     */
    public RedisConnection get(RedisConnectionAddr addr) {
        try {
            RedisConnection cache = addr.getCache();
            if (cache != null && cache.isValid()) {
                return cache;
            }
            String url = addr.getUrl();
            //优先使用相同eventLoop的连接
            EventLoop eventLoop = eventLoopThreadLocal.get();
            if (eventLoop != null) {
                //看看是否已经初始化好了，如果是，直接返回
                ConcurrentHashMap<String, RedisConnection> map = CamelliaMapUtils.computeIfAbsent(eventLoopMap, eventLoop, k -> new ConcurrentHashMap<>());
                RedisConnection connection = map.get(url);
                if (connection != null && connection.isValid()) {
                    addr.setCache(connection);//如果是使用当前eventLoop初始化的，则可以放入快速缓存
                    return connection;
                }
                //如果没有初始化好，或者已有连接不可用了，则初始化一个
                LockMap lockMap = CamelliaMapUtils.computeIfAbsent(this.lockMapMap, eventLoop, k -> new LockMap());
                connection = initRedisConnection(map, lockMap, eventLoop, addr);
                if (connection != null && connection.isValid()) {
                    addr.setCache(connection);//如果是使用当前eventLoop初始化的，则可以放入快速缓存
                    return connection;
                }
            }
            //否则看看公共连接池，如果有，直接返回
            RedisConnection connection = map.get(url);
            if (connection != null && connection.isValid()) {
                return connection;
            }
            //如果没有，则使用公共eventLoopGroup去初始化一个连接
            eventLoop = eventLoopGroup.next();
            LockMap lockMap = CamelliaMapUtils.computeIfAbsent(this.lockMapMap, addr.getUrl(), k -> new LockMap());
            connection = initRedisConnection(map, lockMap, eventLoop, addr);
            if (connection != null && connection.isValid()) {
                return connection;
            }
            String log = "get RedisConnection fail, url = " + url;
            ErrorLogCollector.collect(RedisConnectionHub.class, log);
            return null;
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisConnectionHub.class, "get RedisConnection error, addr = " + addr.getUrl(), e);
            return null;
        }
    }

    /**
     * 新建一个连接，优先使用当前eventLoop新建连接，如果没有，则走公共eventLoopGroup新建连接
     * @param host host
     * @param port port
     * @param userName userName
     * @param password password
     * @return RedisConnection
     */
    public RedisConnection newConnection(String host, int port, String userName, String password) {
        try {
            return newConnection(new RedisConnectionAddr(host, port, userName, password));
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisConnectionHub.class,
                    "new RedisConnection error, host = " + host + ",port=" + port + ",userName=" + userName + ",password=" + password, e);
            return null;
        }
    }

    /**
     * 新建一个连接，优先使用当前eventLoop新建连接，如果没有，则走公共eventLoopGroup新建连接
     * @param addr addr
     * @return RedisConnection
     */
    public RedisConnection newConnection(RedisConnectionAddr addr) {
        try {
            EventLoop eventLoop = eventLoopThreadLocal.get();
            if (eventLoop == null) {
                eventLoop = eventLoopGroup.next();
            }
            RedisConnection connection = initRedisConnection(eventLoop, addr, false, false, true);
            if (connection.isValid()) {
                return connection;
            } else {
                connection.stop();
                return null;
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisConnectionHub.class, "new RedisConnection error, addr = " + addr.getUrl(), e);
            return null;
        }
    }

    /**
     * 预热一组连接（使用主EventLoopGroup）
     * @param host host
     * @param port port
     * @param userName userName
     * @param password password
     * @return true/false
     */
    public boolean preheat(String host, int port, String userName, String password) {
        return preheat(host, port, userName, password, 0);
    }

    /**
     * 预热一组连接（使用主EventLoopGroup）
     * @param host host
     * @param port port
     * @param userName userName
     * @param password password
     * @param db db
     * @return true/false
     */
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

    //初始化一个连接，初始化完成后会放入map，会做并发控制，map中只有一个实例
    private RedisConnection initRedisConnection(ConcurrentHashMap<String, RedisConnection> map, LockMap lockMap,
                                                EventLoop eventLoop, RedisConnectionAddr addr) {
        String url = addr.getUrl();
        RedisConnection connection = map.get(url);
        if (connection == null || !connection.isValid()) {
            synchronized (lockMap.getLockObj(url)) {
                connection = map.get(url);
                if (connection == null || !connection.isValid()) {
                    connection = initRedisConnection(eventLoop, addr, true, true, false);
                    if (connection.isValid()) {
                        RedisConnection oldConnection = map.put(url, connection);
                        if (oldConnection != null) {
                            oldConnection.stop();
                        }
                    } else {
                        connection.stop();
                    }
                }
            }
        }
        if (connection.isValid()) {
            return connection;
        }
        return null;
    }

    //初始化一个连接
    private RedisConnection initRedisConnection(EventLoop eventLoop, RedisConnectionAddr addr, boolean heartbeatEnable,
                                                boolean checkIdle, boolean skipCommandSpendTimeMonitor) {
        RedisConnectionConfig config = new RedisConnectionConfig();
        config.setHost(addr.getHost());
        config.setPort(addr.getPort());
        config.setUserName(addr.getUserName());
        config.setPassword(addr.getPassword());
        config.setReadonly(addr.isReadonly());
        config.setDb(addr.getDb());
        config.setEventLoop(eventLoop);
        config.setHeartbeatTimeoutMillis(heartbeatEnable ? heartbeatTimeoutMillis : -1);
        config.setHeartbeatIntervalSeconds(heartbeatEnable ? heartbeatIntervalSeconds : -1);
        config.setConnectTimeoutMillis(connectTimeoutMillis);
        config.setCloseIdleConnection(checkIdle && closeIdleConnection);
        config.setCheckIdleConnectionThresholdSeconds(checkIdleConnectionThresholdSeconds);
        config.setCloseIdleConnectionDelaySeconds(closeIdleConnectionDelaySeconds);
        if (skipCommandSpendTimeMonitor) {
            config.setSkipCommandSpendTimeMonitor(true);
        }
        config.setTcpNoDelay(tcpNoDelay);
        config.setTcpQuickAck(tcpQuickAck);
        config.setSoKeepalive(soKeepalive);
        config.setSoRcvbuf(soRcvbuf);
        config.setSoSndbuf(soSndbuf);
        config.setWriteBufferWaterMarkLow(writeBufferWaterMarkLow);
        config.setWriteBufferWaterMarkHigh(writeBufferWaterMarkHigh);
        config.setFastFailStats(fastFailStats);
        RedisConnection connection = new RedisConnection(config);
        connection.start();
        return connection;
    }

    private void reloadConf() {
        long failBanMillis = ProxyDynamicConf.getLong("redis.connection.fail.ban.millis", fastFailStats.getFailBanMillis());
        if (failBanMillis != fastFailStats.getFailBanMillis()) {
            logger.info("RedisConnectionHub failBanMillis, {} -> {}", fastFailStats.getFailBanMillis(), failBanMillis);
            fastFailStats.setFailBanMillis(failBanMillis);
        }
        int failCountThreshold = ProxyDynamicConf.getInt("redis.connection.fail.count.threshold", fastFailStats.getFailCountThreshold());
        if (failCountThreshold != fastFailStats.getFailCountThreshold()) {
            logger.info("RedisConnectionHub failCountThreshold, {} -> {}", fastFailStats.getFailCountThreshold(), failCountThreshold);
            fastFailStats.setFailCountThreshold(failCountThreshold);
        }
    }
}