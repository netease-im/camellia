package com.netease.nim.camellia.redis.proxy.upstream.connection;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.proxy.conf.EventLoopGroupResult;
import com.netease.nim.camellia.redis.proxy.conf.NettyConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.netty.ChannelType;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.tls.upstream.ProxyUpstreamTlsProvider;
import com.netease.nim.camellia.redis.proxy.tls.upstream.TlsEnableCache;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.tools.utils.LockMap;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.conf.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.util.*;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.util.concurrent.FastThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    private EventLoopGroup tcpEventLoopGroup = null;
    private EventLoopGroup udsEventLoopGroup = null;

    private final ConcurrentHashMap<EventLoop, ConcurrentHashMap<String, RedisConnection>> eventLoopMap = new ConcurrentHashMap<>();

    private final FastThreadLocal<EventLoop> eventLoopThreadLocal = new FastThreadLocal<>();

    private FastFailStats fastFailStats;

    private int heartbeatIntervalSeconds = Constants.Upstream.heartbeatIntervalSeconds;
    private long heartbeatTimeoutMillis = Constants.Upstream.heartbeatTimeoutMillis;
    private int connectTimeoutMillis = Constants.Upstream.connectTimeoutMillis;

    private boolean closeIdleConnection = Constants.Upstream.closeIdleConnection;
    private long checkIdleConnectionThresholdSeconds = Constants.Upstream.checkIdleConnectionThresholdSeconds;
    private int closeIdleConnectionDelaySeconds = Constants.Upstream.closeIdleConnectionDelaySeconds;

    private final ConcurrentHashMap<Object, LockMap> lockMapMap = new ConcurrentHashMap<>();

    private ProxyUpstreamTlsProvider tlsProvider;
    private UpstreamAddrConverter upstreamAddrConverter;

    public static RedisConnectionHub instance = new RedisConnectionHub();
    private RedisConnectionHub() {
    }
    public static RedisConnectionHub getInstance() {
        return instance;
    }

    public void init() {
        if (!init.compareAndSet(false, true)) {
            logger.warn("RedisConnectionHub duplicate init");
            return;
        }

        this.tcpEventLoopGroup = NettyConf.upstreamEventLoopGroup(NettyConf.Type.tcp_upstream);
        this.udsEventLoopGroup = NettyConf.upstreamEventLoopGroup(NettyConf.Type.uds_upstream);

        this.connectTimeoutMillis = ProxyDynamicConf.getInt("upstream.connect.timeout.millis", Constants.Upstream.connectTimeoutMillis);
        this.heartbeatIntervalSeconds = ProxyDynamicConf.getInt("upstream.heartbeat.interval.seconds", Constants.Upstream.heartbeatIntervalSeconds);
        this.heartbeatTimeoutMillis = ProxyDynamicConf.getLong("upstream.heartbeat.timeout.millis", Constants.Upstream.heartbeatTimeoutMillis);
        logger.info("RedisConnectionHub, connectTimeoutMillis = {}, heartbeatIntervalSeconds = {}, heartbeatTimeoutMillis = {}",
                this.connectTimeoutMillis, this.heartbeatIntervalSeconds, this.heartbeatTimeoutMillis);

        int failCountThreshold = ProxyDynamicConf.getInt("upstream.fail.count.threshold", Constants.Upstream.failCountThreshold);
        long failBanMillis = ProxyDynamicConf.getLong("upstream.fail.ban.millis", Constants.Upstream.failBanMillis);

        fastFailStats = new FastFailStats(failCountThreshold, failBanMillis);
        logger.info("RedisConnectionHub, failCountThreshold = {}, failBanMillis = {}", fastFailStats.getFailCountThreshold(), fastFailStats.getFailBanMillis());
        this.closeIdleConnection = ProxyDynamicConf.getBoolean("upstream.close.idle.connection", Constants.Upstream.closeIdleConnection);
        this.checkIdleConnectionThresholdSeconds = ProxyDynamicConf.getLong("upstream.check.idle.connection.threshold.seconds", Constants.Upstream.checkIdleConnectionThresholdSeconds);
        this.closeIdleConnectionDelaySeconds = ProxyDynamicConf.getInt("upstream.close.idle.connection.delay.seconds", Constants.Upstream.closeIdleConnectionDelaySeconds);
        logger.info("RedisConnectionHub, closeIdleConnection = {}, checkIdleConnectionThresholdSeconds = {}, closeIdleConnectionDelaySeconds = {}",
                this.closeIdleConnection, this.checkIdleConnectionThresholdSeconds, this.closeIdleConnectionDelaySeconds);

        this.tlsProvider = ConfigInitUtil.initProxyUpstreamTlsProvider();
        if (tlsProvider != null) {
            boolean success = this.tlsProvider.init();
            logger.info("RedisConnectionHub, ProxyUpstreamTlsProvider = {}, init = {}",
                    tlsProvider.getClass().getName(), success);
        }
        this.upstreamAddrConverter = ConfigInitUtil.initUpstreamAddrConverter();
        if (upstreamAddrConverter != null) {
            logger.info("RedisConnectionHub, UpstreamAddrConverter = {}", upstreamAddrConverter.getClass().getName());
        }

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
     * @param upstreamClient upstreamClient
     * @param host host
     * @param port port
     * @param userName userName
     * @param password password
     * @return RedisConnection
     */
    public RedisConnection get(IUpstreamClient upstreamClient, String host, int port, String userName, String password) {
        RedisConnectionAddr addr = new RedisConnectionAddr(host, port, userName, password, false, 0, false);
        return get(upstreamClient, addr);
    }

    /**
     * 获取一个连接，优先使用相同eventLoop的连接，如果获取不到，则走公共连接池
     * @param addr addr
     * @return RedisConnection
     */
    public RedisConnection get(RedisConnectionAddr addr) {
        return get(null, null, addr);
    }

    /**
     * 获取一个连接，优先使用相同eventLoop的连接，如果获取不到，则走公共连接池
     * @param upstreamClient upstreamClient
     * @param addr addr
     * @return RedisConnection
     */
    public RedisConnection get(IUpstreamClient upstreamClient, RedisConnectionAddr addr) {
        Resource resource = upstreamClient == null ? null : upstreamClient.getResource();
        return get(resource, upstreamClient, addr);
    }


    /**
     * 获取一个连接，优先使用相同eventLoop的连接，如果获取不到，则走公共连接池
     * @param resource 归属的resource
     * @param upstreamClient upstreamClient
     * @param addr addr
     * @return RedisConnection
     */
    public RedisConnection get(Resource resource, IUpstreamClient upstreamClient, RedisConnectionAddr addr) {
        try {
            RedisConnection cache = addr.getCache();
            if (cache != null && cache.isValid()) {
                return cache;
            }
            String url = addr.getUrl();
            //优先使用相同eventLoop的连接
            EventLoop eventLoop = eventLoopThreadLocal.get();
            if (eventLoop != null && eventLoopMatch(addr, eventLoop)) {
                //看看是否已经初始化好了，如果是，直接返回
                ConcurrentHashMap<String, RedisConnection> map = CamelliaMapUtils.computeIfAbsent(eventLoopMap, eventLoop, k -> new ConcurrentHashMap<>());
                RedisConnection connection = map.get(url);
                if (connection != null && connection.isValid()) {
                    addr.setCache(connection);//如果是使用当前eventLoop初始化的，则可以放入快速缓存
                    return connection;
                }
                //如果没有初始化好，或者已有连接不可用了，则初始化一个
                LockMap lockMap = CamelliaMapUtils.computeIfAbsent(this.lockMapMap, eventLoop, k -> new LockMap());
                connection = initRedisConnection(resource, upstreamClient, map, lockMap, eventLoop, addr);
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
            ChannelType channelType = Utils.channelType(addr);
            //如果没有，则使用公共eventLoopGroup去初始化一个连接
            if (channelType == ChannelType.tcp) {
                eventLoop = tcpEventLoopGroup.next();
            } else if (channelType == ChannelType.uds) {
                eventLoop = udsEventLoopGroup.next();
            } else {
                return null;
            }
            LockMap lockMap = CamelliaMapUtils.computeIfAbsent(this.lockMapMap, addr.getUrl(), k -> new LockMap());
            connection = initRedisConnection(resource, upstreamClient, map, lockMap, eventLoop, addr);
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
     * @param resource resource
     * @param host host
     * @param port port
     * @param userName userName
     * @param password password
     * @return RedisConnection
     */
    public RedisConnection newConnection(Resource resource, String host, int port, String userName, String password) {
        return newConnection(resource, null, new RedisConnectionAddr(host, port, userName, password, false, 0, false));
    }

    /**
     * 新建一个连接，优先使用当前eventLoop新建连接，如果没有，则走公共eventLoopGroup新建连接
     * @param upstreamClient upstreamClient
     * @param addr addr
     * @return RedisConnection
     */
    public RedisConnection newConnection(IUpstreamClient upstreamClient, RedisConnectionAddr addr) {
        Resource resource = upstreamClient == null ? null : upstreamClient.getResource();
        return newConnection(resource, upstreamClient, addr);
    }

    /**
     * 新建一个连接，优先使用当前eventLoop新建连接，如果没有，则走公共eventLoopGroup新建连接
     * @param resource resource
     * @param upstreamClient upstreamClient
     * @param addr addr
     * @return RedisConnection
     */
    public RedisConnection newConnection(Resource resource, IUpstreamClient upstreamClient, RedisConnectionAddr addr) {
        try {
            EventLoop eventLoop = selectEventLoop(addr);
            if (eventLoop == null) {
                return null;
            }
            RedisConnection connection = initRedisConnection(eventLoop, addr, false, false, true, resource, upstreamClient);
            if (connection.isValid()) {
                return connection;
            } else {
                connection.stop();
                return null;
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisConnectionHub.class, "new RedisConnection error, addr = " + PasswordMaskUtils.maskAddr(addr), e);
            return null;
        }
    }

    /**
     * 预热一组连接
     * @param upstreamClient upstreamClient
     * @param addr addr
     * @return true/false
     */
    public boolean preheat(IUpstreamClient upstreamClient, RedisConnectionAddr addr) {
        ChannelType channelType = Utils.channelType(addr);
        EventLoopGroup workGroup;
        int workThread;
        if (channelType == ChannelType.tcp) {
            EventLoopGroupResult result = GlobalRedisProxyEnv.getTcpEventLoopGroupResult();
            if (result == null) {
                return true;
            }
            workThread = result.workThread();
            workGroup = result.workGroup();
            addr = new RedisConnectionAddr(addr.getHost(), addr.getPort(), addr.getUserName(), addr.getPassword(), false, addr.getDb(), false);
        } else if (channelType == ChannelType.uds) {
            EventLoopGroupResult result = GlobalRedisProxyEnv.getUdsEventLoopGroupResult();
            if (result == null) {
                return true;
            }
            workThread = result.workThread();
            workGroup = result.workGroup();
            addr = new RedisConnectionAddr(addr.getUdsPath(), addr.getUserName(), addr.getPassword(), false, addr.getDb(), false);
        } else {
            return false;
        }

        if (workGroup != null && workThread > 0) {
            logger.info("try preheat, addr = {}", PasswordMaskUtils.maskAddr(addr));
            for (int i = 0; i < workThread; i++) {
                EventLoop eventLoop = workGroup.next();
                updateEventLoop(eventLoop);
                RedisConnection redisConnection = get(upstreamClient, addr);
                if (redisConnection == null) {
                    logger.error("preheat fail, addr = {}", PasswordMaskUtils.maskAddr(addr));
                    throw new CamelliaRedisException("preheat fail, addr = " + PasswordMaskUtils.maskAddr(addr));
                }
                Reply reply;
                try {
                    CompletableFuture<Reply> future = redisConnection.sendCommand(RedisCommand.PING.raw());
                    int timeout = ProxyDynamicConf.getInt("preheat.ping.timeout.seconds", 10);
                    reply = future.get(timeout, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new CamelliaRedisException("preheat fail, addr = " + PasswordMaskUtils.maskAddr(addr), e);
                }
                String resp = Utils.checkPingReply(reply);
                if (resp == null) {
                    logger.error("preheat fail, addr = {}, reply = {}", PasswordMaskUtils.maskAddr(addr), reply);
                    throw new CamelliaRedisException("preheat fail, addr = " + PasswordMaskUtils.maskAddr(addr) + ", reply = " + reply);
                }
            }
            logger.info("preheat success, addr = {}", PasswordMaskUtils.maskAddr(addr));
            return true;
        }
        return false;
    }

    /**
     * getAllConnections
     * @return connections
     */
    public Set<RedisConnection> getAllConnections() {
        Set<RedisConnection> set = new HashSet<>(map.values());
        for (Map.Entry<EventLoop, ConcurrentHashMap<String, RedisConnection>> entry : eventLoopMap.entrySet()) {
            set.addAll(entry.getValue().values());
        }
        return set;
    }

    //初始化一个连接，初始化完成后会放入map，会做并发控制，map中只有一个实例
    private RedisConnection initRedisConnection(Resource resource, IUpstreamClient upstreamClient, ConcurrentHashMap<String, RedisConnection> map, LockMap lockMap,
                                                EventLoop eventLoop, RedisConnectionAddr addr) {
        String url = addr.getUrl();
        RedisConnection connection = map.get(url);
        if (connection == null || !connection.isValid()) {
            synchronized (lockMap.getLockObj(url)) {
                connection = map.get(url);
                if (connection == null || !connection.isValid()) {
                    connection = initRedisConnection(eventLoop, addr, true, true, false, resource, upstreamClient);
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
                                                boolean checkIdle, boolean skipCommandSpendTimeMonitor,
                                                Resource resource, IUpstreamClient upstreamClient) {
        RedisConnectionConfig config = new RedisConnectionConfig();
        config.setHost(addr.getHost());
        config.setPort(addr.getPort());
        config.setUdsPath(addr.getUdsPath());
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
        if (addr.getUdsPath() != null) {
            config.setTcpNoDelay(NettyConf.tcpNoDelay(NettyConf.Type.uds_upstream));
            config.setSoKeepalive(NettyConf.soKeepalive(NettyConf.Type.uds_upstream));
            config.setSoRcvbuf(NettyConf.soRcvbuf(NettyConf.Type.uds_upstream));
            config.setSoSndbuf(NettyConf.soSndbuf(NettyConf.Type.uds_upstream));
            config.setWriteBufferWaterMarkLow(NettyConf.writeBufferWaterMarkLow(NettyConf.Type.uds_upstream));
            config.setWriteBufferWaterMarkHigh(NettyConf.writeBufferWaterMarkHigh(NettyConf.Type.uds_upstream));
        } else {
            config.setTcpNoDelay(NettyConf.tcpNoDelay(NettyConf.Type.tcp_upstream));
            config.setSoKeepalive(NettyConf.soKeepalive(NettyConf.Type.tcp_upstream));
            config.setSoRcvbuf(NettyConf.soRcvbuf(NettyConf.Type.tcp_upstream));
            config.setSoSndbuf(NettyConf.soSndbuf(NettyConf.Type.tcp_upstream));
            config.setWriteBufferWaterMarkLow(NettyConf.writeBufferWaterMarkLow(NettyConf.Type.tcp_upstream));
            config.setWriteBufferWaterMarkHigh(NettyConf.writeBufferWaterMarkHigh(NettyConf.Type.tcp_upstream));
        }
        config.setFastFailStats(fastFailStats);
        if (resource != null && TlsEnableCache.tlsEnable(resource) && tlsProvider != null) {
            config.setProxyUpstreamTlsProvider(tlsProvider);
            config.setResource(resource);
        }
        config.setUpstreamClient(upstreamClient);
        config.setUpstreamHostConverter(upstreamAddrConverter);
        RedisConnection connection = new RedisConnection(config);
        connection.start();
        return connection;
    }

    private boolean eventLoopMatch(RedisConnectionAddr addr, EventLoop eventLoop) {
        ChannelType channelType = Utils.channelType(addr);
        if (channelType == ChannelType.tcp) {
            return true;
        } else if (channelType == ChannelType.uds) {
            EventLoopGroup parent = eventLoop.parent();
            if (parent instanceof MultiThreadIoEventLoopGroup ioEventLoopGroup) {
                return ioEventLoopGroup.isIoType(EpollIoHandler.class) || ioEventLoopGroup.isIoType(KQueueIoHandler.class)
                        || ioEventLoopGroup.isIoType(IoUringIoHandler.class);
            }
            return false;
        }
        return false;
    }

    private EventLoop selectEventLoop(RedisConnectionAddr addr) {
        ChannelType channelType = Utils.channelType(addr);
        EventLoop loop = eventLoopThreadLocal.get();
        if (loop == null) {
            if (channelType == ChannelType.tcp) {
                return tcpEventLoopGroup.next();
            } else if (channelType == ChannelType.uds) {
                return udsEventLoopGroup.next();
            }
        } else {
            if (channelType == ChannelType.tcp) {
                return loop;
            } else if (channelType == ChannelType.uds) {
                EventLoopGroup parent = loop.parent();
                if (parent instanceof MultiThreadIoEventLoopGroup ioEventLoopGroup) {
                    boolean match = ioEventLoopGroup.isIoType(EpollIoHandler.class) || ioEventLoopGroup.isIoType(KQueueIoHandler.class)
                            || ioEventLoopGroup.isIoType(IoUringIoHandler.class);
                    if (match) {
                        return loop;
                    }
                }
                return udsEventLoopGroup.next();
            }
        }
        return null;
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