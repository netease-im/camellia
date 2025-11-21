package com.netease.nim.camellia.redis.proxy.upstream.sentinel;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.base.resource.RedisSentinelResource;
import com.netease.nim.camellia.redis.base.resource.RedissSentinelResource;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionStatus;
import com.netease.nim.camellia.redis.proxy.upstream.standalone.AbstractSimpleRedisClient;
import com.netease.nim.camellia.redis.proxy.upstream.utils.HostAndPort;
import com.netease.nim.camellia.redis.proxy.upstream.utils.Renew;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * Created by caojiajun on 2020/08/07.
 */
public class RedisSentinelClient extends AbstractSimpleRedisClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisSentinelClient.class);

    private final AtomicInteger renewIndex = new AtomicInteger(0);

    private final Resource resource;
    private final Resource sentinelResource;
    private final String master;
    private final String userName;
    private final String password;
    private final int db;
    private final String sentinelUserName;
    private final String sentinelPassword;
    private final List<RedisSentinelResource.Node> nodes;
    private volatile RedisConnectionAddr redisConnectionAddr;
    private final List<RedisSentinelMasterListener> masterListenerList = new ArrayList<>();
    private final Object lock = new Object();
    private Renew renew;

    public RedisSentinelClient(RedissSentinelResource resource) {
        this.resource = resource;
        this.sentinelResource = RedisSentinelUtils.parseSentinelResource(resource);
        this.master = resource.getMaster();
        this.userName = resource.getUserName();
        this.password = resource.getPassword();
        this.db = resource.getDb();
        this.sentinelUserName = resource.getSentinelUserName();
        this.sentinelPassword = resource.getSentinelPassword();
        this.nodes = resource.getNodes();
    }

    public RedisSentinelClient(RedisSentinelResource resource) {
        this.resource = resource;
        this.sentinelResource = RedisSentinelUtils.parseSentinelResource(resource);
        this.master = resource.getMaster();
        this.userName = resource.getUserName();
        this.password = resource.getPassword();
        this.db = resource.getDb();
        this.sentinelUserName = resource.getSentinelUserName();
        this.sentinelPassword = resource.getSentinelPassword();
        this.nodes = resource.getNodes();
    }

    @Override
    public void start() {
        boolean sentinelAvailable = false;
        for (RedisSentinelResource.Node node : nodes) {
            RedisSentinelMasterResponse redisSentinelMasterResponse = RedisSentinelUtils.getMasterAddr(sentinelResource, node.getHost(), node.getPort(),
                    master, sentinelUserName, sentinelPassword);
            if (redisSentinelMasterResponse.sentinelAvailable()) {
                sentinelAvailable = true;
            }
            if (redisSentinelMasterResponse.master() != null) {
                HostAndPort hostAndPort = redisSentinelMasterResponse.master();
                redisConnectionAddr = new RedisConnectionAddr(hostAndPort.getHost(), hostAndPort.getPort(), userName, password, db);
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
        for (RedisSentinelResource.Node node : nodes) {
            RedisSentinelMasterListener masterListener = new RedisSentinelMasterListener(resource, new HostAndPort(node.getHost(), node.getPort()), master,
                    sentinelUserName, sentinelPassword, this::masterUpdate);
            masterListener.setDaemon(true);
            masterListener.start();
            masterListenerList.add(masterListener);
        }
        int intervalSeconds = ProxyDynamicConf.getInt("redis.sentinel.schedule.renew.interval.seconds", 600);
        renew = new Renew(resource, this::renew0, intervalSeconds);
        logger.info("RedisSentinelClient start success, resource = {}", PasswordMaskUtils.maskResource(getResource()));
    }

    private void masterUpdate(HostAndPort master) {
        if (master == null) return;
        synchronized (lock) {
            RedisConnectionAddr newNode = new RedisConnectionAddr(master.getHost(), master.getPort(), userName, password, db);
            RedisConnectionAddr oldNode = redisConnectionAddr;
            if (!Objects.equals(newNode.getUrl(), oldNode.getUrl())) {
                redisConnectionAddr = newNode;
                logger.info("sentinel redis master node update, resource = {}, oldMaster = {}, newMaster = {}",
                        PasswordMaskUtils.maskResource(resource.getUrl()), PasswordMaskUtils.maskAddr(oldNode), PasswordMaskUtils.maskAddr(newNode));
            }
        }
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
        renew.close();
        for (RedisSentinelMasterListener listener : masterListenerList) {
            listener.shutdown();
        }
        logger.warn("upstream client shutdown, resource = {}", PasswordMaskUtils.maskResource(getResource()));
    }

    @Override
    public void renew() {
        if (renew != null) {
            renew.renew();
        }
    }

    private void renew0() {
        try {
            if (!masterListenerList.isEmpty()) {
                int index = Math.abs(renewIndex.getAndIncrement()) % masterListenerList.size();
                RedisSentinelMasterListener masterListener = masterListenerList.get(index);
                masterListener.renew();
            }
        } catch (Exception e) {
            logger.error("redis sentinel renew error, resource = {}", PasswordMaskUtils.maskResource(resource.getUrl()), e);
        }
    }
}
