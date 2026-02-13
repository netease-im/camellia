package com.netease.nim.camellia.redis.proxy.discovery.jedis;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.ServerNode;
import com.netease.nim.camellia.redis.base.utils.ProxyUtil;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.redis.proxy.discovery.common.*;
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
    private static final boolean defaultJedisPoolLazyInit = true;
    private static final int defaultJedisPoolInitialSize = 16;
    private static final int defaultTimeout = 2000;
    private static final int defaultMaxRetry = 5;
    private static final boolean defaultSideCarFirst = false;
    private static final ScheduledExecutorService defaultSchedule = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new CamelliaThreadFactory(RedisProxyJedisPool.class));
    private static final JedisPoolInitializer defaultJedisPoolInitializer = new JedisPoolInitializer.Default();
    private static String defaultLocalHost = "";
    static {
        try {
            defaultLocalHost = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignore) {
        }
    }

    private final Object lock = new Object();

    private final Map<ServerNode, JedisPool> jedisPoolMap = new HashMap<>();

    private final long id = idGenerator.incrementAndGet();
    private final long bid;
    private final String bgroup;
    private final CamelliaDiscovery discovery;
    private final GenericObjectPoolConfig poolConfig;
    private final int timeout;
    private final String password;
    private final int maxRetry;
    private final ScheduledExecutorService scheduledExecutorService;
    private final IProxySelector proxySelector;
    private final boolean jedisPoolLazyInit;
    private final int jedisPoolInitialSize;
    private final JedisPoolInitializer jedisPoolInitializer;
    private final int db;

    private RedisProxyJedisPool(long bid, String bgroup, CamelliaDiscovery discovery, GenericObjectPoolConfig poolConfig,
                               int timeout, String password, int refreshSeconds, int maxRetry, boolean sideCarFirst, String localhost,
                               RegionResolver regionResolver, IProxySelector proxySelector, boolean jedisPoolLazyInit, int jedisPoolInitialSize, JedisPoolInitializer jedisPoolInitializer, ScheduledExecutorService scheduledExecutorService, int db) {
        this.bid = bid;
        this.bgroup = bgroup;
        this.db = db;
        this.jedisPoolLazyInit = jedisPoolLazyInit;
        this.jedisPoolInitialSize = jedisPoolInitialSize;
        if (discovery == null) {
            throw new IllegalArgumentException("proxyDiscovery is null");
        }
        this.discovery = discovery;
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
        this.jedisPoolInitializer = jedisPoolInitializer == null ? defaultJedisPoolInitializer : jedisPoolInitializer;
        this.scheduledExecutorService = scheduledExecutorService == null ? defaultSchedule : scheduledExecutorService;
        init();
        if (jedisPoolLazyInit) {
            this.scheduledExecutorService.scheduleAtFixedRate(new RefreshThread(this),
                    5, refreshSeconds, TimeUnit.SECONDS);
        } else {
            this.scheduledExecutorService.scheduleAtFixedRate(new RefreshThread(this),
                    refreshSeconds, refreshSeconds, TimeUnit.SECONDS);
        }

        RedisProxyJedisPoolContext.init(this);
    }

    public static class Builder {

        private long bid = -1;//业务id，小于等于0表示不指定
        private String bgroup = null;//业务分组
        private int db = 0;
        private CamelliaDiscovery discovery;//proxy discovery，用于获取proxy列表以及获取proxy的变更通知，默认提供了基于zk的实现，你也可以自己实现
        private GenericObjectPoolConfig poolConfig = new JedisPoolConfig();//jedis pool config
        private int timeout = defaultTimeout;//超时
        private String password;//密码
        private int refreshSeconds = defaultRefreshSeconds;//兜底的从proxyDiscovery刷新proxy列表的间隔
        private int maxRetry = defaultMaxRetry;//获取jedis时的重试次数
        //因为每个proxy都要初始化一个JedisPool，当proxy数量很多的时候，可能会引起RedisProxyJedisPool初始化过慢
        //若开启jedisPoolLazyInit，则会根据proxySelector策略优先初始化jedisPoolInitialSize个proxy，剩余proxy会延迟初始化，从而加快RedisProxyJedisPool的初始化过程
        private boolean jedisPoolLazyInit = defaultJedisPoolLazyInit;//是否需要延迟初始化jedisPool，默认true，如果延迟初始化，则一开始会初始化少量的proxy对应的jedisPool，随后兜底线程会初始化剩余的proxy对应的jedisPool
        private int jedisPoolInitialSize = defaultJedisPoolInitialSize;//延迟初始化jedisPool时，一开始初始化的proxy个数，默认16个
        //以下参数用于设置proxy的选择策略
        //当显式的指定了proxySelector
        //--则使用自定义的proxy选择策略
        //若没有显示指定：
        //--当以下参数均未设置时，则会从所有proxy里随机挑选proxy发起请求，此时，实际使用的proxySelector是RandomProxySelector
        //--当设置了sideCarFirst=true，则会优先使用同机部署的proxy，即side-car-proxy，此时实际使用的proxySelector是SideCarFirstProxySelector
        //--localhost用于判断proxy是否是side-car-proxy，若缺失该参数，则会自动获取本机ip
        //--当设置了sideCarFirst=true，但是又找不到side-car-proxy，SideCarFirstProxySelector会优先使用相同region下的proxy，用于判断proxy归属于哪个region的方法是RegionResolver
        //-----当regionResolver未设置时，默认使用DummyRegionResolver，即认为所有proxy都归属于同一个proxy
        //-----我们还提供了一个IpSegmentRegionResolver的实现，该实现用ip段的方式来划分proxy的region，当然你也可以实现一个自定义的RegionResolver
        private boolean sideCarFirst = defaultSideCarFirst;
        private String localhost = defaultLocalHost;
        private RegionResolver regionResolver;
        private IProxySelector proxySelector;
        private ScheduledExecutorService scheduledExecutorService = defaultSchedule;
        private JedisPoolInitializer jedisPoolInitializer = defaultJedisPoolInitializer;

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

        public Builder db(int db) {
            this.db = db;
            return this;
        }

        public Builder proxyDiscovery(CamelliaDiscovery discovery) {
            this.discovery = discovery;
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

        public Builder jedisPoolLazyInit(boolean jedisPoolLazyInit) {
            this.jedisPoolLazyInit = jedisPoolLazyInit;
            return this;
        }

        public Builder jedisPoolInitialSize(int jedisPoolInitialSize) {
            this.jedisPoolInitialSize = jedisPoolInitialSize;
            return this;
        }

        public Builder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
            this.scheduledExecutorService = scheduledExecutorService;
            return this;
        }

        public Builder jedisPoolInitializer(JedisPoolInitializer jedisPoolInitializer) {
            this.jedisPoolInitializer = jedisPoolInitializer;
            return this;
        }

        public RedisProxyJedisPool build() {
            return new RedisProxyJedisPool(bid, bgroup, discovery, poolConfig, timeout, password,
                    refreshSeconds, maxRetry, sideCarFirst, localhost,
                    regionResolver, proxySelector, jedisPoolLazyInit, jedisPoolInitialSize, jedisPoolInitializer, scheduledExecutorService, db);
        }
    }

    @Override
    public Jedis getResource() {
        int retry = 0;
        Exception cause = null;
        while (retry < maxRetry) {
            try {
                ServerNode proxy = proxySelector.next(retry == 0);
                if (proxy == null) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("proxy not found and try again:{} times", retry);
                    }
                    retry ++;
                    continue;
                }
                JedisPool jedisPool = jedisPoolMap.get(proxy);
                if (jedisPool == null) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("jedisPool not found and try again:{} times", retry);
                    }
                    retry ++;
                    proxySelector.ban(proxy);
                    continue;
                }
                try {
                    return jedisPool.getResource();
                } catch (Exception e) {
                    logger.warn("exception is raise up and try {} times error message is:{}", retry, e.getMessage());
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
            throw new RedisProxyJedisPoolException("Could not get a resource from the pool");
        } else {
            throw new RedisProxyJedisPoolException("Could not get a resource from the pool", cause);
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
        if (scheduledExecutorService != defaultSchedule) {
            scheduledExecutorService.shutdown();
        }
        this.discovery.clearCallback(callback);
        synchronized (lock) {
            for (Map.Entry<ServerNode, JedisPool> entry : jedisPoolMap.entrySet()) {
                entry.getValue().close();
            }
            jedisPoolMap.clear();
        }
    }

    @Override
    public void destroy() {
        close();
    }

    private final CamelliaDiscovery.Callback callback = new CamelliaDiscovery.Callback() {
        @Override
        public void add(ServerNode proxy) {
            RedisProxyJedisPool.this.add(proxy);
        }

        @Override
        public void remove(ServerNode proxy) {
            RedisProxyJedisPool.this.remove(proxy);
        }
    };

    private void init() {
        List<ServerNode> list = this.discovery.findAll();
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("proxy list is empty");
        }
        List<ServerNode> sortedList = proxySelector.sort(list);
        if (jedisPoolLazyInit && list.size() > jedisPoolInitialSize) {
            int i = 0;
            for (ServerNode proxy : sortedList) {
                add(proxy);
                i ++;
                if (i >= jedisPoolInitialSize) {
                    break;
                }
            }
        } else {
            for (ServerNode proxy : sortedList) {
                add(proxy);
            }
        }
        this.discovery.setCallback(callback);
    }

    private void add(ServerNode proxy) {
        if (proxy == null) return;
        synchronized (lock) {
            try {
                proxySelector.add(proxy);
                JedisPool jedisPool = jedisPoolMap.get(proxy);
                if (jedisPool == null) {
                    jedisPool = jedisPoolInitializer.initJedisPool(new JedisPoolInitialContext(proxy, poolConfig, bid, bgroup, timeout, password, db));
                    jedisPoolMap.put(proxy, jedisPool);
                }
            } catch (Exception e) {
                logger.error("add proxy error, proxy = {}", proxy, e);
            }
        }
    }

    public static class JedisPoolInitialContext {
        private final ServerNode proxy;
        private final GenericObjectPoolConfig poolConfig;
        private final Long bid;
        private final String bgroup;
        private final int timeout;
        private final String password;
        private final int db;

        public JedisPoolInitialContext(ServerNode proxy, GenericObjectPoolConfig poolConfig, Long bid, String bgroup, int timeout, String password, int db) {
            this.proxy = proxy;
            this.poolConfig = poolConfig;
            this.bid = bid;
            this.bgroup = bgroup;
            this.timeout = timeout;
            this.password = password;
            this.db = db;
        }

        public ServerNode getProxy() {
            return proxy;
        }

        public GenericObjectPoolConfig getPoolConfig() {
            return poolConfig;
        }

        public Long getBid() {
            return bid;
        }

        public String getBgroup() {
            return bgroup;
        }

        public int getTimeout() {
            return timeout;
        }

        public String getPassword() {
            return password;
        }

        public int getDb() {
            return db;
        }
    }

    public static interface JedisPoolInitializer {

        JedisPool initJedisPool(JedisPoolInitialContext context);

        public static class Default implements JedisPoolInitializer {

            @Override
            public JedisPool initJedisPool(JedisPoolInitialContext context) {
                String clientName = null;
                if (context.getBid() > 0 && context.getBgroup() != null) {
                    clientName = ProxyUtil.buildClientName(context.getBid(), context.getBgroup());
                }
                return new JedisPool(context.poolConfig, context.getProxy().getHost(),
                        context.getProxy().getPort(), context.getTimeout(), context.getPassword(), context.getDb(), clientName);
            }
        }
    }

    private void remove(ServerNode proxy) {
        if (proxy == null) return;
        synchronized (lock) {
            try {
                boolean removed = proxySelector.remove(proxy);
                if (removed) {
                    JedisPool remove = jedisPoolMap.remove(proxy);
                    if (remove != null) {
                        scheduledExecutorService.schedule(() -> {
                            try {
                                remove.close();
                            } catch (Exception e) {
                                logger.error("close jedis pool error, proxy = {}", proxy, e);
                            }
                        }, 10, TimeUnit.SECONDS);
                    }
                }
            } catch (Exception e) {
                logger.error("remove proxy error, proxy = {}", proxy, e);
            }
        }
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
                List<ServerNode> list = proxyJedisPool.discovery.findAll();
                if (list != null && !list.isEmpty()) {
                    list = new ArrayList<>(list);
                    Collections.shuffle(list);
                    //add
                    for (ServerNode proxy : list) {
                        proxyJedisPool.add(proxy);
                    }
                    //remove
                    Set<ServerNode> proxySet = proxyJedisPool.proxySelector.getAll();
                    Set<ServerNode> oldSet = new HashSet<>(proxySet);
                    list.forEach(oldSet::remove);
                    if (!oldSet.isEmpty()) {
                        List<ServerNode> removed = new ArrayList<>(oldSet);
                        Collections.shuffle(removed);
                        for (ServerNode proxy : removed) {
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
