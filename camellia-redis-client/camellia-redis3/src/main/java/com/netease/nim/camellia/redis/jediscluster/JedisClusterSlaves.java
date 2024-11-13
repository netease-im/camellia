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
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by caojiajun on 2024/11/12
 */
public class JedisClusterSlaves {

    private static final Logger logger = LoggerFactory.getLogger(JedisClusterSlaves.class);

    private final JedisClusterWrapper jedisCluster;
    private final RedisClusterSlavesResource resource;

    private final GenericObjectPoolConfig poolConfig;
    private final int connectionTimeout;
    private final int soTimeout;

    private boolean renewing = false;
    private Map<Integer, List<String>> slotMap = new HashMap<>();
    private Set<String> slaves = new HashSet<>();

    private final Map<String, ReadOnlyJedisPool> poolMap = new HashMap<>();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();


    public JedisClusterSlaves(RedisClusterSlavesResource resource, GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout,
                              JedisClusterWrapper jedisCluster, ScheduledExecutorService scheduledExecutorService, int redisClusterSlaveRenewIntervalSeconds) {
        this.poolConfig = poolConfig;
        this.connectionTimeout = connectionTimeout;
        this.soTimeout = soTimeout;
        this.jedisCluster = jedisCluster;
        this.resource = resource;
        renew();
        scheduledExecutorService.scheduleAtFixedRate(this::renew, redisClusterSlaveRenewIntervalSeconds, redisClusterSlaveRenewIntervalSeconds, TimeUnit.SECONDS);
    }

    public void renew() {
        if (renewing) {
            return;
        }
        writeLock.lock();
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
                                pool = new ReadOnlyJedisPool(poolConfig, hostAndPort.getHost(), hostAndPort.getPort(),
                                        connectionTimeout, soTimeout, resource.getPassword(), 0, null, false, null, null, null);
                                poolMap.put(key, pool);
                            }
                            slaves.add(key);
                        }
                        newSlotMap.put(slot, slaves);
                        newSlaves.addAll(slaves);
                    }
                }
                this.slotMap = newSlotMap;
                this.slaves = newSlaves;
            } finally {
                renewing = false;
            }
        } catch (Throwable e) {
            logger.error("renew error, url = {}", resource, e);
        } finally {
            writeLock.unlock();
        }
    }

    public ReadOnlyJedisPool getPool(int slot) {
        readLock.lock();
        try {
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
        } finally {
            readLock.unlock();
        }
    }

    public String getSlaveNode(int slot) {
        readLock.lock();
        try {
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
        } finally {
            readLock.unlock();
        }
    }

    public ReadOnlyJedisPool getPool(String slaveNode) {
        readLock.lock();
        try {
            return poolMap.get(slaveNode);
        } finally {
            readLock.unlock();
        }
    }

    public Set<String> getSlaves() {
        readLock.lock();
        try {
            return slaves;
        } finally {
            readLock.unlock();
        }
    }
}
