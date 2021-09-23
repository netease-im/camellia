package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.async.sentinel.RedisSentinelMasterListener;
import com.netease.nim.camellia.redis.proxy.command.async.sentinel.RedisSentinelMasterResponse;
import com.netease.nim.camellia.redis.proxy.command.async.sentinel.RedisSentinelUtils;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.resource.RedisSentinelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 *
 * Created by caojiajun on 2020/08/07.
 */
public class AsyncCamelliaRedisSentinelClient extends AsyncCamelliaSimpleClient {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCamelliaRedisSentinelClient.class);

    private final RedisSentinelResource redisSentinelResource;
    private volatile RedisClientAddr redisClientAddr;
    private final Object lock = new Object();

    public AsyncCamelliaRedisSentinelClient(RedisSentinelResource redisSentinelResource) {
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
                redisClientAddr = new RedisClientAddr(hostAndPort.getHost(), hostAndPort.getPort(), redisSentinelResource.getUserName(), redisSentinelResource.getPassword());
                logger.info("redis sentinel init, url = {}, master = {}", PasswordMaskUtils.maskResource(redisSentinelResource.getUrl()), PasswordMaskUtils.maskAddr(redisClientAddr));
                break;
            }
        }
        if (redisClientAddr == null) {
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
                    RedisClientAddr newNode = new RedisClientAddr(m.getHost(), m.getPort(), redisSentinelResource.getUserName(), redisSentinelResource.getPassword());
                    RedisClientAddr oldNode = redisClientAddr;
                    if (!Objects.equals(newNode.getUrl(), oldNode.getUrl())) {
                        redisClientAddr = newNode;
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
    public RedisClientAddr getAddr() {
        return redisClientAddr;
    }

    @Override
    public Resource getResource() {
        return redisSentinelResource;
    }
}
