package com.netease.nim.camellia.redis.proxy.upstream;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiUtil;
import com.netease.nim.camellia.core.api.ResourceTableRemoveCallback;
import com.netease.nim.camellia.core.api.ResourceTableUpdateCallback;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.env.ShardingFunc;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.redis.proxy.plugin.DefaultBeanFactory;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.tools.utils.SysUtils;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyBeanFactory;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.route.ProxyRouteConfUpdater;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.tools.utils.LockMap;
import com.netease.nim.camellia.redis.proxy.monitor.ChannelMonitor;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;
import com.netease.nim.camellia.redis.proxy.util.ConfigInitUtil;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
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
public class UpstreamRedisClientTemplateChooser implements IUpstreamClientTemplateChooser {

    private static final Logger logger = LoggerFactory.getLogger(UpstreamRedisClientTemplateChooser.class);

    private final CamelliaTranspondProperties properties;
    private RedisProxyEnv env;
    private CamelliaApi apiService;
    private ProxyRouteConfUpdater updater;

    private boolean multiTenancySupport = true;//是否支持多租户
    private final ProxyBeanFactory proxyBeanFactory;

    private UpstreamRedisClientTemplate remoteInstance;
    private UpstreamRedisClientTemplate localInstance;
    private UpstreamRedisClientTemplate customInstance;
    private final ConcurrentHashMap<String, UpstreamRedisClientTemplate> remoteInstanceMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UpstreamRedisClientTemplate> customInstanceMap = new ConcurrentHashMap<>();

    public UpstreamRedisClientTemplateChooser(CamelliaTranspondProperties properties, ProxyBeanFactory proxyBeanFactory) {
        this.properties = properties;
        this.proxyBeanFactory = proxyBeanFactory != null ? proxyBeanFactory : DefaultBeanFactory.INSTANCE;
        init();
    }

    @Override
    public IUpstreamClientTemplate choose(Long bid, String bgroup) {
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
            CompletableFuture<IUpstreamClientTemplate> future = chooseAsync(bid, bgroup);
            if (future == null) return null;
            return future.get();
        } catch (Exception e) {
            ErrorLogCollector.collect(UpstreamRedisClientTemplateChooser.class, "choose AsyncCamelliaRedisTemplate error", e);
            return null;
        }
    }

    @Override
    public CompletableFuture<IUpstreamClientTemplate> chooseAsync(Long bid, String bgroup) {
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

    @Override
    public boolean isMultiTenancySupport() {
        return multiTenancySupport;
    }

    @Override
    public RedisProxyEnv getEnv() {
        return env;
    }

    private CompletableFuture<IUpstreamClientTemplate> wrapper(UpstreamRedisClientTemplate template) {
        CompletableFuture<IUpstreamClientTemplate> future = new CompletableFuture<>();
        future.complete(template);
        return future;
    }

    private final LockMap lockMap = new LockMap();
    private final ConcurrentHashMap<String, AtomicBoolean> initTagMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LinkedBlockingQueue<CompletableFuture<IUpstreamClientTemplate>>> futureQueueMap = new ConcurrentHashMap<>();
    private final ExecutorService exec = new ThreadPoolExecutor(SysUtils.getCpuNum(), SysUtils.getCpuNum(), 0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10000), new DefaultThreadFactory("async-redis-template-init"));

    private CompletableFuture<IUpstreamClientTemplate> initAsync(long bid, String bgroup,
                                                                     ConcurrentHashMap<String, UpstreamRedisClientTemplate> map,
                                                                     AsyncCamelliaRedisTemplateInitializer initializer) {
        String key = bid + "|" + bgroup;
        UpstreamRedisClientTemplate template = map.get(key);
        if (template != null) {
            AtomicBoolean initTag = _getInitTag(key);
            if (initTag.get()) {
                synchronized (lockMap.getLockObj(key)) {
                    if (initTag.get()) {
                        CompletableFuture<IUpstreamClientTemplate> future = new CompletableFuture<>();
                        boolean offer = _getFutureQueue(key).offer(future);
                        if (!offer) {
                            future.complete(null);
                            ErrorLogCollector.collect(UpstreamRedisClientTemplateChooser.class, "init AsyncCamelliaRedisTemplate async fail, queue full");
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
        CompletableFuture<IUpstreamClientTemplate> future = new CompletableFuture<>();
        AtomicBoolean initTag = _getInitTag(key);
        if (initTag.compareAndSet(false, true)) {
            try {
                exec.submit(() -> {
                    UpstreamRedisClientTemplate redisTemplate = null;
                    try {
                        redisTemplate = initializer.init(bid, bgroup);
                        map.put(key, redisTemplate);
                        future.complete(redisTemplate);
                    } catch (Exception e) {
                        ErrorLogCollector.collect(UpstreamRedisClientTemplateChooser.class, "init AsyncCamelliaRedisTemplate error", e);
                        future.complete(null);
                    } finally {
                        clearFutureQueue(key, initTag, redisTemplate);
                    }
                });
            } catch (Exception e) {
                ErrorLogCollector.collect(UpstreamRedisClientTemplateChooser.class, "submit AsyncCamelliaRedisTemplate init task error", e);
                clearFutureQueue(key, initTag, null);
            }
        } else {
            LinkedBlockingQueue<CompletableFuture<IUpstreamClientTemplate>> queue = CamelliaMapUtils.computeIfAbsent(futureQueueMap, key,
                    k -> new LinkedBlockingQueue<>(1000000));
            boolean offer = queue.offer(future);
            if (!offer) {
                future.complete(null);
                ErrorLogCollector.collect(UpstreamRedisClientTemplateChooser.class, "init AsyncCamelliaRedisTemplate async fail, queue full");
            }
        }
        return future;
    }

    private void clearFutureQueue(String key, AtomicBoolean initTag, UpstreamRedisClientTemplate template) {
        synchronized (lockMap.getLockObj(key)) {
            LinkedBlockingQueue<CompletableFuture<IUpstreamClientTemplate>> queue = _getFutureQueue(key);
            CompletableFuture<IUpstreamClientTemplate> completableFuture = queue.poll();
            while (completableFuture != null) {
                try {
                    completableFuture.complete(template);
                } catch (Exception e) {
                    ErrorLogCollector.collect(UpstreamRedisClientTemplateChooser.class, "future complete error", e);
                }
                completableFuture = queue.poll();
            }
            initTag.compareAndSet(true, false);
        }
    }

    private AtomicBoolean _getInitTag(String key) {
        return CamelliaMapUtils.computeIfAbsent(initTagMap, key, k -> new AtomicBoolean(false));
    }

    private LinkedBlockingQueue<CompletableFuture<IUpstreamClientTemplate>> _getFutureQueue(String key) {
        return CamelliaMapUtils.computeIfAbsent(futureQueueMap, key, k -> new LinkedBlockingQueue<>(1000000));
    }

    private interface AsyncCamelliaRedisTemplateInitializer {
        UpstreamRedisClientTemplate init(long bid, String bgroup);
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
            localInstance = new UpstreamRedisClientTemplate(env, resourceTable);
        } else {
            String resourceTableFilePath = local.getResourceTableFilePath();
            if (resourceTableFilePath != null) {
                localInstance = new UpstreamRedisClientTemplate(env, resourceTableFilePath, local.getCheckIntervalMillis());
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

    private UpstreamRedisClientTemplate initCustomInstance(long bid, String bgroup) {
        CamelliaTranspondProperties.CustomProperties custom = properties.getCustom();
        if (custom == null) return null;
        UpstreamRedisClientTemplate template = new UpstreamRedisClientTemplate(env, bid, bgroup, updater, custom.getReloadIntervalMillis());
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

    private UpstreamRedisClientTemplate initRemoteInstance(long bid, String bgroup) {
        if (apiService == null) return null;
        CamelliaTranspondProperties.RemoteProperties remote = properties.getRemote();
        if (remote == null) return null;
        boolean monitorEnable = remote.isMonitorEnable();
        long checkIntervalMillis = remote.getCheckIntervalMillis();
        UpstreamRedisClientTemplate template = new UpstreamRedisClientTemplate(env, apiService, bid, bgroup, monitorEnable, checkIntervalMillis);
        logger.info("AsyncCamelliaRedisTemplate init, bid = {}, bgroup = {}", bid, bgroup);
        return template;
    }

    private void initEnv() {
        CamelliaTranspondProperties.RedisConfProperties redisConf = properties.getRedisConf();

        UpstreamRedisClientFactory clientFactory = new UpstreamRedisClientFactory.Default(redisConf.getRedisClusterMaxAttempts());

        RedisConnectionHub.getInstance().init(properties);

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

        env = new RedisProxyEnv.Builder()
                .proxyEnv(proxyEnv)
                .clientFactory(clientFactory)
                .multiWriteMode(redisConf.getMultiWriteMode())
                .build();
    }
}
