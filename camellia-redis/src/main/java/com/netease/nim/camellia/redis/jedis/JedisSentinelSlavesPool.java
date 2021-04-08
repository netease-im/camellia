package com.netease.nim.camellia.redis.jedis;

import com.netease.nim.camellia.redis.conf.CamelliaRedisConstants;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.resource.RedisSentinelResource;
import com.netease.nim.camellia.redis.resource.RedisSentinelSlavesResource;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * Created by caojiajun on 2021/4/7
 */
public class JedisSentinelSlavesPool extends JedisPool {

    private static final Logger logger = LoggerFactory.getLogger(JedisSentinelSlavesPool.class);

    private final ConcurrentHashMap<String, JedisPool> poolMap = new ConcurrentHashMap<>();

    private final GenericObjectPoolConfig poolConfig;
    private final int timeout;
    private final String password;
    private HostAndPort master;
    private List<HostAndPort> slaves;

    public JedisSentinelSlavesPool(RedisSentinelSlavesResource redisSentinelSlavesResource,
                                   GenericObjectPoolConfig poolConfig, int timeout) {
        this(redisSentinelSlavesResource, poolConfig, timeout, CamelliaRedisConstants.Jedis.redisSentinelSlavesCheckIntervalMillis);
    }

    public JedisSentinelSlavesPool(RedisSentinelSlavesResource redisSentinelSlavesResource,
                                   GenericObjectPoolConfig poolConfig, int timeout, long slavesCheckIntervalMillis) {
        this.poolConfig = poolConfig;
        this.timeout = timeout;
        this.password = redisSentinelSlavesResource.getPassword();
        for (RedisSentinelResource.Node node : redisSentinelSlavesResource.getNodes()) {
            if (redisSentinelSlavesResource.isWithMaster()) {
                MasterListener masterListener = new MasterListener(this, redisSentinelSlavesResource.getMaster(), node.getHost(), node.getPort());
                if (master == null) {
                    masterListener.init();
                }
                masterListener.setDaemon(true);
                masterListener.start();
            }
            SlavesListener slavesListener = new SlavesListener(this, redisSentinelSlavesResource.getMaster(), node.getHost(), node.getPort(), slavesCheckIntervalMillis);
            if (slaves == null || slaves.isEmpty()) {
                slavesListener.init();
            }
            slavesListener.setDaemon(true);
            slavesListener.start();
        }
        if (master == null && (slaves == null || slaves.isEmpty())) {
            throw new CamelliaRedisException("Could not get an available node of master/slave, url = " + redisSentinelSlavesResource.getUrl());
        }
    }

    @Override
    public Jedis getResource() {
        int retry = 3;
        Exception cause = null;
        while (retry > 0) {
            retry --;
            try {
                if (master == null) {
                    int index = ThreadLocalRandom.current().nextInt(slaves.size());
                    HostAndPort slave = slaves.get(index);
                    JedisPool jedisPool = poolMap.get(slave.getUrl());
                    if (jedisPool != null) {
                        return jedisPool.getResource();
                    }
                } else {
                    int size = slaves.size() + 1;
                    int index = ThreadLocalRandom.current().nextInt(size);
                    if (index == 0) {
                        JedisPool jedisPool = poolMap.get(master.getUrl());
                        if (jedisPool != null) {
                            return jedisPool.getResource();
                        }
                    } else {
                        HostAndPort slave = slaves.get(index - 1);
                        JedisPool jedisPool = poolMap.get(slave.getUrl());
                        if (jedisPool != null) {
                            return jedisPool.getResource();
                        }
                    }
                }
            } catch (Exception e) {
                cause = e;
            }
        }
        if (cause == null) {
            throw new CamelliaRedisException("Could not get a resource from the pool");
        } else {
            throw new CamelliaRedisException("Could not get a resource from the pool", cause);
        }
    }

    @Override
    public void returnBrokenResource(Jedis resource) {
        if (resource != null) {
            resource.close();
        }
    }

    @Override
    public void returnResource(Jedis resource) {
        if (resource != null) {
            resource.close();
        }
    }

    private synchronized void updateMaster(HostAndPort master) {
        initPool(master);
        this.master = master;
    }

    private synchronized void updateSlaves(List<HostAndPort> slaves) {
        for (HostAndPort slave : slaves) {
            initPool(slave);
        }
        this.slaves = slaves;
    }

    private static class HostAndPort {
        private final String host;
        private final int port;
        private final String url;

        public HostAndPort(String host, int port) {
            this.host = host;
            this.port = port;
            this.url = host + ":" + port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getUrl() {
            return url;
        }
    }

    private void initPool(HostAndPort hostAndPort) {
        JedisPool jedisPool = poolMap.get(hostAndPort.getUrl());
        if (jedisPool == null) {
            jedisPool = new JedisPool(poolConfig, hostAndPort.getHost(), hostAndPort.getPort(), timeout, password);
            poolMap.put(hostAndPort.getUrl(), jedisPool);
        }
    }

    private static class SlavesListener extends Thread {
        private final JedisSentinelSlavesPool jedisSentinelSlavesPool;
        private final String masterName;
        private final String host;
        private final int port;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final long checkIntervalMillis;

        public SlavesListener(JedisSentinelSlavesPool jedisSentinelSlavesPool, String masterName, String host, int port, long checkIntervalMillis) {
            super(String.format("SlavesListener-%s-[%s:%d]", masterName, host, port));
            this.jedisSentinelSlavesPool = jedisSentinelSlavesPool;
            this.masterName = masterName;
            this.host = host;
            this.port = port;
            this.checkIntervalMillis = checkIntervalMillis;
        }

        public void init() {
            try {
                try (Jedis jedis = new Jedis(host, port)) {
                    refresh(jedis);
                }
            } catch (Exception e) {
                logger.error("SlavesListener init error", e);
            }
        }

        private void refresh(Jedis jedis) {
            List<HostAndPort> slaves = new ArrayList<>();
            List<Map<String, String>> list = jedis.sentinelSlaves(masterName);
            for (Map<String, String> map : list) {
                try {
                    String ip = map.get("ip");
                    int port = Integer.parseInt(map.get("port"));
                    String flags = map.get("flags");
                    if (flags != null && flags.equals("slave")) {
                        slaves.add(new HostAndPort(ip, port));
                    }
                } catch (Exception e) {
                    logger.error("parse slaves error", e);
                }
            }
            this.jedisSentinelSlavesPool.updateSlaves(slaves);
        }

        @Override
        public void run() {
            running.set(true);
            Jedis jedis = null;
            while (running.get()) {
                try {
                    if (jedis == null) {
                        jedis = new Jedis(host, port);
                    }
                    refresh(jedis);
                    TimeUnit.MILLISECONDS.sleep(checkIntervalMillis);
                } catch (Exception e) {
                    logger.error("sentinelSlaves error", e);
                    if (jedis != null) {
                        jedis.close();
                        jedis = null;
                    }
                }
            }
        }
    }

    private static class MasterListener extends Thread {

        private final JedisSentinelSlavesPool jedisSentinelSlavesPool;
        private final String masterName;
        private final String host;
        private final int port;
        private final AtomicBoolean running = new AtomicBoolean(false);

        public MasterListener(JedisSentinelSlavesPool jedisSentinelSlavesPool, String masterName, String host, int port) {
            super(String.format("MasterListener-%s-[%s:%d]", masterName, host, port));
            this.jedisSentinelSlavesPool = jedisSentinelSlavesPool;
            this.masterName = masterName;
            this.host = host;
            this.port = port;
        }

        public void init() {
            try (Jedis jedis = new Jedis(host, port)) {
                List<String> masterAddr = jedis.sentinelGetMasterAddrByName(masterName);
                if (masterAddr == null || masterAddr.size() != 2) {
                    logger.warn("Can not get master addr, master name: " + masterName + ". Sentinel: " + host + "：" + port + ".");
                } else {
                    String host = masterAddr.get(0);
                    int port = Integer.parseInt(masterAddr.get(1));
                    HostAndPort hostAndPort = new HostAndPort(host, port);
                    jedisSentinelSlavesPool.updateMaster(hostAndPort);
                }
            } catch (Exception e) {
                logger.error("MasterListener init error", e);
            }
        }

        @Override
        public void run() {
            running.set(true);
            while (running.get()) {
                try (Jedis jedis = new Jedis(host, port)) {
                    if (!running.get()) {
                        break;
                    }
                    List<String> masterAddr = jedis.sentinelGetMasterAddrByName(masterName);
                    if (masterAddr == null || masterAddr.size() != 2) {
                        logger.warn("Can not get master addr, master name: " + masterName + ". Sentinel: " + host + "：" + port + ".");
                    } else {
                        String host = masterAddr.get(0);
                        int port = Integer.parseInt(masterAddr.get(1));
                        HostAndPort hostAndPort = new HostAndPort(host, port);
                        jedisSentinelSlavesPool.updateMaster(hostAndPort);
                    }
                    jedis.subscribe(new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            logger.trace("Sentinel " + host + ":" + port + " published: " + message + ".");
                            String[] switchMasterMsg = message.split(" ");
                            if (switchMasterMsg.length > 3) {
                                if (masterName.equals(switchMasterMsg[0])) {
                                    HostAndPort hostAndPort = new HostAndPort(host, port);
                                    jedisSentinelSlavesPool.updateMaster(hostAndPort);
                                } else {
                                    logger.trace("Ignoring message on +switch-master for master name "
                                            + switchMasterMsg[0] + ", our master name is " + masterName);
                                }
                            } else {
                                logger.warn("Invalid message received on Sentinel " + host + ":" + port
                                        + " on channel +switch-master: " + message);
                            }
                        }
                    }, "+switch-master");
                } catch (Exception e) {
                    if (running.get()) {
                        logger.warn("Lost connection to Sentinel at " + host + ":" + port + ". Sleeping 5000ms and retrying.", e);
                        try {
                            TimeUnit.MILLISECONDS.sleep(5000);
                        } catch (InterruptedException e1) {
                            logger.warn("Sleep interrupted: ", e1);
                        }
                    } else {
                        logger.trace("Unsubscribing from Sentinel at " + host + ":" + port);
                    }
                }
            }
        }
    }

}
