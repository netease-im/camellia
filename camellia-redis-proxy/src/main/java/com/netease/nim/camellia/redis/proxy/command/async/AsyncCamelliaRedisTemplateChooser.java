package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiUtil;
import com.netease.nim.camellia.core.api.ResourceTableRemoveCallback;
import com.netease.nim.camellia.core.api.ResourceTableUpdateCallback;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.env.ShardingFunc;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ShardingFuncUtil;
import com.netease.nim.camellia.redis.proxy.command.async.route.ProxyRouteConfUpdater;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.core.util.LockMap;
import com.netease.nim.camellia.redis.proxy.monitor.ChannelMonitor;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class AsyncCamelliaRedisTemplateChooser {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCamelliaRedisTemplateChooser.class);

    private final CamelliaTranspondProperties properties;
    private AsyncCamelliaRedisEnv env;
    private CamelliaApi apiService;

    private final LockMap lockMap = new LockMap();
    private AsyncCamelliaRedisTemplate remoteInstance;
    private AsyncCamelliaRedisTemplate localInstance;
    private AsyncCamelliaRedisTemplate customInstance;
    private final ConcurrentHashMap<String, AsyncCamelliaRedisTemplate> remoteInstanceMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AsyncCamelliaRedisTemplate> customInstanceMap = new ConcurrentHashMap<>();

    public AsyncCamelliaRedisTemplateChooser(CamelliaTranspondProperties properties) {
        this.properties = properties;
        init();
    }

    public AsyncCamelliaRedisTemplate choose(Long bid, String bgroup) {
        CamelliaTranspondProperties.Type type = properties.getType();
        if (type == CamelliaTranspondProperties.Type.LOCAL) {
            return localInstance;
        } else if (type == CamelliaTranspondProperties.Type.REMOTE) {
            CamelliaTranspondProperties.RemoteProperties remote = properties.getRemote();
            if (!remote.isDynamic()) {
                if (logger.isTraceEnabled()) {
                    logger.trace("not dynamic, return default remoteInstance");
                }
                return remoteInstance;
            }
            if (bid == null || bid <= 0 || bgroup == null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("not dynamic, return default remoteInstance");
                }
                return remoteInstance;
            }
            return initOrCreateRemoteInstance(bid, bgroup);
        } else if (type == CamelliaTranspondProperties.Type.CUSTOM) {
            CamelliaTranspondProperties.CustomProperties custom = properties.getCustom();
            if (!custom.isDynamic()) {
                if (logger.isTraceEnabled()) {
                    logger.trace("not dynamic, return default customInstance");
                }
                return customInstance;
            }
            if (bid == null || bid <= 0 || bgroup == null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("not dynamic, return default customInstance");
                }
                return customInstance;
            }
            return initOrCreateCustomInstance(bid, bgroup);
        } else if (type == CamelliaTranspondProperties.Type.AUTO) {
            if (bid == null || bid <= 0 || bgroup == null) {
                if (localInstance != null) return localInstance;
                if (remoteInstance != null) return remoteInstance;
                if (customInstance != null) return customInstance;
                logger.warn("no bid/bgroup, return null");
                return null;
            }
            AsyncCamelliaRedisTemplate template = initOrCreateRemoteInstance(bid, bgroup);
            if (template != null) return template;
            return initOrCreateCustomInstance(bid, bgroup);
        }
        return null;
    }

    private void init() {
        CamelliaTranspondProperties.Type type = properties.getType();
        if (type == null) {
            throw new IllegalArgumentException();
        }
        initEnv();
        logger.info("CamelliaRedisProxy init, type = {}", type);
        if (type == CamelliaTranspondProperties.Type.LOCAL) {
            initLocal(true);
        } else if (type == CamelliaTranspondProperties.Type.REMOTE) {
            initRemote(true);
        } else if (type == CamelliaTranspondProperties.Type.CUSTOM) {
            initCustom(true);
        } else if (type == CamelliaTranspondProperties.Type.AUTO) {
            initLocal(false);
            initRemote(false);
            initCustom(false);
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

    private void initRemote(boolean throwError) {
        CamelliaTranspondProperties.RemoteProperties remote = properties.getRemote();
        if (remote == null) {
            if (throwError) {
                throw new IllegalArgumentException("remote is null");
            } else {
                return;
            }
        }
        String url = remote.getUrl();
        if (url == null) {
            if (throwError) {
                throw new IllegalArgumentException("remote.url is null");
            } else {
                return;
            }
        }
        apiService = CamelliaApiUtil.init(url, remote.getConnectTimeoutMillis(), remote.getReadTimeoutMillis());
        logger.info("ApiService init, url = {}", url);
        boolean dynamic = remote.isDynamic();
        logger.info("Remote dynamic = {}", dynamic);
        if (remote.getBid() > 0 && remote.getBgroup() != null) {
            remoteInstance = initOrCreateRemoteInstance(remote.getBid(), remote.getBgroup());
        }
    }

    private void initLocal(boolean throwError) {
        CamelliaTranspondProperties.LocalProperties local = properties.getLocal();
        if (local == null) {
            if (throwError) {
                throw new IllegalArgumentException("local is null");
            } else {
                return;
            }
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
        if (localInstance == null && throwError) {
            throw new IllegalArgumentException("local.resourceTable/local.resourceTableFilePath is null");
        }
    }

    private void initCustom(boolean throwError) {
        CamelliaTranspondProperties.CustomProperties custom = properties.getCustom();
        if (custom == null) {
            if (throwError) {
                throw new IllegalArgumentException("custom is null");
            } else {
                return;
            }
        }
        ProxyRouteConfUpdater proxyRouteConfUpdater = custom.getProxyRouteConfUpdater();
        if (proxyRouteConfUpdater == null) {
            if (throwError) {
                throw new IllegalArgumentException("proxyRouteConfUpdater is null");
            } else {
                return;
            }
        }
        boolean dynamic = custom.isDynamic();
        logger.info("Custom dynamic = {}", dynamic);
        if (custom.getBid() > 0 && custom.getBgroup() != null) {
            customInstance = initOrCreateCustomInstance(custom.getBid(), custom.getBgroup());
        }
    }

    private AsyncCamelliaRedisTemplate initOrCreateCustomInstance(long bid, String bgroup) {
        CamelliaTranspondProperties.CustomProperties custom = properties.getCustom();
        if (custom == null) return null;
        String key = bid + "|" + bgroup;
        AsyncCamelliaRedisTemplate template = customInstanceMap.get(key);
        if (template == null) {
            synchronized (lockMap.getLockObj(key)) {
                template = customInstanceMap.get(key);
                if (template == null) {
                    ProxyRouteConfUpdater updater = custom.getProxyRouteConfUpdater();
                    template = new AsyncCamelliaRedisTemplate(env, bid, bgroup, updater, custom.getReloadIntervalMillis());
                    //更新的callback和删除的callback
                    ResourceTableUpdateCallback updateCallback = template.getUpdateCallback();
                    ResourceTableRemoveCallback templateRemoveCallback = template.getRemoveCallback();
                    ResourceTableRemoveCallback removeCallback = () -> {
                        customInstanceMap.remove(key);
                        templateRemoveCallback.callback();
                        Set<ChannelInfo> channelMap = ChannelMonitor.getChannelMap(bid, bgroup);
                        int size = channelMap.size();
                        for (ChannelInfo channelInfo : channelMap) {
                            channelInfo.getCtx().close();
                        }
                        logger.info("force close client connect for resourceTable remove, bid = {}, bgroup = {}, count = {}", bid, bgroup, size);
                    };
                    updater.addCallback(bid, bgroup, updateCallback, removeCallback);
                    customInstanceMap.put(key, template);
                    logger.info("AsyncCamelliaRedisTemplate init, bid = {}, bgroup = {}", bid, bgroup);
                }
            }
        }
        return template;
    }

    private AsyncCamelliaRedisTemplate initOrCreateRemoteInstance(long bid, String bgroup) {
        if (apiService == null) return null;
        String key = bid + "|" + bgroup;
        AsyncCamelliaRedisTemplate template = remoteInstanceMap.get(key);
        if (template == null) {
            synchronized (lockMap.getLockObj(key)) {
                template = remoteInstanceMap.get(key);
                if (template == null) {
                    boolean monitorEnable = properties.getRemote().isMonitorEnable();
                    long checkIntervalMillis = properties.getRemote().getCheckIntervalMillis();
                    template = new AsyncCamelliaRedisTemplate(env, apiService, bid, bgroup, monitorEnable, checkIntervalMillis);
                    remoteInstanceMap.put(key, template);
                    logger.info("AsyncCamelliaRedisTemplate init, bid = {}, bgroup = {}", bid, bgroup);
                }
            }
        }
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
        RedisClientHub.eventLoopGroup = new NioEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client"));
        RedisClientHub.eventLoopGroupBackup = new NioEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("camellia-redis-client-backup"));
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
        logger.info("RedisClient, so_keepalive = {}, tcp_no_delay = {}, so_rcvbuf = {}, so_sndbuf = {}, write_buffer_water_mark_Low = {}, write_buffer_water_mark_high = {}",
                RedisClientHub.soKeepalive, RedisClientHub.tcpNoDelay, RedisClientHub.soRcvbuf,
                RedisClientHub.soSndbuf, RedisClientHub.writeBufferWaterMarkLow, RedisClientHub.writeBufferWaterMarkHigh);

        RedisClientHub.initDynamicConf();

        ProxyEnv.Builder builder = new ProxyEnv.Builder();
        ShardingFunc shardingFunc  = redisConf.getShardingFuncInstance();
        if (shardingFunc != null) {
            builder.shardingFunc(shardingFunc);
            logger.info("ShardingFunc, className = {}", shardingFunc.getClass().getName());
        } else {
            String className = redisConf.getShardingFunc();
            if (className != null) {
                shardingFunc = ShardingFuncUtil.forName(className);
                builder.shardingFunc(shardingFunc);
                logger.info("ShardingFunc, className = {}", className);
            }
        }
        logger.info("multi write mode = {}", redisConf.getMultiWriteMode());
        ProxyEnv proxyEnv = builder.build();

        env = new AsyncCamelliaRedisEnv.Builder()
                .proxyEnv(proxyEnv)
                .clientFactory(clientFactory)
                .multiWriteMode(redisConf.getMultiWriteMode())
                .build();
    }
}
