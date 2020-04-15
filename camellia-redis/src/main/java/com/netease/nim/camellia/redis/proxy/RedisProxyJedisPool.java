package com.netease.nim.camellia.redis.proxy;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2019/11/6.
 */
public class RedisProxyJedisPool extends JedisPool {

    private static final Logger logger = LoggerFactory.getLogger(RedisProxyJedisPool.class);
    private static final AtomicLong idGenerator = new AtomicLong(0);
    private static final int defaultRefreshSeconds = 60;
    private static final int defaultTimeout = 2000;
    private static final int defaultMaxRetry = 5;

    private final Object lock = new Object();

    private Map<Proxy, JedisPool> jedisPoolMap = new HashMap<>();
    private List<Proxy> proxyList = new ArrayList<>();

    private long id = idGenerator.incrementAndGet();
    private long bid = -1;
    private String bgroup;
    private IProxyDiscovery proxyDiscovery;
    private GenericObjectPoolConfig poolConfig = new JedisPoolConfig();
    private int timeout = defaultTimeout;
    private String password;
    private int maxRetry = defaultMaxRetry;
    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(RedisProxyJedisPool.class));

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery) {
        this(-1, null, proxyDiscovery, null, defaultTimeout, null, defaultRefreshSeconds, defaultMaxRetry);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, null, defaultRefreshSeconds, defaultMaxRetry);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout, String password) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, password, defaultRefreshSeconds, defaultMaxRetry);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password, int refreshSeconds, int maxRetry) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, password, refreshSeconds, maxRetry);
    }

    public RedisProxyJedisPool(long bid, String bgroup, IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password) {
        this(bid, bgroup, proxyDiscovery, poolConfig, timeout, password, defaultRefreshSeconds, defaultMaxRetry);
    }

    public RedisProxyJedisPool(long bid, String bgroup, IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password, int refreshSeconds, int maxRetry) {
        this.bid = bid;
        this.bgroup = bgroup;
        this.proxyDiscovery = proxyDiscovery;
        this.poolConfig = poolConfig;
        this.timeout = timeout;
        this.password = password;
        this.maxRetry = maxRetry;
        init();
        scheduledExecutorService.scheduleAtFixedRate(new RefreshThread(this),
                refreshSeconds, refreshSeconds, TimeUnit.SECONDS);
        RedisProxyJedisPoolContext.init(this);
    }

    @Override
    public Jedis getResource() {
        int retry = 0;
        Exception cause = null;
        while (!proxyList.isEmpty() && !jedisPoolMap.isEmpty() && retry < maxRetry) {
            try {
                int index = ThreadLocalRandom.current().nextInt(proxyList.size());
                Proxy proxy = proxyList.get(index);
                if (proxy == null) {
                    retry ++;
                    continue;
                }
                JedisPool jedisPool = jedisPoolMap.get(proxy);
                if (jedisPool == null) {
                    retry ++;
                    continue;
                }
                return jedisPool.getResource();
            } catch (Exception e) {
                cause = e;
                retry ++;
            }
        }
        if (cause == null) {
            throw new CamelliaRedisException("Could not get a resource from the pool");
        } else {
            throw new CamelliaRedisException("Could not get a resource from the pool", cause);
        }
    }

    public long getId() {
        return id;
    }

    @Override
    public void returnBrokenResource(Jedis resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void returnResource(Jedis resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        RedisProxyJedisPoolContext.remove(this);
        scheduledExecutorService.shutdown();
        this.proxyDiscovery.clearCallback(callback);
        synchronized (lock) {
            proxyList.clear();
            for (Map.Entry<Proxy, JedisPool> entry : jedisPoolMap.entrySet()) {
                entry.getValue().close();
            }
            jedisPoolMap.clear();
        }
    }

    private IProxyDiscovery.Callback callback = new IProxyDiscovery.Callback() {
        @Override
        public void add(Proxy proxy) {
            RedisProxyJedisPool.this.add(proxy);
        }

        @Override
        public void remove(Proxy proxy) {
            RedisProxyJedisPool.this.remove(proxy);
        }
    };

    private void init() {
        List<Proxy> list = this.proxyDiscovery.findAll();
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("proxy list is empty");
        }
        for (Proxy proxy : list) {
            add(proxy);
        }
        this.proxyDiscovery.setCallback(callback);
    }

    private void add(Proxy proxy) {
        if (proxy == null) return;
        synchronized (lock) {
            try {
                if (!proxyList.contains(proxy)) {
                    JedisPool jedisPool = initJedisPool(proxy);
                    jedisPoolMap.put(proxy, jedisPool);
                    proxyList.add(proxy);
                } else {
                    logger.warn("proxyList contains proxy, skip add proxy! proxy = {}", proxy.toString());
                }
            } catch (Exception e) {
                logger.error("add proxy error, proxy = {}", proxy, e);
            }
        }
    }

    private void remove(Proxy proxy) {
        if (proxy == null) return;
        synchronized (lock) {
            try {
                if (proxyList.size() == 1 && proxyList.contains(proxy)) {
                    logger.warn("proxyList.size = 1, skip remove proxy! proxy = {}", proxy.toString());
                    return;
                }
                proxyList.remove(proxy);
                JedisPool remove = jedisPoolMap.remove(proxy);
                if (remove != null) {
                    remove.close();
                }
            } catch (Exception e) {
                logger.error("remove proxy error, proxy = {}", proxy, e);
            }
        }
    }

    private JedisPool initJedisPool(Proxy proxy) {
        String clientName = null;
        if (bid > 0 && bgroup != null) {
            clientName = ProxyUtil.buildClientName(bid, bgroup);
        }
        return new JedisPool(poolConfig, proxy.getHost(), proxy.getPort(), timeout, password, 0, clientName);
    }

    //兜底线程
    private static class RefreshThread extends Thread {
        private RedisProxyJedisPool proxyJedisPool;

        RefreshThread(RedisProxyJedisPool proxyJedisPool) {
            this.proxyJedisPool = proxyJedisPool;
        }

        @Override
        public void run() {
            try {
                List<Proxy> list = proxyJedisPool.proxyDiscovery.findAll();
                if (list != null && !list.isEmpty()) {
                    List<Proxy> proxyList = proxyJedisPool.proxyList;
                    Set<Proxy> newSet = new HashSet<>(list);
                    newSet.removeAll(proxyList);
                    if (!newSet.isEmpty()) {
                        for (Proxy proxy : newSet) {
                            proxyJedisPool.add(proxy);
                        }
                    }
                    Set<Proxy> oldSet = new HashSet<>(proxyList);
                    oldSet.removeAll(list);
                    if (!oldSet.isEmpty()) {
                        for (Proxy proxy : oldSet) {
                            proxyJedisPool.remove(proxy);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("refresh error", e);
            }
        }
    }
}
