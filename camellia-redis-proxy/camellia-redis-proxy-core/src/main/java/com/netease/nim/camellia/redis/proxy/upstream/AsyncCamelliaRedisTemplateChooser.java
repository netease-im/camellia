package com.netease.nim.camellia.redis.proxy.upstream;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiUtil;
import com.netease.nim.camellia.core.api.ResourceTableRemoveCallback;
import com.netease.nim.camellia.core.api.ResourceTableUpdateCallback;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.env.ShardingFunc;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.redis.proxy.netty.NettyTransportMode;
import com.netease.nim.camellia.redis.proxy.plugin.DefaultBeanFactory;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.tools.utils.SysUtils;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyBeanFactory;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientHub;
import com.netease.nim.camellia.redis.proxy.route.ProxyRouteConfUpdater;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.tools.utils.LockMap;
import com.netease.nim.camellia.redis.proxy.monitor.ChannelMonitor;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;
import com.netease.nim.camellia.redis.proxy.util.ConfigInitUtil;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class AsyncCamelliaRedisTemplateChooser {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCamelliaRedisTemplateChooser.class);

    private final CamelliaTranspondProperties properties;
    private AsyncCamelliaRedisEnv env;
    private CamelliaApi apiService;
    private ProxyRouteConfUpdater updater;

    private boolean multiTenancySupport = true;//是否支持多租户
    private final ProxyBeanFactory proxyBeanFactory;

    private AsyncCamelliaRedisTemplate remoteInstance;
    private AsyncCamelliaRedisTemplate localInstance;
    private AsyncCamelliaRedisTemplate customInstance;
    private final ConcurrentHashMap<String, AsyncCamelliaRedisTemplate> remoteInstanceMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AsyncCamelliaRedisTemplate> customInstanceMap = new ConcurrentHashMap<>();

    public AsyncCamelliaRedisTemplateChooser(CamelliaTranspondProperties properties, ProxyBeanFactory proxyBeanFactory) {
        this.properties = properties;
        this.proxyBeanFactory = proxyBeanFactory != null ? proxyBeanFactory : DefaultBeanFactory.INSTANCE;
        init();
    }

    public AsyncCamelliaRedisTemplate choose(Long bid, String bgroup) {
        try {
            if (!multiTenancySupport) {
                CamelliaTranspondProperties.Type type = properties.getType();
                if (type == CamelliaTranspondProperties.Type.LOCAL) {
                    return localInstance;
                } else if (type == CamelliaTranspondProperties.Type.REMOTE) {
                    return remoteInstance;
                } else if (type == CamelliaTranspondProperties.Type.CUSTOM) {
                    return customInstance;
                } else {
                    return null;
                }
            }
            if (bid == null || bid <= 0 || bgroup == null) {
                CamelliaTranspondProperties.Type type = properties.getType();
                if (type == CamelliaTranspondProperties.Type.REMOTE) {
                    return remoteInstance;
                } else if (type == CamelliaTranspondProperties.Type.CUSTOM) {
                    return customInstance;
                }
            }
            CompletableFuture<AsyncCamelliaRedisTemplate> future = chooseAsync(bid, bgroup);
            if (future == null) return null;
            return future.get();
        } catch (Exception e) {
            ErrorLogCollector.collect(AsyncCamelliaRedisTemplateChooser.class, "choose AsyncCamelliaRedisTemplate error", e);
            return null;
        }
    }

    public CompletableFuture<AsyncCamelliaRedisTemplate> chooseAsync(Long bid, String bgroup) {
        CamelliaTranspondProperties.Type type = properties.getType();
        if (type == CamelliaTranspondProperties.Type.LOCAL) {
            return wrapper(localInstance);
        } else if (type == CamelliaTranspondProperties.Type.REMOTE) {
            CamelliaTranspondProperties.RemoteProperties remote = properties.getRemote();
            if (!remote.isDynamic()) {
                return wrapper(remoteInstance);
            }
            if (bid == null || bid <= 0 || bgroup == null) {
                return wrapper(remoteInstance);
            }
            return initAsync(bid, bgroup, remoteInstanceMap, this::initRemoteInstance);
        } else if (type == CamelliaTranspondProperties.Type.CUSTOM) {
            CamelliaTranspondProperties.CustomProperties custom = properties.getCustom();
            if (!custom.isDynamic()) {
                return wrapper(customInstance);
            }
            if (bid == null || bid <= 0 || bgroup == null) {
                return wrapper(customInstance);
            }
            return initAsync(bid, bgroup, customInstanceMap, this::initCustomInstance);
        }
        return null;
    }

    public boolean isMultiTenancySupport() {
        return multiTenancySupport;
    }

    private CompletableFuture<AsyncCamelliaRedisTemplate> wrapper(AsyncCamelliaRedisTemplate template) {
        CompletableFuture<AsyncCamelliaRedisTemplate> future = new CompletableFuture<>();
        future.complete(template);
        return future;
    }

    private final LockMap lockMap = new LockMap();
    private final ConcurrentHashMap<String, AtomicBoolean> initTagMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LinkedBlockingQueue<CompletableFuture<AsyncCamelliaRedisTemplate>>> futureQueueMap = new ConcurrentHashMap<>();
    private final ExecutorService exec = new ThreadPoolExecutor(SysUtils.getCpuNum(), SysUtils.getCpuNum(), 0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10000), new DefaultThreadFactory("async-redis-template-init"));

    private CompletableFuture<AsyncCamelliaRedisTemplate> initAsync(long bid, String bgroup,
                                                                    ConcurrentHashMap<String, AsyncCamelliaRedisTemplate> map,
                                                                    AsyncCamelliaRedisTemplateInitializer initializer) {
        String key = bid + "|" + bgroup;
        AsyncCamelliaRedisTemplate template = map.get(key);
        if (template != null) {
            AtomicBoolean initTag = _getInitTag(key);
            if (initTag.get()) {
                synchronized (lockMap.getLockObj(key)) {
                    if (initTag.get()) {
                        CompletableFuture<AsyncCamelliaRedisTemplate> future = new CompletableFuture<>();
                        boolean offer = _getFutureQueue(key).offer(future);
                        if (!offer) {
                            future.complete(null);
                            ErrorLogCollector.collect(AsyncCamelliaRedisTemplateChooser.class, "init AsyncCamelliaRedisTemplate async fail, queue full");
                        }
                        return future;
                    } else {
                        return wrapper(template);
                    }
                }
            } else {
                return wrapper(template);
            }
        }
        CompletableFuture<AsyncCamelliaRedisTemplate> future = new CompletableFuture<>();
        AtomicBoolean initTag = _getInitTag(key);
        if (initTag.compareAndSet(false, true)) {
            try {
                exec.submit(() -> {
                    AsyncCamelliaRedisTemplate redisTemplate = null;
                    try {
                        redisTemplate = initializer.init(bid, bgroup);
                        map.put(key, redisTemplate);
                        future.complete(redisTemplate);
                    } catch (Exception e) {
                        ErrorLogCollector.collect(AsyncCamelliaRedisTemplateChooser.class, "init AsyncCamelliaRedisTemplate error", e);
                        future.complete(null);
                    } finally {
                        clearFutureQueue(key, initTag, redisTemplate);
                    }
                });
            } catch (Exception e) {
                ErrorLogCollector.collect(AsyncCamelliaRedisTemplateChooser.class, "submit AsyncCamelliaRedisTemplate init task error", e);
                clearFutureQueue(key, initTag, null);
            }
        } else {
            LinkedBlockingQueue<CompletableFuture<AsyncCamelliaRedisTemplate>> queue = CamelliaMapUtils.computeIfAbsent(futureQueueMap, key,
                    k -> new LinkedBlockingQueue<>(1000000));
            boolean offer = queue.offer(future);
            if (!offer) {
                future.complete(null);
                ErrorLogCollector.collect(AsyncCamelliaRedisTemplateChooser.class, "init AsyncCamelliaRedisTemplate async fail, queue full");
            }
        }
        return future;
    }

    private void clearFutureQueue(String key, AtomicBoolean initTag, AsyncCamelliaRedisTemplate template) {
        synchronized (lockMap.getLockObj(key)) {
            LinkedBlockingQueue<CompletableFuture<AsyncCamelliaRedisTemplate>> queue = _getFutureQueue(key);
            CompletableFuture<AsyncCamelliaRedisTemplate> completableFuture = queue.poll();
            while (completableFuture != null) {
                try {
                    completableFuture.complete(template);
                } catch (Exception e) {
                    ErrorLogCollector.collect(AsyncCamelliaRedisTemplateChooser.class, "future complete error", e);
                }
                completableFuture = queue.poll();
            }
            initTag.compareAndSet(true, false);
        }
    }

    private AtomicBoolean _getInitTag(String key) {
        return CamelliaMapUtils.computeIfAbsent(initTagMap, key, k -> new AtomicBoolean(false));
    }

    private LinkedBlockingQueue<CompletableFuture<AsyncCamelliaRedisTemplate>> _getFutureQueue(String key) {
        return CamelliaMapUtils.computeIfAbsent(futureQueueMap, key, k -> new LinkedBlockingQueue<>(1000000));
    }

    private interface AsyncCamelliaRedisTemplateInitializer {
        AsyncCamelliaRedisTemplate init(long bid, String bgroup);
    }

    private void init() {
        CamelliaTranspondProperties.Type type = properties.getType();
        if (type == null) {
            throw new IllegalArgumentException();
        }
        initEnv();
        logger.info("CamelliaRedisProxy init, type = {}", type);
        if (type == CamelliaTranspondProperties.Type.LOCAL) {
            initLocal();
        } else if (type == CamelliaTranspondProperties.Type.REMOTE) {
            initRemote();
        } else if (type == CamelliaTranspondProperties.Type.CUSTOM) {
            initCustom();
        }
        if (properties.getRedisConf().isPreheat()) {
            if (localInstance != null) {
                localInstance.preheat();
            }
            if (remoteInstance != null) {
                remoteInstance.preheat();
            }
            if (customInstance != null) {
                customInstance.preheat();
            }
        }
    }

    private void initLocal() {
        CamelliaTranspondProperties.LocalProperties local = properties.getLocal();
        if (local == null) {
            throw new IllegalArgumentException("local is null");
        }
        ResourceTable resourceTable = local.getResourceTable();
        if (resourceTable != null) {
            localInstance = new AsyncCamelliaRedisTemplate(env, resourceTable);
        } else {
            String resourceTableFilePath = local.getResourceTableFilePath();
            if (resourceTableFilePath != null) {
                localInstance = new AsyncCamelliaRedisTemplate(env, resourceTableFilePath, local.getCheckIntervalMillis());
            }
        }
        if (localInstance == null) {
            throw new IllegalArgumentException("local.resourceTable/local.resourceTableFilePath is null");
        }
        multiTenancySupport = false;
    }

    private void initRemote() {
        CamelliaTranspondProperties.RemoteProperties remote = properties.getRemote();
        if (remote == null) {
            throw new IllegalArgumentException("remote is null");
        }
        String url = remote.getUrl();
        if (url == null) {
            throw new IllegalArgumentException("remote.url is null");
        }

        apiService = CamelliaApiUtil.init(url, remote.getConnectTimeoutMillis(), remote.getReadTimeoutMillis(), remote.getHeaderMap());
        logger.info("ApiService init, url = {}", url);
        boolean dynamic = remote.isDynamic();
        logger.info("Remote dynamic = {}", dynamic);
        if (remote.getBid() > 0 && remote.getBgroup() != null) {
            remoteInstance = initRemoteInstance(remote.getBid(), remote.getBgroup());
            remoteInstanceMap.put(remote.getBid() + "|" + remote.getBgroup(), remoteInstance);
        }
        multiTenancySupport = remote.isDynamic();
    }

    private void initCustom() {
        CamelliaTranspondProperties.CustomProperties custom = properties.getCustom();
        if (custom == null) {
            throw new IllegalArgumentException("custom is null");
        }
        String className = custom.getProxyRouteConfUpdaterClassName();
        ProxyRouteConfUpdater proxyRouteConfUpdater = (ProxyRouteConfUpdater) proxyBeanFactory.getBean(BeanInitUtils.parseClass(className));
        if (proxyRouteConfUpdater == null) {
            throw new IllegalArgumentException("proxyRouteConfUpdater is null");
        }
        this.updater = proxyRouteConfUpdater;
        boolean dynamic = custom.isDynamic();
        logger.info("Custom dynamic = {}", dynamic);
        if (custom.getBid() > 0 && custom.getBgroup() != null) {
            customInstance = initCustomInstance(custom.getBid(), custom.getBgroup());
            customInstanceMap.put(custom.getBid() + "|" + custom.getBgroup(), customInstance);
        }
        multiTenancySupport = custom.isDynamic();
    }

    private AsyncCamelliaRedisTemplate initCustomInstance(long bid, String bgroup) {
        CamelliaTranspondProperties.CustomProperties custom = properties.getCustom();
        if (custom == null) return null;
        AsyncCamelliaRedisTemplate template = new AsyncCamelliaRedisTemplate(env, bid, bgroup, updater, custom.getReloadIntervalMillis());
        //更新的callback
        ResourceTableUpdateCallback updateCallback = template::update;
        //删除的callback
        ResourceTableRemoveCallback removeCallback = () -> {
            customInstanceMap.remove(bid + "|" + bgroup);
            template.shutdown();
            Set<ChannelInfo> channelMap = ChannelMonitor.getChannelMap(bid, bgroup);
            int size = channelMap.size();
            for (ChannelInfo channelInfo : channelMap) {
                try {
                    channelInfo.getCtx().close();
                } catch (Exception e) {
                    logger.error("force close client connect error, bid = {}, bgroup = {}, consid = {}", bid, bgroup, channelInfo.getConsid(), e);
                }
            }
            logger.info("force close client connect for resourceTable remove, bid = {}, bgroup = {}, count = {}", bid, bgroup, size);
        };
        //添加callback
        updater.addCallback(bid, bgroup, updateCallback, removeCallback);
        logger.info("AsyncCamelliaRedisTemplate init, bid = {}, bgroup = {}", bid, bgroup);
        return template;
    }

    private AsyncCamelliaRedisTemplate initRemoteInstance(long bid, String bgroup) {
        if (apiService == null) return null;
        CamelliaTranspondProperties.RemoteProperties remote = properties.getRemote();
        if (remote == null) return null;
        boolean monitorEnable = remote.isMonitorEnable();
        long checkIntervalMillis = remote.getCheckIntervalMillis();
        AsyncCamelliaRedisTemplate template = new AsyncCamelliaRedisTemplate(env, apiService, bid, bgroup, monitorEnable, checkIntervalMillis);
        logger.info("AsyncCamelliaRedisTemplate init, bid = {}, bgroup = {}", bid, bgroup);
        return template;
    }

    public AsyncCamelliaRedisEnv getEnv() {
        return env;
    }

    private void initEnv() {
        CamelliaTranspondProperties.RedisConfProperties redisConf = properties.getRedisConf();

        AsyncNettyClientFactory clientFactory = new AsyncNettyClientFactory.Default(redisConf.getRedisClusterMaxAttempts());

        RedisClientHub.connectTimeoutMillis = redisConf.getConnectTimeoutMillis();
        RedisClientHub.heartbeatIntervalSeconds = redisConf.getHeartbeatIntervalSeconds();
        RedisClientHub.heartbeatTimeoutMillis = redisConf.getHeartbeatTimeoutMillis();
        logger.info("RedisClient, connectTimeoutMillis = {}, heartbeatIntervalSeconds = {}, heartbeatTimeoutMillis = {}",
                RedisClientHub.connectTimeoutMillis, RedisClientHub.heartbeatIntervalSeconds, RedisClientHub.heartbeatTimeoutMillis);
        RedisClientHub.failCountThreshold = redisConf.getFailCountThreshold();
        RedisClientHub.failBanMillis = redisConf.getFailBanMillis();
        NettyTransportMode nettyTransportMode = GlobalRedisProxyEnv.getNettyTransportMode();
        if (nettyTransportMode == NettyTransportMode.epoll) {
            RedisClientHub.eventLoopGroup = new EpollEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client"));
            RedisClientHub.eventLoopGroupBackup = new EpollEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client-backup"));
        } else if (nettyTransportMode == NettyTransportMode.kqueue) {
            RedisClientHub.eventLoopGroup = new KQueueEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client"));
            RedisClientHub.eventLoopGroupBackup = new KQueueEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client-backup"));
        } else if (nettyTransportMode == NettyTransportMode.io_uring) {
            RedisClientHub.eventLoopGroup = new IOUringEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client"));
            RedisClientHub.eventLoopGroupBackup = new IOUringEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client-backup"));
        } else {
            RedisClientHub.eventLoopGroup = new NioEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client"));
            RedisClientHub.eventLoopGroupBackup = new NioEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client-backup"));
        }
        logger.info("RedisClient, failCountThreshold = {}, failBanMillis = {}",
                RedisClientHub.failCountThreshold, RedisClientHub.failBanMillis);
        RedisClientHub.closeIdleConnection = redisConf.isCloseIdleConnection();
        RedisClientHub.checkIdleConnectionThresholdSeconds = redisConf.getCheckIdleConnectionThresholdSeconds();
        RedisClientHub.closeIdleConnectionDelaySeconds = redisConf.getCloseIdleConnectionDelaySeconds();
        logger.info("RedisClient, closeIdleConnection = {}, checkIdleConnectionThresholdSeconds = {}, closeIdleConnectionDelaySeconds = {}",
                RedisClientHub.closeIdleConnection, RedisClientHub.checkIdleConnectionThresholdSeconds, RedisClientHub.closeIdleConnectionDelaySeconds);

        RedisClientHub.soKeepalive = properties.getNettyProperties().isSoKeepalive();
        RedisClientHub.tcpNoDelay = properties.getNettyProperties().isTcpNoDelay();
        RedisClientHub.soRcvbuf = properties.getNettyProperties().getSoRcvbuf();
        RedisClientHub.soSndbuf = properties.getNettyProperties().getSoSndbuf();
        RedisClientHub.writeBufferWaterMarkLow = properties.getNettyProperties().getWriteBufferWaterMarkLow();
        RedisClientHub.writeBufferWaterMarkHigh = properties.getNettyProperties().getWriteBufferWaterMarkHigh();
        RedisClientHub.tcpQuickAck = GlobalRedisProxyEnv.getNettyTransportMode() == NettyTransportMode.epoll && properties.getNettyProperties().isTcpQuickAck();
        logger.info("RedisClient, so_keepalive = {}, tcp_no_delay = {}, tcp_quick_ack = {}, so_rcvbuf = {}, so_sndbuf = {}, write_buffer_water_mark_Low = {}, write_buffer_water_mark_high = {}",
                RedisClientHub.soKeepalive, RedisClientHub.tcpNoDelay, RedisClientHub.tcpQuickAck, RedisClientHub.soRcvbuf,
                RedisClientHub.soSndbuf, RedisClientHub.writeBufferWaterMarkLow, RedisClientHub.writeBufferWaterMarkHigh);

        RedisClientHub.initDynamicConf();

        ProxyEnv.Builder builder = new ProxyEnv.Builder();
        String shardingFuncClassName = redisConf.getShardingFunc();
        if (shardingFuncClassName != null) {
            ShardingFunc shardingFunc = (ShardingFunc) proxyBeanFactory.getBean(BeanInitUtils.parseClass(shardingFuncClassName));
            builder.shardingFunc(shardingFunc);
            logger.info("ShardingFunc, className = {}", shardingFuncClassName);
        }

        logger.info("multi write mode = {}", redisConf.getMultiWriteMode());

        GlobalRedisProxyEnv.setDiscoveryFactory(ConfigInitUtil.initProxyDiscoveryFactory(redisConf, proxyBeanFactory));

        ProxyEnv proxyEnv = builder.build();

        env = new AsyncCamelliaRedisEnv.Builder()
                .proxyEnv(proxyEnv)
                .clientFactory(clientFactory)
                .multiWriteMode(redisConf.getMultiWriteMode())
                .build();
    }
}
