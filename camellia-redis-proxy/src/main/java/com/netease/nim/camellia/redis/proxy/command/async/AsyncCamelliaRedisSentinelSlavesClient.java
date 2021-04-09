package com.netease.nim.camellia.redis.proxy.command.async;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.async.sentinel.*;
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
                    this.master = new RedisClientAddr(masterResponse.getMaster().getHost(), masterResponse.getMaster().getPort(), redisSentinelSlavesResource.getPassword());
                    logger.info("redis-sentinel-slaves init, url = {}, master = {}", redisSentinelSlavesResource.getUrl(), this.master);
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
                    slaves.add(new RedisClientAddr(slave.getHost(), slave.getPort(), redisSentinelSlavesResource.getPassword()));
                }
                this.slaves = slaves;
                logger.info("redis-sentinel-slaves init, url = {}, slaves = {}", redisSentinelSlavesResource.getUrl(), this.slaves);
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
                                    logger.info("master update, url = {}, newMaster = {}, oldMaster = {}", getResource().getUrl(), null, oldMaster);
                                    AsyncCamelliaRedisSentinelSlavesClient.this.master = null;
                                }
                            } else {
                                String password = AsyncCamelliaRedisSentinelSlavesClient.this.redisSentinelSlavesResource.getPassword();
                                RedisClientAddr newMaster = new RedisClientAddr(master.getHost(), master.getPort(), password);

                                boolean needUpdate = false;
                                if (oldMaster == null) {
                                    needUpdate = true;
                                } else if (!newMaster.getUrl().equals(oldMaster.getUrl())) {
                                    needUpdate = true;
                                }
                                if (needUpdate) {
                                    logger.info("master update, url = {}, newMaster = {}, oldMaster = {}", getResource().getUrl(), newMaster, oldMaster);
                                    AsyncCamelliaRedisSentinelSlavesClient.this.master = newMaster;
                                }
                            }
                        } catch (Exception e) {
                            logger.error("MasterUpdateCallback error, url = {}", redisSentinelSlavesResource.getUrl(), e);
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
                            newSlaves.add(new RedisClientAddr(slave.getHost(), slave.getPort(), password));
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
                            logger.info("slaves update, url = {}, newSlaves = {}, oldSlaves = {}", getResource().getUrl(), newSlaves, oldSlaves);
                            AsyncCamelliaRedisSentinelSlavesClient.this.slaves = newSlaves;
                        }
                    } catch (Exception e) {
                        logger.error("SlavesUpdateCallback error, url = {}", getResource().getUrl(), e);
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
        logger.info("try preheat, url = {}", getResource().getUrl());
        if (master != null) {
            logger.info("try preheat, url = {}, master = {}", getResource().getUrl(), master.getUrl());
            boolean result = RedisClientHub.preheat(master.getHost(), master.getPort(), master.getPassword());
            logger.info("preheat result = {}, url = {}, master = {}", result, getResource().getUrl(), master.getUrl());
        }
        if (slaves != null) {
            for (RedisClientAddr slave : slaves) {
                logger.info("try preheat, url = {}, slave = {}", getResource().getUrl(), slave.getUrl());
                boolean result = RedisClientHub.preheat(slave.getHost(), slave.getPort(), slave.getPassword());
                logger.info("preheat result = {}, url = {}, slave = {}", result, getResource().getUrl(), slave.getUrl());
            }
        }
        logger.info("preheat success, url = {}", getResource().getUrl());
    }

    @Override
    public RedisClientAddr getAddr() {
        int retry = 3;
        while (retry > 0) {
            retry --;
            try {
                if (master != null) {
                    int index = ThreadLocalRandom.current().nextInt(slaves.size() + 1);
                    if (index == 0) {
                        return master;
                    }
                    return slaves.get(index - 1);
                } else {
                    if (slaves.isEmpty()) return null;
                    int index = ThreadLocalRandom.current().nextInt(slaves.size());
                    return slaves.get(index);
                }
            } catch (Exception e) {
                ErrorLogCollector.collect(AsyncCamelliaRedisSentinelSlavesClient.class, "getAddr error, url = " + redisSentinelSlavesResource.getUrl(), e);
            }
        }
        return null;
    }

    @Override
    public Resource getResource() {
        return redisSentinelSlavesResource;
    }
}
