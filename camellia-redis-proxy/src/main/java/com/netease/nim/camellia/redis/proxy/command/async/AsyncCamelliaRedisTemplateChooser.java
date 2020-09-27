package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiUtil;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.env.ShadingFunc;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ShadingFuncUtil;
import com.netease.nim.camellia.core.util.SysUtils;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class AsyncCamelliaRedisTemplateChooser {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCamelliaRedisTemplateChooser.class);

    private final CamelliaTranspondProperties properties;
    private AsyncCamelliaRedisEnv env;
    private CamelliaApi apiService;

    private final Object lock = new Object();
    private AsyncCamelliaRedisTemplate remoteInstance;
    private AsyncCamelliaRedisTemplate localInstance;
    private final Map<String, AsyncCamelliaRedisTemplate> remoteInstanceMap = new HashMap<>();

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
        } else if (type == CamelliaTranspondProperties.Type.AUTO) {
            if (bid == null || bid <= 0 || bgroup == null) {
                if (localInstance != null) return localInstance;
                if (remoteInstance != null) return remoteInstance;
                logger.warn("no bid/bgroup, return null");
                return null;
            }
            return initOrCreateRemoteInstance(bid, bgroup);
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
        } else if (type == CamelliaTranspondProperties.Type.AUTO) {
            initLocal(false);
            initRemote(false);
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
        if (resourceTable == null) {
            if (throwError) {
                throw new IllegalArgumentException("local.resourceTable is null");
            } else {
                return;
            }
        }
        localInstance = new AsyncCamelliaRedisTemplate(env, resourceTable);
    }

    private AsyncCamelliaRedisTemplate initOrCreateRemoteInstance(long bid, String bgroup) {
        if (apiService == null) return null;
        String key = bid + "|" + bgroup;
        AsyncCamelliaRedisTemplate template = remoteInstanceMap.get(key);
        if (template == null) {
            synchronized (lock) {
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
        RedisClientHub.eventLoopGroup = new NioEventLoopGroup(redisConf.getDefaultTranspondWorkThread(), new DefaultThreadFactory("redis-client"));
        logger.info("RedisClient, failCountThreshold = {}, failBanMillis = {}",
                RedisClientHub.failCountThreshold, RedisClientHub.failBanMillis);

        ProxyEnv.Builder builder = new ProxyEnv.Builder();
        String className = redisConf.getShadingFunc();
        if (className != null) {
            ShadingFunc shadingFunc = ShadingFuncUtil.forName(className);
            builder.shadingFunc(shadingFunc);
            logger.info("ShadingFunc, className = {}", className);
        }
        ProxyEnv proxyEnv = builder.build();

        env = new AsyncCamelliaRedisEnv.Builder()
                .proxyEnv(proxyEnv)
                .clientFactory(clientFactory)
                .multiWriteType(redisConf.getMultiWriteType())
                .build();
    }
}
