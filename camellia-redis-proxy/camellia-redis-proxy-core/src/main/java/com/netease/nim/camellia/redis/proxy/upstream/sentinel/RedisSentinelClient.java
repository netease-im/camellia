package com.netease.nim.camellia.redis.proxy.upstream.sentinel;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.base.resource.RedisSentinelResource;
import com.netease.nim.camellia.redis.proxy.upstream.standalone.AbstractSimpleRedisClient;
import com.netease.nim.camellia.redis.proxy.upstream.utils.HostAndPort;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 *
 * Created by caojiajun on 2020/08/07.
 */
public class RedisSentinelClient extends AbstractSimpleRedisClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisSentinelClient.class);

    private final RedisSentinelResource redisSentinelResource;
    private volatile RedisConnectionAddr redisConnectionAddr;
    private final Object lock = new Object();

    public RedisSentinelClient(RedisSentinelResource redisSentinelResource) {
        this.redisSentinelResource = redisSentinelResource;
        String master = redisSentinelResource.getMaster();
        boolean sentinelAvailable = false;
        for (RedisSentinelResource.Node node : redisSentinelResource.getNodes()) {
            RedisSentinelMasterResponse redisSentinelMasterResponse = RedisSentinelUtils.getMasterAddr(node.getHost(), node.getPort(), master);
            if (redisSentinelMasterResponse.isSentinelAvailable()) {
                sentinelAvailable = redisSentinelMasterResponse.isSentinelAvailable();
            }
            if (redisSentinelMasterResponse.getMaster() != null) {
                HostAndPort hostAndPort = redisSentinelMasterResponse.getMaster();
                redisConnectionAddr = new RedisConnectionAddr(hostAndPort.getHost(), hostAndPort.getPort(), redisSentinelResource.getUserName(), redisSentinelResource.getPassword(), redisSentinelResource.getDb());
                logger.info("redis sentinel init, url = {}, master = {}", PasswordMaskUtils.maskResource(redisSentinelResource.getUrl()), PasswordMaskUtils.maskAddr(redisConnectionAddr));
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
        for (RedisSentinelResource.Node node : redisSentinelResource.getNodes()) {
            RedisSentinelMasterListener.MasterUpdateCallback callback = m -> {
                if (m == null) return;
                synchronized (lock) {
                    RedisConnectionAddr newNode = new RedisConnectionAddr(m.getHost(), m.getPort(), redisSentinelResource.getUserName(), redisSentinelResource.getPassword(), redisSentinelResource.getDb());
                    RedisConnectionAddr oldNode = redisConnectionAddr;
                    if (!Objects.equals(newNode.getUrl(), oldNode.getUrl())) {
                        redisConnectionAddr = newNode;
                        logger.info("sentinel redis master node update, resource = {}, oldMaster = {}, newMaster = {}",
                                PasswordMaskUtils.maskResource(redisSentinelResource.getUrl()), PasswordMaskUtils.maskAddr(oldNode), PasswordMaskUtils.maskAddr(newNode));
                    }
                }
            };
            RedisSentinelMasterListener masterListener = new RedisSentinelMasterListener(redisSentinelResource, new HostAndPort(node.getHost(), node.getPort()), master, callback);
            masterListener.setDaemon(true);
            masterListener.start();
        }
    }

    @Override
    public RedisConnectionAddr getAddr() {
        return redisConnectionAddr;
    }

    @Override
    public Resource getResource() {
        return redisSentinelResource;
    }
}
