package com.netease.nim.camellia.redis.proxy.upstream;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiUtil;
import com.netease.nim.camellia.core.api.ResourceTableRemoveCallback;
import com.netease.nim.camellia.core.api.ResourceTableUpdateCallback;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.env.ShardingFunc;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.redis.proxy.command.ProxyCommandProcessor;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.conf.ServerConf;
import com.netease.nim.camellia.redis.proxy.upstream.utils.ScheduledResourceChecker;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.executor.CamelliaLinearInitializationExecutor;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.route.ProxyRouteConfUpdater;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaRouteProperties;
import com.netease.nim.camellia.redis.proxy.monitor.ChannelMonitor;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.util.ConfigInitUtil;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class UpstreamRedisClientTemplateFactory implements IUpstreamClientTemplateFactory {

    private static final Logger logger = LoggerFactory.getLogger(UpstreamRedisClientTemplateFactory.class);

    private String EXECUTOR_KEY;

    private final CamelliaRouteProperties properties;
    private RedisProxyEnv env;
    private CamelliaApi apiService;
    private ProxyRouteConfUpdater updater;

    private boolean lazyInitEnable;
    private boolean multiTenantsSupport = true;//是否支持多租户
    private final ProxyCommandProcessor proxyCommandProcessor;

    private UpstreamRedisClientTemplate remoteInstance;
    private UpstreamRedisClientTemplate localInstance;
    private UpstreamRedisClientTemplate customInstance;
    private CamelliaLinearInitializationExecutor<String, IUpstreamClientTemplate> executor;

    public UpstreamRedisClientTemplateFactory(ProxyCommandProcessor proxyCommandProcessor) {
        this.proxyCommandProcessor = proxyCommandProcessor;
        this.properties = ServerConf.getRouteProperties();
        init();
        logger.info("UpstreamRedisClientTemplateFactory init success");
    }

    @Override
    public IUpstreamClientTemplate getOrInitialize(Long bid, String bgroup) {
        try {
            if (!multiTenantsSupport) {
                CamelliaRouteProperties.Type type = properties.getType();
                if (type == CamelliaRouteProperties.Type.LOCAL) {
                    if (localInstance != null) {
                        return localInstance;
                    }
                    if (executor != null && EXECUTOR_KEY != null) {
                        return executor.get(EXECUTOR_KEY);
                    }
                } else if (type == CamelliaRouteProperties.Type.REMOTE) {
                    if (remoteInstance != null) {
                        return remoteInstance;
                    }
                    if (executor != null && EXECUTOR_KEY != null) {
                        return executor.get(EXECUTOR_KEY);
                    }
                } else if (type == CamelliaRouteProperties.Type.CUSTOM) {
                    if (customInstance != null) {
                        return customInstance;
                    }
                    if (executor != null && EXECUTOR_KEY != null) {
                        return executor.get(EXECUTOR_KEY);
                    }
                }
                return null;
            }
            if (bid == null || bid <= 0 || bgroup == null) {
                CamelliaRouteProperties.Type type = properties.getType();
                if (type == CamelliaRouteProperties.Type.REMOTE) {
                    if (remoteInstance != null) {
                        return remoteInstance;
                    }
                    if (executor != null && EXECUTOR_KEY != null) {
                        return executor.get(EXECUTOR_KEY);
                    }
                } else if (type == CamelliaRouteProperties.Type.CUSTOM) {
                    if (customInstance != null) {
                        return customInstance;
                    }
                    if (executor != null && EXECUTOR_KEY != null) {
                        return executor.get(EXECUTOR_KEY);
                    }
                }
                return null;
            }
            CompletableFuture<IUpstreamClientTemplate> future = getOrInitializeAsync(bid, bgroup);
            if (future == null) return null;
            return future.get();
        } catch (Exception e) {
            ErrorLogCollector.collect(UpstreamRedisClientTemplateFactory.class, "getOrInitialize UpstreamRedisClientTemplate error", e);
            return null;
        }
    }

    @Override
    public CompletableFuture<IUpstreamClientTemplate> getOrInitializeAsync(Long bid, String bgroup) {
        CamelliaRouteProperties.Type type = properties.getType();
        if (type == CamelliaRouteProperties.Type.LOCAL) {
            if (localInstance != null) {
                return CompletableFuture.completedFuture(localInstance);
            }
            if (executor != null && EXECUTOR_KEY != null) {
                return executor.getOrInitialize(EXECUTOR_KEY);
            }
        } else if (type == CamelliaRouteProperties.Type.REMOTE) {
            CamelliaRouteProperties.RemoteProperties remote = properties.getRemote();
            if (remoteInstance != null) {
                if (!remote.isDynamic()) {
                    return CompletableFuture.completedFuture(remoteInstance);
                }
                if (bid == null || bid <= 0 || bgroup == null) {
                    return CompletableFuture.completedFuture(remoteInstance);
                }
            }
            if (executor != null) {
                if (!remote.isDynamic()) {
                    if (EXECUTOR_KEY != null) {
                        return executor.getOrInitialize(EXECUTOR_KEY);
                    }
                    return null;
                }
                if (bid == null || bid <= 0 || bgroup == null) {
                    if (EXECUTOR_KEY != null) {
                        return executor.getOrInitialize(EXECUTOR_KEY);
                    }
                    return null;
                }
                return executor.getOrInitialize(Utils.getCacheKey(bid, bgroup));
            }
        } else if (type == CamelliaRouteProperties.Type.CUSTOM) {
            CamelliaRouteProperties.CustomProperties custom = properties.getCustom();
            if (customInstance != null) {
                if (!custom.isDynamic()) {
                    return CompletableFuture.completedFuture(customInstance);
                }
                if (bid == null || bid <= 0 || bgroup == null) {
                    return CompletableFuture.completedFuture(customInstance);
                }
            }
            if (executor != null) {
                if (!custom.isDynamic()) {
                    if (EXECUTOR_KEY != null) {
                        return executor.getOrInitialize(EXECUTOR_KEY);
                    }
                    return null;
                }
                if (bid == null || bid <= 0 || bgroup == null) {
                    if (EXECUTOR_KEY != null) {
                        return executor.getOrInitialize(EXECUTOR_KEY);
                    }
                    return null;
                }
                return executor.getOrInitialize(Utils.getCacheKey(bid, bgroup));
            }
        }
        return null;
    }

    @Override
    public IUpstreamClientTemplate tryGet(Long bid, String bgroup) {
        CamelliaRouteProperties.Type type = properties.getType();
        if (type == CamelliaRouteProperties.Type.LOCAL) {
            if (localInstance != null) {
                return localInstance;
            }
            if (executor != null && EXECUTOR_KEY != null) {
                return executor.get(EXECUTOR_KEY);
            }
        } else if (type == CamelliaRouteProperties.Type.REMOTE) {
            CamelliaRouteProperties.RemoteProperties remote = properties.getRemote();
            if (remoteInstance != null) {
                if (!remote.isDynamic()) {
                    return remoteInstance;
                }
                if (bid == null || bid <= 0 || bgroup == null) {
                    return remoteInstance;
                }
            }
            if (executor != null) {
                if (!remote.isDynamic()) {
                    if (EXECUTOR_KEY != null) {
                        return executor.get(EXECUTOR_KEY);
                    }
                    return null;
                }
                if (bid == null || bid <= 0 || bgroup == null) {
                    if (EXECUTOR_KEY != null) {
                        return executor.get(EXECUTOR_KEY);
                    }
                    return null;
                }
                return executor.get(Utils.getCacheKey(bid, bgroup));
            }
        } else if (type == CamelliaRouteProperties.Type.CUSTOM) {
            CamelliaRouteProperties.CustomProperties custom = properties.getCustom();
            if (customInstance != null) {
                if (!custom.isDynamic()) {
                    return customInstance;
                }
                if (bid == null || bid <= 0 || bgroup == null) {
                    return customInstance;
                }
            }
            if (executor != null) {
                if (!custom.isDynamic()) {
                    if (EXECUTOR_KEY != null) {
                        return executor.get(EXECUTOR_KEY);
                    }
                    return null;
                }
                if (bid == null || bid <= 0 || bgroup == null) {
                    if (EXECUTOR_KEY != null) {
                        return executor.get(EXECUTOR_KEY);
                    }
                    return null;
                }
                return executor.get(Utils.getCacheKey(bid, bgroup));
            }
        }
        return null;
    }

    @Override
    public boolean isMultiTenantsSupport() {
        return multiTenantsSupport;
    }

    @Override
    public RedisProxyEnv getEnv() {
        return env;
    }

    @Override
    public boolean lazyInitEnable() {
        return lazyInitEnable;
    }

    @Override
    public synchronized int shutdown() {
        int size = 0;
        if (localInstance != null) {
            localInstance.shutdown();
            localInstance = null;
            size ++;
        }
        if (remoteInstance != null) {
            remoteInstance.shutdown();
            remoteInstance = null;
            size ++;
        }
        if (customInstance != null) {
            customInstance.shutdown();;
            customInstance = null;
            size ++;
        }
        if (executor != null) {
            Map<String, IUpstreamClientTemplate> map = executor.getAll();
            if (map != null) {
                for (Map.Entry<String, IUpstreamClientTemplate> entry : map.entrySet()) {
                    String key = entry.getKey();
                    IUpstreamClientTemplate template = entry.getValue();
                    template.shutdown();
                    executor.remove(key);
                    size ++;
                }
            }
        }
        return size;
    }

    private void init() {
        CamelliaRouteProperties.Type type = properties.getType();
        if (type == null) {
            throw new IllegalArgumentException();
        }
        this.lazyInitEnable = ProxyDynamicConf.getBoolean("upstream.lazy.init.enable", false);
        initEnv();
        logger.info("CamelliaRedisProxy init, type = {}", type);
        if (type == CamelliaRouteProperties.Type.LOCAL) {
            initLocal();
        } else if (type == CamelliaRouteProperties.Type.REMOTE) {
            initRemote();
        } else if (type == CamelliaRouteProperties.Type.CUSTOM) {
            initCustom();
        }
        boolean preheatEnable = ProxyDynamicConf.getBoolean("upstream.preheat.enable", true);
        if (preheatEnable) {
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
        CamelliaRouteProperties.LocalProperties local = properties.getLocal();
        if (local == null) {
            throw new IllegalArgumentException("local is null");
        }
        JSONObject transpondConfig = new JSONObject(true);
        transpondConfig.put("type", "local");

        ResourceTable resourceTable = local.getResourceTable();
        if (resourceTable == null) {
            String resourceTableFilePath = local.getResourceTableFilePath();
            if (resourceTableFilePath != null) {
                transpondConfig.put("resourceTableFilePath", resourceTableFilePath);
            } else {
                throw new IllegalArgumentException("local.resourceTable/local.resourceTableFilePath is null");
            }
        }
        if (!lazyInitEnable) {
            localInstance = initLocalTemplate(local);
        } else {
            executor = new CamelliaLinearInitializationExecutor<>("upstream-redis-client-template-init", key -> initLocalTemplate(local),
                    () -> ProxyDynamicConf.getInt("upstream.redis.client.template.init.pending.size", 1000000));
            EXECUTOR_KEY = "local";
        }
        multiTenantsSupport = false;
        transpondConfig.put("multiTenantsSupport", false);
        proxyCommandProcessor.setTranspondConfig(transpondConfig);
    }

    private UpstreamRedisClientTemplate initLocalTemplate(CamelliaRouteProperties.LocalProperties local) {
        ResourceTable resourceTable = local.getResourceTable();
        UpstreamRedisClientTemplate template = null;
        if (resourceTable != null) {
            template = new UpstreamRedisClientTemplate(env, resourceTable);
        } else {
            String resourceTableFilePath = local.getResourceTableFilePath();
            if (resourceTableFilePath != null) {
                template = new UpstreamRedisClientTemplate(env, resourceTableFilePath, local.getCheckIntervalMillis());
            }
        }
        return template;
    }

    private void initRemote() {
        CamelliaRouteProperties.RemoteProperties remote = properties.getRemote();
        if (remote == null) {
            throw new IllegalArgumentException("remote is null");
        }
        String url = remote.getUrl();
        if (url == null) {
            throw new IllegalArgumentException("remote.url is null");
        }
        JSONObject transpondConfig = new JSONObject(true);
        transpondConfig.put("type", "remote");
        transpondConfig.put("url", url);

        apiService = CamelliaApiUtil.init(url, remote.getConnectTimeoutMillis(), remote.getReadTimeoutMillis(), remote.getHeaderMap());
        logger.info("ApiService init, url = {}", url);
        boolean dynamic = remote.isDynamic();
        logger.info("Remote dynamic = {}", dynamic);

        multiTenantsSupport = remote.isDynamic();

        boolean executorInit = false;

        if (remote.getBid() > 0 && remote.getBgroup() != null) {
            transpondConfig.put("bid", remote.getBid());
            transpondConfig.put("brgoup", remote.getBgroup());
            if (!lazyInitEnable) {
                remoteInstance = initRemoteInstance(remote.getBid(), remote.getBgroup());
            } else {
                EXECUTOR_KEY = remote.getBid() + "|" + remote.getBgroup();
                executorInit = true;
            }
        }
        if (multiTenantsSupport || executorInit) {
            executor = new CamelliaLinearInitializationExecutor<>("upstream-redis-client-template-init", key -> {
                String[] split = key.split("\\|");
                return initRemoteInstance(Long.parseLong(split[0]), split[1]);
            }, () -> ProxyDynamicConf.getInt("upstream.redis.client.template.init.pending.size", 1000000));

            if (remote.getBid() > 0 && remote.getBgroup() != null && remoteInstance != null) {
                executor.put(remote.getBid() + "|" + remote.getBgroup(), remoteInstance);
            }
        }
        transpondConfig.put("multiTenantsSupport", multiTenantsSupport);
        proxyCommandProcessor.setTranspondConfig(transpondConfig);
    }

    private void initCustom() {
        CamelliaRouteProperties.CustomProperties custom = properties.getCustom();
        if (custom == null) {
            throw new IllegalArgumentException("custom is null");
        }
        ProxyRouteConfUpdater proxyRouteConfUpdater = custom.getProxyRouteConfUpdater();
        if (proxyRouteConfUpdater == null) {
            throw new IllegalArgumentException("proxyRouteConfUpdater is null");
        }

        JSONObject transpondConfig = new JSONObject(true);
        transpondConfig.put("type", "custom");
        transpondConfig.put("proxyRouteConfUpdater", proxyRouteConfUpdater.getClass().getName());

        this.updater = proxyRouteConfUpdater;
        boolean dynamic = custom.isDynamic();
        logger.info("Custom dynamic = {}", dynamic);

        multiTenantsSupport = custom.isDynamic();
        boolean executorInit = false;

        if (custom.getBid() > 0 && custom.getBgroup() != null) {
            transpondConfig.put("bid", custom.getBid());
            transpondConfig.put("brgoup", custom.getBgroup());
            if (!lazyInitEnable) {
                customInstance = initCustomInstance(custom.getBid(), custom.getBgroup());
            } else {
                EXECUTOR_KEY = custom.getBid() + "|" + custom.getBgroup();
                executorInit = true;
            }
        }

        if (multiTenantsSupport || executorInit) {
            executor = new CamelliaLinearInitializationExecutor<>("upstream-redis-client-template-init", key -> {
                String[] split = key.split("\\|");
                return initCustomInstance(Long.parseLong(split[0]), split[1]);
            }, () -> ProxyDynamicConf.getInt("upstream.redis.client.template.init.pending.size", 1000000));

            if (custom.getBid() > 0 && custom.getBgroup() != null && customInstance != null) {
                executor.put(custom.getBid() + "|" + custom.getBgroup(), customInstance);
            }
        }
        transpondConfig.put("multiTenantsSupport", multiTenantsSupport);
        proxyCommandProcessor.setTranspondConfig(transpondConfig);
    }

    private UpstreamRedisClientTemplate initCustomInstance(long bid, String bgroup) {
        CamelliaRouteProperties.CustomProperties custom = properties.getCustom();
        if (custom == null) return null;
        logger.info("UpstreamRedisClientTemplate init custom instance, bid = {}, bgroup = {}", bid, bgroup);
        UpstreamRedisClientTemplate template = new UpstreamRedisClientTemplate(env, bid, bgroup, updater, custom.getReloadIntervalMillis());
        //更新的callback
        ResourceTableUpdateCallback updateCallback = template::update;
        //删除的callback
        ResourceTableRemoveCallback removeCallback = () -> {
            if (executor != null) {
                executor.remove(Utils.getCacheKey(bid, bgroup));
            }
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
        return template;
    }

    private UpstreamRedisClientTemplate initRemoteInstance(long bid, String bgroup) {
        if (apiService == null) return null;
        CamelliaRouteProperties.RemoteProperties remote = properties.getRemote();
        if (remote == null) return null;
        boolean monitorEnable = remote.isMonitorEnable();
        long checkIntervalMillis = remote.getCheckIntervalMillis();
        logger.info("UpstreamRedisClientTemplate init remote instance, bid = {}, bgroup = {}", bid, bgroup);
        return new UpstreamRedisClientTemplate(env, apiService, bid, bgroup, monitorEnable, checkIntervalMillis);
    }

    private void initEnv() {
        int maxAttempts = ProxyDynamicConf.getInt("upstream.redis.cluster.max.attempts", Constants.Upstream.redisClusterMaxAttempts);
        UpstreamRedisClientFactory clientFactory = new UpstreamRedisClientFactory.Default(maxAttempts);

        RedisConnectionHub.getInstance().init();

        ProxyEnv.Builder builder = new ProxyEnv.Builder();


        ShardingFunc shardingFunc = ConfigInitUtil.initShardingFunc();
        if (shardingFunc != null) {
            builder.shardingFunc(shardingFunc);
            logger.info("ShardingFunc, className = {}", shardingFunc.getClass().getName());
        }

        GlobalRedisProxyEnv.setDiscoveryFactory(ConfigInitUtil.initProxyDiscoveryFactory());

        ProxyEnv proxyEnv = builder.build();

        env = new RedisProxyEnv.Builder()
                .proxyEnv(proxyEnv)
                .clientFactory(clientFactory)
                .resourceChecker(new ScheduledResourceChecker(clientFactory))
                .build();
        GlobalRedisProxyEnv.setRedisProxyEnv(env);
    }
}
