package com.netease.nim.camellia.redis.proxy;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.InetAddress;
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
    private static final boolean defaultSidCarFirst = false;
    private static String defaultLocalHost = "";
    static {
        try {
            defaultLocalHost = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignore) {
        }
    }

    private final Object lock = new Object();

    private final Map<Proxy, JedisPool> jedisPoolMap = new HashMap<>();
    private final List<Proxy> proxyList = new ArrayList<>();

    private final long id = idGenerator.incrementAndGet();
    private final long bid;
    private final String bgroup;
    private final IProxyDiscovery proxyDiscovery;
    private final GenericObjectPoolConfig poolConfig;
    private final int timeout;
    private final String password;
    private final int maxRetry;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(RedisProxyJedisPool.class));
    private final Set<Proxy> banProxySet = new HashSet<>();
    private final boolean sidCarFirst;
    private Proxy sidCarProxy;
    private final String localhost;//用于判断proxy是否sid-car

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery) {
        this(-1, null, proxyDiscovery, null, defaultTimeout, null, defaultRefreshSeconds, defaultMaxRetry, defaultSidCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, boolean sidCarFirst) {
        this(-1, null, proxyDiscovery, null, defaultTimeout, null, defaultRefreshSeconds, defaultMaxRetry, sidCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, boolean sidCarFirst, String localhost) {
        this(-1, null, proxyDiscovery, null, defaultTimeout, null, defaultRefreshSeconds, defaultMaxRetry, sidCarFirst, localhost);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, null, defaultRefreshSeconds, defaultMaxRetry, defaultSidCarFirst);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout, boolean sidCarFirst) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, null, defaultRefreshSeconds, defaultMaxRetry, sidCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout, boolean sidCarFirst, String localhost) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, null, defaultRefreshSeconds, defaultMaxRetry, sidCarFirst, localhost);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout, String password) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, password, defaultRefreshSeconds, defaultMaxRetry, defaultSidCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout, String password, boolean sidCarFirst) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, password, defaultRefreshSeconds, defaultMaxRetry, sidCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout, String password, boolean sidCarFirst, String localhost) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, password, defaultRefreshSeconds, defaultMaxRetry, sidCarFirst, localhost);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password, int refreshSeconds, int maxRetry) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, password, refreshSeconds, maxRetry, defaultSidCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(long bid, String bgroup, IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password) {
        this(bid, bgroup, proxyDiscovery, poolConfig, timeout, password, defaultRefreshSeconds, defaultMaxRetry, defaultSidCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(long bid, String bgroup, IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password, boolean sidCarFirst) {
        this(bid, bgroup, proxyDiscovery, poolConfig, timeout, password, defaultRefreshSeconds, defaultMaxRetry, sidCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(long bid, String bgroup, IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password, int refreshSeconds, int maxRetry) {
        this(bid, bgroup, proxyDiscovery, poolConfig, timeout, password, refreshSeconds, maxRetry, defaultSidCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(long bid, String bgroup, IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password, int refreshSeconds, int maxRetry, boolean sidCarFirst) {
        this(bid, bgroup, proxyDiscovery, poolConfig, timeout, password, refreshSeconds, maxRetry, sidCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(long bid, String bgroup, IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password, int refreshSeconds, int maxRetry, boolean sidCarFirst, String localhost) {
        this.bid = bid;
        this.bgroup = bgroup;
        if (proxyDiscovery == null) {
            throw new IllegalArgumentException("proxyDiscovery is null");
        }
        this.sidCarFirst = sidCarFirst;
        this.proxyDiscovery = proxyDiscovery;
        if (poolConfig == null) {
            this.poolConfig = new JedisPoolConfig();
        } else {
            this.poolConfig = poolConfig;
        }
        this.timeout = timeout;
        this.password = password;
        this.maxRetry = maxRetry;
        this.localhost = localhost;
        init();
        scheduledExecutorService.scheduleAtFixedRate(new RefreshThread(this),
                refreshSeconds, refreshSeconds, TimeUnit.SECONDS);
        RedisProxyJedisPoolContext.init(this);
    }

    @Override
    public Jedis getResource() {
        if (sidCarFirst && sidCarProxy != null) {
            try {
                JedisPool jedisPool = jedisPoolMap.get(sidCarProxy);
                if (jedisPool == null) {
                    sidCarProxy = null;
                } else {
                    return jedisPool.getResource();
                }
            } catch (Exception e) {
                sidCarProxy = null;
            }
        }

        int retry = 0;
        Exception cause = null;
        Proxy proxy = null;
        boolean originalProxyList;//是否使用的原始的proxyList
        List<Proxy> proxyList;
        if (!banProxySet.isEmpty()) {//如果banProxySet不是为空，则不使用原始的proxyList，copy一份，并且remove掉banProxySet
            proxyList = new ArrayList<>(this.proxyList);
            proxyList.removeAll(banProxySet);
            originalProxyList = false;
            if (proxyList.isEmpty()) {//如果remove掉banProxySet之后，发现变成了空，则退回到使用原始的proxyList，并且清空banProxySet
                synchronized (lock) {
                    banProxySet.clear();
                }
                proxyList = this.proxyList;
                originalProxyList = true;
            }
        } else {
            proxyList = this.proxyList;
            originalProxyList = true;
        }
        while (!proxyList.isEmpty() && !jedisPoolMap.isEmpty() && retry < maxRetry) {
            try {
                int index = ThreadLocalRandom.current().nextInt(proxyList.size());
                proxy = proxyList.get(index);
                if (proxy == null) {
                    retry ++;
                    continue;
                }
                JedisPool jedisPool = jedisPoolMap.get(proxy);
                if (jedisPool == null) {
                    proxy = null;
                    retry ++;
                    continue;
                }
                return jedisPool.getResource();
            } catch (Exception e) {
                cause = e;
                retry ++;
                if (proxy != null) {//如果获取到了proxy，但是获取Jedis失败了，则把这个proxy放到banProxySet里
                    synchronized (lock) {
                        banProxySet.add(proxy);
                    }
                    if (!originalProxyList) {//如果没有使用原始的originalProxyList，则从proxyList去掉这个proxy
                        proxyList.remove(proxy);
                        if (proxyList.isEmpty()) {//如果去掉之后发现proxyList变成了空，则退回到使用原始的proxyList，并且清空一下banProxySet
                            proxyList = this.proxyList;
                            originalProxyList = true;
                            synchronized (lock) {
                                banProxySet.clear();
                            }
                        }
                    }
                    proxy = null;
                }
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
            banProxySet.clear();
            proxyList.clear();
            for (Map.Entry<Proxy, JedisPool> entry : jedisPoolMap.entrySet()) {
                entry.getValue().close();
            }
            jedisPoolMap.clear();
        }
    }

    private final IProxyDiscovery.Callback callback = new IProxyDiscovery.Callback() {
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
                banProxySet.clear();
                if (!proxyList.contains(proxy)) {
                    JedisPool jedisPool = initJedisPool(proxy);
                    jedisPoolMap.put(proxy, jedisPool);
                    proxyList.add(proxy);
                } else {
                    logger.warn("proxyList contains proxy, skip add proxy! proxy = {}", proxy.toString());
                }
                if (isSidCarProxy(proxy)) {
                    sidCarProxy = proxy;
                }
            } catch (Exception e) {
                logger.error("add proxy error, proxy = {}", proxy, e);
            }
        }
    }

    private boolean isSidCarProxy(Proxy proxy) {
        if (proxy == null) return false;
        try {
            return proxy.getHost().equals(localhost);
        } catch (Exception e) {
            return false;
        }
    }

    private void remove(Proxy proxy) {
        if (proxy == null) return;
        synchronized (lock) {
            try {
                if (isSidCarProxy(proxy)) {
                    sidCarProxy = null;
                }
                banProxySet.clear();
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
        private final RedisProxyJedisPool proxyJedisPool;

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
                    for (Proxy proxy : proxyList) {
                        if (proxyJedisPool.isSidCarProxy(proxy)) {
                            proxyJedisPool.sidCarProxy = proxy;
                        }
                    }
                }
                proxyJedisPool.banProxySet.clear();
            } catch (Exception e) {
                logger.error("refresh error", e);
            }
        }
    }
}
