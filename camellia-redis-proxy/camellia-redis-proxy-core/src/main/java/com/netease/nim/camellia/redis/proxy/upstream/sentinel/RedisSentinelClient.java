package com.netease.nim.camellia.redis.proxy.upstream.sentinel;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.base.resource.RedisSentinelResource;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionStatus;
import com.netease.nim.camellia.redis.proxy.upstream.standalone.AbstractSimpleRedisClient;
import com.netease.nim.camellia.redis.proxy.upstream.utils.HostAndPort;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * Created by caojiajun on 2020/08/07.
 */
public class RedisSentinelClient extends AbstractSimpleRedisClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisSentinelClient.class);

    private final RedisSentinelResource resource;
    private volatile RedisConnectionAddr redisConnectionAddr;
    private final List<RedisSentinelMasterListener> masterListenerList = new ArrayList<>();
    private final Object lock = new Object();

    public RedisSentinelClient(RedisSentinelResource resource) {
        this.resource = resource;
        String master = resource.getMaster();
        boolean sentinelAvailable = false;
        for (RedisSentinelResource.Node node : resource.getNodes()) {
            RedisSentinelMasterResponse redisSentinelMasterResponse = RedisSentinelUtils.getMasterAddr(node.getHost(), node.getPort(),
                    master, resource.getSentinelUserName(), resource.getSentinelPassword());
            if (redisSentinelMasterResponse.isSentinelAvailable()) {
                sentinelAvailable = redisSentinelMasterResponse.isSentinelAvailable();
            }
            if (redisSentinelMasterResponse.getMaster() != null) {
                HostAndPort hostAndPort = redisSentinelMasterResponse.getMaster();
                redisConnectionAddr = new RedisConnectionAddr(hostAndPort.getHost(), hostAndPort.getPort(), resource.getUserName(), resource.getPassword(), resource.getDb());
                logger.info("redis sentinel init, url = {}, master = {}", PasswordMaskUtils.maskResource(resource.getUrl()), PasswordMaskUtils.maskAddr(redisConnectionAddr));
                break;
            }
        }
        if (redisConnectionAddr == null) {
            if (sentinelAvailable) {
                throw new CamelliaRedisException("can connect to sentinel, but " + master + " seems to be not monitored...");
            } else {
                throw new CamelliaRedisException("all sentinels down, cannot determine where is " + master + " master is running...");
            }
        }
        for (RedisSentinelResource.Node node : resource.getNodes()) {
            RedisSentinelMasterListener.MasterUpdateCallback callback = m -> {
                if (m == null) return;
                synchronized (lock) {
                    RedisConnectionAddr newNode = new RedisConnectionAddr(m.getHost(), m.getPort(), resource.getUserName(), resource.getPassword(), resource.getDb());
                    RedisConnectionAddr oldNode = redisConnectionAddr;
                    if (!Objects.equals(newNode.getUrl(), oldNode.getUrl())) {
                        redisConnectionAddr = newNode;
                        logger.info("sentinel redis master node update, resource = {}, oldMaster = {}, newMaster = {}",
                                PasswordMaskUtils.maskResource(resource.getUrl()), PasswordMaskUtils.maskAddr(oldNode), PasswordMaskUtils.maskAddr(newNode));
                    }
                }
            };
            RedisSentinelMasterListener masterListener = new RedisSentinelMasterListener(resource, new HostAndPort(node.getHost(), node.getPort()), master,
                    resource.getSentinelUserName(), resource.getSentinelPassword(), callback);
            masterListener.setDaemon(true);
            masterListener.start();
            masterListenerList.add(masterListener);
        }
        logger.info("RedisSentinelClient init success, resource = {}", resource.getUrl());
    }

    @Override
    public RedisConnectionAddr getAddr() {
        return redisConnectionAddr;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public boolean isValid() {
        return getStatus(getAddr()) == RedisConnectionStatus.VALID;
    }

    @Override
    public synchronized void shutdown() {
        for (RedisSentinelMasterListener listener : masterListenerList) {
            listener.shutdown();
        }
        logger.warn("upstream client shutdown, url = {}", getUrl());
    }
}
