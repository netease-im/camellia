package com.netease.nim.camellia.redis.proxy.command.sync;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiUtil;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.env.ShadingFunc;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ShadingFuncUtil;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.jedis.JedisPoolFactory;
import com.netease.nim.camellia.redis.jediscluster.JedisClusterFactory;
import com.netease.nim.camellia.redis.proxy.command.ClientCommandUtil;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.util.JedisRedisEnvUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPoolConfig;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
public class SyncCommandProcessorChooser {

    private static final Logger logger = LoggerFactory.getLogger(SyncCommandProcessorChooser.class);

    private final CamelliaTranspondProperties properties;
    private final Map<String, SyncCommandProcessor> remoteInstanceMap = new HashMap<>();
    private final Object lock = new Object();
    private SyncCommandProcessor remoteInstance;
    private SyncCommandProcessor localInstance;
    private CamelliaApi apiService;
    private CamelliaRedisEnv env;

    public SyncCommandProcessorChooser(CamelliaTranspondProperties properties) {
        this.properties = properties;
        init();
    }

    public SyncCommandProcessor choose(ChannelInfo channelInfo) {
        CamelliaTranspondProperties.Type type = properties.getType();
        if (type == CamelliaTranspondProperties.Type.LOCAL) {
            return localInstance;
        } else if (type == CamelliaTranspondProperties.Type.REMOTE) {
            CamelliaTranspondProperties.RemoteProperties remote = properties.getRemote();
            if (!remote.isDynamic()) {
                if (logger.isTraceEnabled()) {
                    logger.trace("sync, not dynamic, return default remoteInstance");
                }
                return remoteInstance;
            }
            Long bid = null;
            String bgroup = null;
            if (channelInfo != null) {
                bid = ClientCommandUtil.getBid(channelInfo);
                bgroup = ClientCommandUtil.getBgroup(channelInfo);
            }
            if (bid == null || bid <= 0 || bgroup == null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("sync, not dynamic, return default remoteInstance");
                }
                return remoteInstance;
            }
            return initOrCreateRemoteInstance(bid, bgroup);
        } else if (type == CamelliaTranspondProperties.Type.AUTO) {
            Long bid = null;
            String bgroup = null;
            if (channelInfo != null) {
                bid = ClientCommandUtil.getBid(channelInfo);
                bgroup = ClientCommandUtil.getBgroup(channelInfo);
            }
            if (bid == null || bid <= 0 || bgroup == null) {
                if (localInstance != null) return localInstance;
                if (remoteInstance != null) return remoteInstance;
                logger.warn("sync, no bid/bgroup, return null");
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
        env = JedisRedisEnvUtil.initEnv(properties.getRedisConf());
        if (type == CamelliaTranspondProperties.Type.LOCAL) {
            logger.info("CamelliaRedisProxy.sync init, type = {}", type);
            initLocal(true);
        } else if (type == CamelliaTranspondProperties.Type.REMOTE) {
            logger.info("CamelliaRedisProxy.sync init, type = {}", type);
            initRemote(true);
        } else if (type == CamelliaTranspondProperties.Type.AUTO) {
            logger.info("CamelliaRedisProxy.sync init, type = {}", type);
            initLocal(false);
            initRemote(false);
        }
    }

    private SyncCommandProcessor initOrCreateRemoteInstance(long bid, String bgroup) {
        if (apiService == null) return null;
        String key = bid + "|" + bgroup;
        SyncCommandProcessor processor = remoteInstanceMap.get(key);
        if (processor == null) {
            synchronized (lock) {
                processor = remoteInstanceMap.get(key);
                if (processor == null) {
                    boolean monitorEnable = properties.getRemote().isMonitorEnable();
                    long checkIntervalMillis = properties.getRemote().getCheckIntervalMillis();
                    CamelliaRedisTemplate template = new CamelliaRedisTemplate(env, apiService, bid, bgroup, monitorEnable, checkIntervalMillis);
                    processor = new SyncCommandProcessor(template);
                    remoteInstanceMap.put(key, processor);
                    logger.info("CamelliaCommandProcessor init, bid = {}, bgroup = {}", bid, bgroup);
                }
            }
        }
        return processor;
    }

    private void initRemote(boolean throwError) {
        CamelliaTranspondProperties.RemoteProperties remote = properties.getRemote();
        if (remote == null) {
            if (throwError) {
                throw new IllegalArgumentException("sync.remote is null");
            } else {
                return;
            }
        }
        String url = remote.getUrl();
        if (url == null) {
            if (throwError) {
                throw new IllegalArgumentException("sync.remote.url is null");
            } else {
                return;
            }
        }
        apiService = CamelliaApiUtil.init(url, remote.getConnectTimeoutMillis(), remote.getReadTimeoutMillis());
        logger.info("ApiService init, sync.url = {}", url);
        boolean dynamic = remote.isDynamic();
        logger.info("sync.Remote dynamic = {}", dynamic);
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
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(env, resourceTable);
        localInstance = new SyncCommandProcessor(template);
    }
}
