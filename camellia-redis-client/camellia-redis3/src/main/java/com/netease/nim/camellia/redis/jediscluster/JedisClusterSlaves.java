package com.netease.nim.camellia.redis.jediscluster;

import com.netease.nim.camellia.redis.base.resource.RedisClusterSlavesResource;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by caojiajun on 2024/11/12
 */
public class JedisClusterSlaves {

    private static final Logger logger = LoggerFactory.getLogger(JedisClusterSlaves.class);

    private final JedisClusterWrapper jedisCluster;
    private final RedisClusterSlavesResource resource;
    private final ScheduledExecutorService scheduledExecutorService;

    private final GenericObjectPoolConfig poolConfig;
    private final int connectionTimeout;
    private final int soTimeout;

    private final ReentrantLock lock = new ReentrantLock();

    private boolean renewing = false;
    private Map<Integer, List<String>> slotMap = new HashMap<>();
    private Set<String> slaves = new HashSet<>();

    private Map<String, ReadOnlyJedisPool> poolMap = new HashMap<>();

    public JedisClusterSlaves(RedisClusterSlavesResource resource, GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout,
                              JedisClusterWrapper jedisCluster, ScheduledExecutorService scheduledExecutorService, int redisClusterSlaveRenewIntervalSeconds) {
        this.poolConfig = poolConfig;
        this.connectionTimeout = connectionTimeout;
        this.soTimeout = soTimeout;
        this.jedisCluster = jedisCluster;
        this.resource = resource;
        renew();
        this.scheduledExecutorService = scheduledExecutorService;
        scheduledExecutorService.scheduleAtFixedRate(this::renew, redisClusterSlaveRenewIntervalSeconds, redisClusterSlaveRenewIntervalSeconds, TimeUnit.SECONDS);
    }

    public void renew() {
        if (renewing) {
            return;
        }
        lock.lock();
        try {
            if (renewing) {
                return;
            }
            renewing = true;
            try {
                Map<Integer, List<HostAndPort>> map = JedisClusterSlaveUtils.clusterSlotsSlaveNodes(jedisCluster);
                if (map == null) {
                    return;
                }
                Map<String, ReadOnlyJedisPool> newPoolMap = new HashMap<>();
                Map<Integer, List<String>> newSlotMap = new HashMap<>();
                Set<String> newSlaves = new HashSet<>();
                for (Map.Entry<Integer, List<HostAndPort>> entry : map.entrySet()) {
                    Integer slot = entry.getKey();
                    List<HostAndPort> list = entry.getValue();
                    if (!list.isEmpty()) {
                        List<String> slaves = new ArrayList<>();
                        for (HostAndPort hostAndPort : list) {
                            String key = hostAndPort.toString();
                            ReadOnlyJedisPool pool = poolMap.get(key);
                            if (pool == null) {
                                pool = newPoolMap.get(key);
                            }
                            if (pool == null) {
                                pool = new ReadOnlyJedisPool(poolConfig, hostAndPort.getHost(), hostAndPort.getPort(),
                                        connectionTimeout, soTimeout, resource.getUserName(), resource.getPassword(),
                                        0, null, false, null, null, null);
                                newPoolMap.put(key, pool);
                            } else {
                                newPoolMap.put(key, pool);
                            }
                            slaves.add(key);
                        }
                        newSlotMap.put(slot, slaves);
                        newSlaves.addAll(slaves);
                    }
                }
                this.slotMap = newSlotMap;
                this.slaves = newSlaves;
                Map<String, ReadOnlyJedisPool> oldPoolMap = this.poolMap;
                this.poolMap = newPoolMap;
                for (String slave : slaves) {
                    oldPoolMap.remove(slave);
                }
                if (!oldPoolMap.isEmpty()) {
                    for (Map.Entry<String, ReadOnlyJedisPool> entry : oldPoolMap.entrySet()) {
                        scheduledExecutorService.schedule(() -> entry.getValue().close(), 60, TimeUnit.SECONDS);
                    }
                }
            } finally {
                renewing = false;
            }
        } catch (Throwable e) {
            logger.error("renew error, url = {}", resource, e);
        } finally {
            lock.unlock();
        }
    }

    public ReadOnlyJedisPool getPool(int slot) {
        boolean withMaster = resource.isWithMaster();
        List<String> slaves = slotMap.get(slot);
        if (slaves == null || slaves.isEmpty()) {
            return null;
        }
        if (withMaster) {
            int index = ThreadLocalRandom.current().nextInt(slaves.size() + 1);
            if (index == 0) {
                return null;//master
            }
            String key = slaves.get(index - 1);
            return poolMap.get(key);
        } else {
            if (slaves.size() == 1) {
                String key = slaves.get(0);
                return poolMap.get(key);
            }
            int index = ThreadLocalRandom.current().nextInt(slaves.size());
            String key = slaves.get(index);
            return poolMap.get(key);
        }
    }

    public String getSlaveNode(int slot) {
        boolean withMaster = resource.isWithMaster();
        List<String> slaves = slotMap.get(slot);
        if (slaves == null || slaves.isEmpty()) {
            return null;
        }
        if (withMaster) {
            int index = ThreadLocalRandom.current().nextInt(slaves.size() + 1);
            if (index == 0) {
                return null;//master
            }
            return slaves.get(index - 1);
        } else {
            if (slaves.size() == 1) {
                return slaves.get(0);
            }
            int index = ThreadLocalRandom.current().nextInt(slaves.size());
            return slaves.get(index);
        }

    }

    public ReadOnlyJedisPool getPool(String slaveNode) {
        return poolMap.get(slaveNode);
    }

    public Set<String> getSlaves() {
        return slaves;
    }
}
