package com.netease.nim.camellia.redis.proxy.upstream;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiUtil;
import com.netease.nim.camellia.core.api.ResourceTableRemoveCallback;
import com.netease.nim.camellia.core.api.ResourceTableUpdateCallback;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.env.ShardingFunc;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.plugin.DefaultBeanFactory;
import com.netease.nim.camellia.redis.proxy.upstream.utils.ScheduledResourceChecker;
import com.netease.nim.camellia.tools.executor.CamelliaLinearInitializationExecutor;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyBeanFactory;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.route.ProxyRouteConfUpdater;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.monitor.ChannelMonitor;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;
import com.netease.nim.camellia.redis.proxy.util.ConfigInitUtil;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;

/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class UpstreamRedisClientTemplateFactory implements IUpstreamClientTemplateFactory {

    private static final Logger logger = LoggerFactory.getLogger(UpstreamRedisClientTemplateFactory.class);

    private final CamelliaTranspondProperties properties;
    private RedisProxyEnv env;
    private CamelliaApi apiService;
    private ProxyRouteConfUpdater updater;

    private boolean multiTenancySupport = true;//是否支持多租户
    private final ProxyBeanFactory proxyBeanFactory;

    private UpstreamRedisClientTemplate remoteInstance;
    private UpstreamRedisClientTemplate localInstance;
    private UpstreamRedisClientTemplate customInstance;
    private CamelliaLinearInitializationExecutor<String, IUpstreamClientTemplate> executor;

    public UpstreamRedisClientTemplateFactory(CamelliaTranspondProperties properties, ProxyBeanFactory proxyBeanFactory) {
        this.properties = properties;
        this.proxyBeanFactory = proxyBeanFactory != null ? proxyBeanFactory : DefaultBeanFactory.INSTANCE;
        init();
        logger.info("UpstreamRedisClientTemplateFactory init success");
    }

    @Override
    public IUpstreamClientTemplate getOrInitialize(Long bid, String bgroup) {
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
        CamelliaTranspondProperties.Type type = properties.getType();
        if (type == CamelliaTranspondProperties.Type.LOCAL) {
            return IUpstreamClientTemplateFactory.wrapper(localInstance);
        } else if (type == CamelliaTranspondProperties.Type.REMOTE) {
            CamelliaTranspondProperties.RemoteProperties remote = properties.getRemote();
            if (!remote.isDynamic()) {
                return IUpstreamClientTemplateFactory.wrapper(remoteInstance);
            }
            if (bid == null || bid <= 0 || bgroup == null) {
                return IUpstreamClientTemplateFactory.wrapper(remoteInstance);
            }
            if (executor != null) {
                return executor.getOrInitialize(bid + "|" + bgroup);
            }
        } else if (type == CamelliaTranspondProperties.Type.CUSTOM) {
            CamelliaTranspondProperties.CustomProperties custom = properties.getCustom();
            if (!custom.isDynamic()) {
                return IUpstreamClientTemplateFactory.wrapper(customInstance);
            }
            if (bid == null || bid <= 0 || bgroup == null) {
                return IUpstreamClientTemplateFactory.wrapper(customInstance);
            }
            if (executor != null) {
                return executor.getOrInitialize(bid + "|" + bgroup);
            }
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
        }
        multiTenancySupport = remote.isDynamic();
        if (multiTenancySupport) {
            executor = new CamelliaLinearInitializationExecutor<>("upstream-redis-client-template-init", key -> {
                String[] split = key.split("\\|");
                return initRemoteInstance(Long.parseLong(split[0]), split[1]);
            }, () -> ProxyDynamicConf.getInt("upstream.redis.client.template.init.pending.size", 1000000));

            if (remote.getBid() > 0 && remote.getBgroup() != null && remoteInstance != null) {
                executor.put(remote.getBid() + "|" + remote.getBgroup(), remoteInstance);
            }
        }
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
        }
        multiTenancySupport = custom.isDynamic();
        if (multiTenancySupport) {
            executor = new CamelliaLinearInitializationExecutor<>("upstream-redis-client-template-init", key -> {
                String[] split = key.split("\\|");
                return initCustomInstance(Long.parseLong(split[0]), split[1]);
            }, () -> ProxyDynamicConf.getInt("upstream.redis.client.template.init.pending.size", 1000000));

            if (custom.getBid() > 0 && custom.getBgroup() != null && customInstance != null) {
                executor.put(custom.getBid() + "|" + custom.getBgroup(), customInstance);
            }
        }
    }

    private UpstreamRedisClientTemplate initCustomInstance(long bid, String bgroup) {
        CamelliaTranspondProperties.CustomProperties custom = properties.getCustom();
        if (custom == null) return null;
        UpstreamRedisClientTemplate template = new UpstreamRedisClientTemplate(env, bid, bgroup, updater, custom.getReloadIntervalMillis());
        //更新的callback
        ResourceTableUpdateCallback updateCallback = template::update;
        //删除的callback
        ResourceTableRemoveCallback removeCallback = () -> {
            if (executor != null) {
                executor.remove(bid + "|" + bgroup);
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
        logger.info("UpstreamRedisClientTemplate init, bid = {}, bgroup = {}", bid, bgroup);
        return template;
    }

    private UpstreamRedisClientTemplate initRemoteInstance(long bid, String bgroup) {
        if (apiService == null) return null;
        CamelliaTranspondProperties.RemoteProperties remote = properties.getRemote();
        if (remote == null) return null;
        boolean monitorEnable = remote.isMonitorEnable();
        long checkIntervalMillis = remote.getCheckIntervalMillis();
        UpstreamRedisClientTemplate template = new UpstreamRedisClientTemplate(env, apiService, bid, bgroup, monitorEnable, checkIntervalMillis);
        logger.info("UpstreamRedisClientTemplate init, bid = {}, bgroup = {}", bid, bgroup);
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
                .resourceChecker(new ScheduledResourceChecker(clientFactory))
                .multiWriteMode(redisConf.getMultiWriteMode())
                .build();
    }
}
