package com.netease.nim.camellia.redis.proxy.upstream.sentinel;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.upstream.standalone.AsyncCamelliaSimpleClient;
import com.netease.nim.camellia.redis.proxy.upstream.utils.HostAndPort;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientAddr;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientHub;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.resource.RedisSentinelResource;
import com.netease.nim.camellia.redis.resource.RedisSentinelSlavesResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * Created by caojiajun on 2021/4/8
 */
public class AsyncCamelliaRedisSentinelSlavesClient extends AsyncCamelliaSimpleClient {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCamelliaRedisSentinelSlavesClient.class);

    private final Object lock = new Object();

    private final RedisSentinelSlavesResource redisSentinelSlavesResource;
    private RedisClientAddr master;
    private List<RedisClientAddr> slaves;

    public AsyncCamelliaRedisSentinelSlavesClient(RedisSentinelSlavesResource redisSentinelSlavesResource) {
        this.redisSentinelSlavesResource = redisSentinelSlavesResource;
        boolean sentinelAvailable = false;
        if (redisSentinelSlavesResource.isWithMaster()) {
            for (RedisSentinelResource.Node node : redisSentinelSlavesResource.getNodes()) {
                RedisSentinelMasterResponse masterResponse = RedisSentinelUtils.getMasterAddr(node.getHost(), node.getPort(), redisSentinelSlavesResource.getMaster());
                if (masterResponse.isSentinelAvailable()) {
                    sentinelAvailable = true;
                }
                if (masterResponse.getMaster() != null) {
                    this.master = new RedisClientAddr(masterResponse.getMaster().getHost(), masterResponse.getMaster().getPort(),
                            redisSentinelSlavesResource.getUserName(), redisSentinelSlavesResource.getPassword(), redisSentinelSlavesResource.getDb());
                    logger.info("redis-sentinel-slaves init, url = {}, master = {}", PasswordMaskUtils.maskResource(redisSentinelSlavesResource.getUrl()), PasswordMaskUtils.maskAddr(this.master));
                    break;
                }
            }
        }
        for (RedisSentinelResource.Node node : redisSentinelSlavesResource.getNodes()) {
            RedisSentinelSlavesResponse slavesResponse = RedisSentinelUtils.getSlaveAddrs(node.getHost(), node.getPort(), redisSentinelSlavesResource.getMaster());
            if (slavesResponse.isSentinelAvailable()) {
                sentinelAvailable = true;
            }
            if (slavesResponse.getSlaves() != null) {
                List<RedisClientAddr> slaves = new ArrayList<>();
                for (HostAndPort slave : slavesResponse.getSlaves()) {
                    slaves.add(new RedisClientAddr(slave.getHost(), slave.getPort(), redisSentinelSlavesResource.getUserName(),
                            redisSentinelSlavesResource.getPassword(), redisSentinelSlavesResource.getDb()));
                }
                this.slaves = slaves;
                logger.info("redis-sentinel-slaves init, url = {}, slaves = {}", PasswordMaskUtils.maskResource(redisSentinelSlavesResource.getUrl()), PasswordMaskUtils.maskAddrs(slaves));
                break;
            }
        }
        if (master == null && (slaves == null || slaves.isEmpty())) {
            if (sentinelAvailable) {
                if (redisSentinelSlavesResource.isWithMaster()) {
                    throw new CamelliaRedisException("can connect to sentinel, but cannot found master/slaves node");
                } else {
                    throw new CamelliaRedisException("can connect to sentinel, but cannot found slaves node");
                }
            } else {
                throw new CamelliaRedisException("all sentinels down");
            }
        }

        for (RedisSentinelResource.Node node : redisSentinelSlavesResource.getNodes()) {
            if (redisSentinelSlavesResource.isWithMaster()) {
                RedisSentinelMasterListener.MasterUpdateCallback masterUpdateCallback = master -> {
                    synchronized (lock) {
                        try {
                            RedisClientAddr oldMaster = AsyncCamelliaRedisSentinelSlavesClient.this.master;
                            if (master == null) {
                                if (oldMaster != null) {
                                    AsyncCamelliaRedisSentinelSlavesClient.this.master = null;
                                    logger.info("master update, url = {}, newMaster = {}, oldMaster = {}", PasswordMaskUtils.maskResource(getResource().getUrl()), null, PasswordMaskUtils.maskAddr(oldMaster));
                                }
                            } else {
                                String password = AsyncCamelliaRedisSentinelSlavesClient.this.redisSentinelSlavesResource.getPassword();
                                RedisClientAddr newMaster = new RedisClientAddr(master.getHost(), master.getPort(), redisSentinelSlavesResource.getUserName(), password, redisSentinelSlavesResource.getDb());

                                boolean needUpdate = false;
                                if (oldMaster == null) {
                                    needUpdate = true;
                                } else if (!newMaster.getUrl().equals(oldMaster.getUrl())) {
                                    needUpdate = true;
                                }
                                if (needUpdate) {
                                    AsyncCamelliaRedisSentinelSlavesClient.this.master = newMaster;
                                    logger.info("master update, url = {}, newMaster = {}, oldMaster = {}",
                                            PasswordMaskUtils.maskResource(getResource().getUrl()), PasswordMaskUtils.maskAddr(newMaster), PasswordMaskUtils.maskAddr(oldMaster));
                                }
                            }
                        } catch (Exception e) {
                            logger.error("MasterUpdateCallback error, url = {}", PasswordMaskUtils.maskResource(getResource().getUrl()), e);
                        }
                    }
                };
                RedisSentinelMasterListener masterListener = new RedisSentinelMasterListener(redisSentinelSlavesResource, new HostAndPort(node.getHost(), node.getPort()),
                        redisSentinelSlavesResource.getMaster(), masterUpdateCallback);
                masterListener.setDaemon(true);
                masterListener.start();
            }

            RedisSentinelSlavesListener.SlavesUpdateCallback slavesUpdateCallback = slaves -> {
                synchronized (lock) {
                    try {
                        if (slaves == null) {
                            slaves = new ArrayList<>();
                        }
                        String password = AsyncCamelliaRedisSentinelSlavesClient.this.redisSentinelSlavesResource.getPassword();
                        List<RedisClientAddr> newSlaves = new ArrayList<>();
                        for (HostAndPort slave : slaves) {
                            newSlaves.add(new RedisClientAddr(slave.getHost(), slave.getPort(), redisSentinelSlavesResource.getUserName(), password, redisSentinelSlavesResource.getDb()));
                        }
                        List<RedisClientAddr> oldSlaves = AsyncCamelliaRedisSentinelSlavesClient.this.slaves;
                        boolean needUpdate = false;
                        if (oldSlaves == null) {
                            needUpdate = true;
                        } else if (oldSlaves.size() != newSlaves.size()) {
                            needUpdate = true;
                        } else {
                            List<String> newStr = new ArrayList<>();
                            for (RedisClientAddr newSlave : newSlaves) {
                                newStr.add(newSlave.getUrl());
                            }
                            Collections.sort(newStr);
                            List<String> oldStr = new ArrayList<>();
                            for (RedisClientAddr oldSlave : oldSlaves) {
                                oldStr.add(oldSlave.getUrl());
                            }
                            Collections.sort(oldStr);
                            if (!newStr.toString().equals(oldStr.toString())) {
                                needUpdate = true;
                            }
                        }
                        if (needUpdate) {
                            AsyncCamelliaRedisSentinelSlavesClient.this.slaves = newSlaves;
                            logger.info("slaves update, url = {}, newSlaves = {}, oldSlaves = {}",
                                    PasswordMaskUtils.maskResource(getResource().getUrl()), PasswordMaskUtils.maskAddrs(newSlaves), PasswordMaskUtils.maskAddrs(oldSlaves));
                        }
                    } catch (Exception e) {
                        logger.error("SlavesUpdateCallback error, url = {}", PasswordMaskUtils.maskResource(getResource().getUrl()), e);
                    }
                }
            };
            RedisSentinelSlavesListener listener = new RedisSentinelSlavesListener(redisSentinelSlavesResource, new HostAndPort(node.getHost(), node.getPort()),
                    redisSentinelSlavesResource.getMaster(), slavesUpdateCallback);
            listener.setDaemon(true);
            listener.start();
        }
    }

    @Override
    public void preheat() {
        logger.info("try preheat, url = {}", PasswordMaskUtils.maskResource(getResource().getUrl()));
        if (master != null) {
            logger.info("try preheat, url = {}, master = {}", PasswordMaskUtils.maskResource(getResource().getUrl()), PasswordMaskUtils.maskAddr(master));
            boolean result = RedisClientHub.preheat(master.getHost(), master.getPort(), master.getUserName(), master.getPassword(), master.getDb());
            logger.info("preheat result = {}, url = {}, master = {}", result, PasswordMaskUtils.maskResource(getResource().getUrl()), PasswordMaskUtils.maskAddr(master));
        }
        if (slaves != null) {
            for (RedisClientAddr slave : slaves) {
                logger.info("try preheat, url = {}, slave = {}", PasswordMaskUtils.maskResource(getResource().getUrl()), PasswordMaskUtils.maskAddr(slave));
                boolean result = RedisClientHub.preheat(slave.getHost(), slave.getPort(), slave.getUserName(), slave.getPassword(), slave.getDb());
                logger.info("preheat result = {}, url = {}, slave = {}", result, PasswordMaskUtils.maskResource(getResource().getUrl()), PasswordMaskUtils.maskAddr(slave));
            }
        }
        logger.info("preheat success, url = {}", PasswordMaskUtils.maskResource(getResource().getUrl()));
    }

    @Override
    public RedisClientAddr getAddr() {
        int retry = 3;
        while (retry > 0) {
            retry --;
            try {
                if (master != null) {
                    if (slaves.isEmpty()) return master;
                    int index = ThreadLocalRandom.current().nextInt(slaves.size() + 1);
                    if (index == 0) {
                        return master;
                    }
                    return slaves.get(index - 1);
                } else {
                    if (slaves.isEmpty()) return null;
                    if (slaves.size() == 1) return slaves.get(0);
                    int index = ThreadLocalRandom.current().nextInt(slaves.size());
                    return slaves.get(index);
                }
            } catch (Exception e) {
                ErrorLogCollector.collect(AsyncCamelliaRedisSentinelSlavesClient.class, "getAddr error, url = " + PasswordMaskUtils.maskResource(redisSentinelSlavesResource.getUrl()), e);
            }
        }
        return null;
    }

    @Override
    public Resource getResource() {
        return redisSentinelSlavesResource;
    }
}
