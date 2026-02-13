package com.netease.nim.camellia.redis.proxy.upstream;

import com.netease.nim.camellia.core.api.ResourceTableRemoveCallback;
import com.netease.nim.camellia.core.api.ResourceTableUpdateCallback;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.env.ShardingFunc;
import com.netease.nim.camellia.redis.proxy.conf.*;
import com.netease.nim.camellia.redis.proxy.route.RouteConfProvider;
import com.netease.nim.camellia.redis.proxy.upstream.utils.ScheduledResourceChecker;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.executor.CamelliaLinearInitializationExecutor;
import com.netease.nim.camellia.redis.proxy.conf.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
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

    private RedisProxyEnv env;
    private final RouteConfProvider provider;

    private boolean lazyInitEnable;
    private boolean multiTenantsSupport = true;//是否支持多租户

    private UpstreamRedisClientTemplate instance;
    private CamelliaLinearInitializationExecutor<String, IUpstreamClientTemplate> executor;

    public UpstreamRedisClientTemplateFactory(RouteConfProvider provider) {
        this.provider = provider;
        init();
        logger.info("UpstreamRedisClientTemplateFactory init success");
    }

    @Override
    public IUpstreamClientTemplate getOrInitialize(Long bid, String bgroup) {
        try {
            if (instance != null) {
                if (!multiTenantsSupport) {
                    return instance;
                }
                if (bid == null || bid <= 0 || bgroup == null) {
                    return instance;
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
        if (instance != null) {
            if (!multiTenantsSupport) {
                return CompletableFuture.completedFuture(instance);
            }
            if (bid == null || bid <= 0 || bgroup == null) {
                return CompletableFuture.completedFuture(instance);
            }
        }
        if (executor != null) {
            if (bid == null) {
                bid = -1L;
            }
            if (bgroup == null) {
                bgroup = "default";
            }
            return executor.getOrInitialize(Utils.getCacheKey(bid, bgroup));
        }
        return null;
    }

    @Override
    public IUpstreamClientTemplate tryGet(Long bid, String bgroup) {
        if (!multiTenantsSupport) {
            return instance;
        }
        if (bid == null || bid <= 0 || bgroup == null) {
            return instance;
        }
        if (executor != null) {
            return executor.get(Utils.getCacheKey(bid, bgroup));
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
        if (instance != null) {
            instance.shutdown();
            instance = null;
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
        this.lazyInitEnable = ProxyDynamicConf.getBoolean("upstream.lazy.init.enable", false);
        initEnv();
        this.multiTenantsSupport = provider.isMultiTenantsSupport();
        if (!multiTenantsSupport && !lazyInitEnable) {
            this.instance = initInstance(-1, "default");
        }
        boolean preheatEnable = ProxyDynamicConf.getBoolean("upstream.preheat.enable", true);
        if (preheatEnable && instance != null) {
            instance.preheat();
        }

        if (multiTenantsSupport || lazyInitEnable) {
            executor = new CamelliaLinearInitializationExecutor<>("upstream-redis-client-template-init", key -> {
                String[] split = key.split("\\|");
                return initInstance(Long.parseLong(split[0]), split[1]);
            }, () -> ProxyDynamicConf.getInt("upstream.redis.client.template.init.pending.size", 1000000));
        }
    }

    private UpstreamRedisClientTemplate initInstance(long bid, String bgroup) {
        logger.info("UpstreamRedisClientTemplate init instance, bid = {}, bgroup = {}", bid, bgroup);
        UpstreamRedisClientTemplate template = new UpstreamRedisClientTemplate(env, bid, bgroup, provider);
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
        provider.addCallback(bid, bgroup, updateCallback, removeCallback);
        return template;
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
