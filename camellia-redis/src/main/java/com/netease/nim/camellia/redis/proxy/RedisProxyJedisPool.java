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
    private static final boolean defaultSideCarFirst = false;
    private static String defaultLocalHost = "";
    static {
        try {
            defaultLocalHost = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignore) {
        }
    }

    private final Object lock = new Object();

    private final Map<Proxy, JedisPool> jedisPoolMap = new HashMap<>();

    private final long id = idGenerator.incrementAndGet();
    private final long bid;
    private final String bgroup;
    private final IProxyDiscovery proxyDiscovery;
    private final GenericObjectPoolConfig poolConfig;
    private final int timeout;
    private final String password;
    private final int maxRetry;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(RedisProxyJedisPool.class));
    private final IProxySelector proxySelector;

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery) {
        this(-1, null, proxyDiscovery, null, defaultTimeout, null, defaultRefreshSeconds, defaultMaxRetry, defaultSideCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, IProxySelector proxySelector) {
        this(-1, null, proxyDiscovery, null, defaultTimeout, null, defaultRefreshSeconds, defaultMaxRetry, defaultSideCarFirst, defaultLocalHost, null, proxySelector);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, boolean sideCarFirst) {
        this(-1, null, proxyDiscovery, null, defaultTimeout, null, defaultRefreshSeconds, defaultMaxRetry, sideCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, boolean sideCarFirst, RegionResolver regionResolver) {
        this(-1, null, proxyDiscovery, null, defaultTimeout, null, defaultRefreshSeconds, defaultMaxRetry, sideCarFirst, defaultLocalHost, regionResolver, null);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, boolean sideCarFirst, String localhost) {
        this(-1, null, proxyDiscovery, null, defaultTimeout, null, defaultRefreshSeconds, defaultMaxRetry, sideCarFirst, localhost);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, boolean sideCarFirst, String localhost, RegionResolver regionResolver) {
        this(-1, null, proxyDiscovery, null, defaultTimeout, null, defaultRefreshSeconds, defaultMaxRetry, sideCarFirst, localhost, regionResolver, null);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, null, defaultRefreshSeconds, defaultMaxRetry, defaultSideCarFirst);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout, IProxySelector proxySelector) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, null, defaultRefreshSeconds, defaultMaxRetry, defaultSideCarFirst, defaultLocalHost, null, proxySelector);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout, boolean sideCarFirst) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, null, defaultRefreshSeconds, defaultMaxRetry, sideCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout, boolean sideCarFirst, RegionResolver regionResolver) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, null, defaultRefreshSeconds, defaultMaxRetry, sideCarFirst, defaultLocalHost, regionResolver, null);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout, boolean sideCarFirst, String localhost) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, null, defaultRefreshSeconds, defaultMaxRetry, sideCarFirst, localhost);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout, String password) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, password, defaultRefreshSeconds, defaultMaxRetry, defaultSideCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout, String password, boolean sideCarFirst) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, password, defaultRefreshSeconds, defaultMaxRetry, sideCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout, String password, boolean sideCarFirst, RegionResolver regionResolver) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, password, defaultRefreshSeconds, defaultMaxRetry, sideCarFirst, defaultLocalHost, regionResolver, null);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout, String password, IProxySelector proxySelector) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, password, defaultRefreshSeconds, defaultMaxRetry, defaultSideCarFirst, defaultLocalHost, null, proxySelector);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout, String password, boolean sideCarFirst, String localhost) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, password, defaultRefreshSeconds, defaultMaxRetry, sideCarFirst, localhost);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig, int timeout, String password, boolean sideCarFirst, String localhost, RegionResolver regionResolver) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, password, defaultRefreshSeconds, defaultMaxRetry, sideCarFirst, localhost, regionResolver, null);
    }

    public RedisProxyJedisPool(IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password, int refreshSeconds, int maxRetry) {
        this(-1, null, proxyDiscovery, poolConfig, timeout, password, refreshSeconds, maxRetry, defaultSideCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(long bid, String bgroup, IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password) {
        this(bid, bgroup, proxyDiscovery, poolConfig, timeout, password, defaultRefreshSeconds, defaultMaxRetry, defaultSideCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(long bid, String bgroup, IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password, boolean sideCarFirst, RegionResolver regionResolver) {
        this(bid, bgroup, proxyDiscovery, poolConfig, timeout, password, defaultRefreshSeconds, defaultMaxRetry, sideCarFirst, defaultLocalHost, regionResolver, null);
    }

    public RedisProxyJedisPool(long bid, String bgroup, IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password, boolean sideCarFirst) {
        this(bid, bgroup, proxyDiscovery, poolConfig, timeout, password, defaultRefreshSeconds, defaultMaxRetry, sideCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(long bid, String bgroup, IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password, IProxySelector proxySelector) {
        this(bid, bgroup, proxyDiscovery, poolConfig, timeout, password, defaultRefreshSeconds, defaultMaxRetry, defaultSideCarFirst, defaultLocalHost, null, proxySelector);
    }

    public RedisProxyJedisPool(long bid, String bgroup, IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password, int refreshSeconds, int maxRetry) {
        this(bid, bgroup, proxyDiscovery, poolConfig, timeout, password, refreshSeconds, maxRetry, defaultSideCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(long bid, String bgroup, IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password, int refreshSeconds, int maxRetry, boolean sideCarFirst) {
        this(bid, bgroup, proxyDiscovery, poolConfig, timeout, password, refreshSeconds, maxRetry, sideCarFirst, defaultLocalHost);
    }

    public RedisProxyJedisPool(long bid, String bgroup, IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password, int refreshSeconds, int maxRetry, boolean sideCarFirst, String localhost) {
        this(bid, bgroup, proxyDiscovery, poolConfig, timeout, password, refreshSeconds, maxRetry, sideCarFirst, localhost, null, null);
    }

    public RedisProxyJedisPool(long bid, String bgroup, IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password, int refreshSeconds, int maxRetry, boolean sideCarFirst, String localhost, RegionResolver regionResolver) {
        this(bid, bgroup, proxyDiscovery, poolConfig, timeout, password, refreshSeconds, maxRetry, sideCarFirst, localhost, regionResolver, null);
    }

    public RedisProxyJedisPool(long bid, String bgroup, IProxyDiscovery proxyDiscovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password, int refreshSeconds, int maxRetry, boolean sideCarFirst, String localhost,
                               RegionResolver regionResolver, IProxySelector proxySelector) {
        this.bid = bid;
        this.bgroup = bgroup;
        if (proxyDiscovery == null) {
            throw new IllegalArgumentException("proxyDiscovery is null");
        }
        this.proxyDiscovery = proxyDiscovery;
        if (poolConfig == null) {
            this.poolConfig = new JedisPoolConfig();
        } else {
            this.poolConfig = poolConfig;
        }
        if (proxySelector != null) {
            this.proxySelector = proxySelector;
        } else {
            if (sideCarFirst) {
                if (regionResolver == null) {
                    regionResolver = new RegionResolver.DummyRegionResolver();
                }
                this.proxySelector = new SideCarFirstProxySelector(localhost, regionResolver);
            } else {
                this.proxySelector = new RandomProxySelector();
            }
        }
        this.timeout = timeout;
        this.password = password;
        this.maxRetry = maxRetry;
        init();
        scheduledExecutorService.scheduleAtFixedRate(new RefreshThread(this),
                refreshSeconds, refreshSeconds, TimeUnit.SECONDS);
        RedisProxyJedisPoolContext.init(this);
    }

    public static class Builder {

        private long bid = -1;
        private String bgroup = null;
        private IProxyDiscovery proxyDiscovery;
        private GenericObjectPoolConfig poolConfig = new JedisPoolConfig();
        private int timeout = defaultTimeout;
        private String password;
        private int refreshSeconds = defaultRefreshSeconds;
        private int maxRetry = defaultMaxRetry;
        private boolean sideCarFirst = defaultSideCarFirst;
        private String localhost = defaultLocalHost;
        private RegionResolver regionResolver;
        private IProxySelector proxySelector;

        public Builder() {
        }

        public Builder bid(long bid) {
            this.bid = bid;
            return this;
        }

        public Builder bgroup(String bgroup) {
            this.bgroup = bgroup;
            return this;
        }

        public Builder proxyDiscovery(IProxyDiscovery proxyDiscovery) {
            this.proxyDiscovery = proxyDiscovery;
            return this;
        }

        public Builder poolConfig(GenericObjectPoolConfig poolConfig) {
            this.poolConfig = poolConfig;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder refreshSeconds(int refreshSeconds) {
            this.refreshSeconds = refreshSeconds;
            return this;
        }

        public Builder maxRetry(int maxRetry) {
            this.maxRetry = maxRetry;
            return this;
        }

        public Builder sideCarFirst(boolean sideCarFirst) {
            this.sideCarFirst = sideCarFirst;
            return this;
        }

        public Builder localhost(String localhost) {
            this.localhost = localhost;
            return this;
        }

        public Builder regionResolver(RegionResolver regionResolver) {
            this.regionResolver = regionResolver;
            return this;
        }

        public Builder proxySelector(IProxySelector proxySelector) {
            this.proxySelector = proxySelector;
            return this;
        }

        public RedisProxyJedisPool build() {
            return new RedisProxyJedisPool(bid, bgroup, proxyDiscovery, poolConfig, timeout, password,
                    refreshSeconds, maxRetry, sideCarFirst, localhost, regionResolver, proxySelector);
        }
    }

    @Override
    public Jedis getResource() {
        int retry = 0;
        Exception cause = null;
        while (retry < maxRetry) {
            try {
                Proxy proxy = proxySelector.next();
                if (proxy == null) {
                    retry ++;
                    continue;
                }
                JedisPool jedisPool = jedisPoolMap.get(proxy);
                if (jedisPool == null) {
                    retry ++;
                    proxySelector.ban(proxy);
                    continue;
                }
                try {
                    return jedisPool.getResource();
                } catch (Exception e) {
                    cause = e;
                    retry ++;
                    proxySelector.ban(proxy);
                }
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

    @Override
    public void close() {
        RedisProxyJedisPoolContext.remove(this);
        scheduledExecutorService.shutdown();
        this.proxyDiscovery.clearCallback(callback);
        synchronized (lock) {
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
                proxySelector.add(proxy);
                JedisPool jedisPool = jedisPoolMap.get(proxy);
                if (jedisPool == null) {
                    jedisPool = initJedisPool(proxy);
                    jedisPoolMap.put(proxy, jedisPool);
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
                proxySelector.remove(proxy);
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

    public String getClientName() {
        String clientName = null;
        if (bid > 0 && bgroup != null) {
            clientName = ProxyUtil.buildClientName(bid, bgroup);
        }
        return clientName;
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
                    Set<Proxy> proxySet = proxyJedisPool.proxySelector.getAll();
                    for (Proxy proxy : list) {
                        proxyJedisPool.proxySelector.add(proxy);
                    }
                    Set<Proxy> oldSet = new HashSet<>(proxySet);
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
