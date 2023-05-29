## 插件实现

### 初始化所有插件

入口`CommandInvoker`的构造器方法中有这样一行代码：

```java
DefaultProxyPluginFactory proxyPluginFactory = new DefaultProxyPluginFactory(serverProperties.getPlugins(), serverProperties.getProxyBeanFactory());
```

目的是从配置中获得所有的插件string以及工厂`ProxyBeanFactory`。

`ProxyBeanFactory` 有两个实现。

- `SpringProxyBeanFactory`：从spring容器中获取对应的bean。
- `DefaultBeanFactory`：通过反射构建出单例实例。

两个实现是为了在spring环境或者非spring环境根据配置的名字获得相应的单例对象。

`DefaultProxyPluginFactory`构造器如下：

```java
public DefaultProxyPluginFactory(List<String> defaultPlugins, ProxyBeanFactory beanFactory) {
    this.defaultPlugins = defaultPlugins;
    this.beanFactory = beanFactory;
    reload();
    int seconds = ProxyDynamicConf.getInt("proxy.plugin.update.interval.seconds", 60);
    // 按照配置的频率去调用reload方法
    ExecutorUtils.scheduleAtFixedRate(this::reload, seconds, seconds, TimeUnit.SECONDS);
}
```

在DefaultProxyPluginFactory中的构造函数调用了一次reload，之后再通过线程实现了后台自动刷新插件配置的方法。

`reload`()方法如下：

```java
    /**
     * 依次调用callbackSet里面的方法，{@link DefaultProxyPluginFactory#CONF_KEY} 配置字符串发生改变的时候才会进行调用
     */
    private void reload() {
        String pluginConf = ProxyDynamicConf.getString(CONF_KEY, "");
        if (!Objects.equals(pluginConf, this.pluginConf)) {
            this.pluginConf = pluginConf;
            for (Runnable runnable : callbackSet) {
                try {
                    runnable.run();
                } catch (Exception e) {
                    logger.error("proxy plugin callback error", e);
                }
            }
        }
    }
```

只根据配置是否改变去调用回调方法，并不会初始化所有的插件。

初始化插件延迟到第一个命令发出的时候，也就是`CommandsTransponder` 创建的时候如下：

```java
public CommandsTransponder(IUpstreamClientTemplateFactory factory, CommandInvokeConfig commandInvokeConfig) {
    this.factory = factory;
    this.authCommandProcessor = commandInvokeConfig.getAuthCommandProcessor();
    this.clusterModeProcessor = commandInvokeConfig.getClusterModeProcessor();
    this.proxyPluginFactory = commandInvokeConfig.getProxyPluginFactory();
    this.proxyPluginInitResp = proxyPluginFactory.initPlugins();
    // 刷新插件用的
    proxyPluginFactory.registerPluginUpdate(() -> proxyPluginInitResp = proxyPluginFactory.initPlugins());
}
```

`registerPluginUpdate`就是将插件初始化方法加入到回调接口中，方便后台线程按时调用`reload()`方法对插件进行刷新。`registerPluginUpdate`就是将插件初始化方法加入到回调接口中，方便后台线程按时调用`reload()`方法对回调set进行遍历从而实现，插件进行刷新。

`initPlugins()`方法如下：

```java
public ProxyPluginInitResp initPlugins() {
    // 根据配置中的名称构建插件
    Set<String> pluginSet = new HashSet<>(defaultPlugins);
    if (pluginConf != null && pluginConf.trim().length() != 0) {
        String[] split = pluginConf.trim().split(",");
        pluginSet.addAll(Arrays.asList(split));
    }
	// ....省略
    List<ProxyPlugin> plugins = new ArrayList<>();
    for (String classOrAlias : pluginSet) {
        ProxyPlugin proxyPlugin = getOrInitProxyPlugin(classOrAlias);
        plugins.add(proxyPlugin);
    }
    // 插件排序，先按request排再按reply排，越大约优先
	// ....省略
    return new ProxyPluginInitResp(requestPlugins, replyPlugins);
}
```

从配置中插件的类名以及别名alias，通过`ProxyBeanFactory`获得相应的bean对象之后，按照配置的优先级分别对request插件和reply插件进行排序。

### 插件执行流程

proxy的命令调用的流程是命令 -> requst插件->redis服务器->reply插件。

## Hot-key-Cache plugin

### 热key判断

判断一个key，是否是hot-key的标准，是一个时间窗口内的命中达到一个阈值即为hot-key。

### 删除策略

整体上使用的是**惰性删除**的策略。不管是counter还是HotKeyCache，当访问到的时候会去判断是否过期并且删除。

### HotKeyCacheKeyChecker

#### 作用

判断key是否需要进行热key缓存。

#### PrefixMatchHotKeyCacheKeyChecker

前缀匹配，根据多个配置key，只要符合当中的一个，就代表需要缓存。类中自带`cache`，无需多次解析配置。同时，当动态缓存刷新的时候，cache也会自动被清除。

```java
/**
 * 对key进行前缀匹配
 * Created by caojiajun on 2021/1/5
 */
public class PrefixMatchHotKeyCacheKeyChecker implements HotKeyCacheKeyChecker {

    private final ConcurrentHashMap<String, Set<String>> cache = new ConcurrentHashMap<>();

    public PrefixMatchHotKeyCacheKeyChecker() {
        ProxyDynamicConf.registerCallback(cache::clear);
    }

    @Override
    public boolean needCache(IdentityInfo identityInfo, byte[] key) {
        try {
            Long bid = identityInfo.getBid();
            String bgroup = identityInfo.getBgroup();
            String cacheKey = Utils.getCacheKey(bid, bgroup);
            // 这里不用DCL是因为不用保证每次获取是同一个对象，也就是说不用做成单例
            Set<String> set = cache.get(cacheKey);
            if (set == null) {
                set = hotKeyCacheKeyPrefix(bid, bgroup);
                cache.put(cacheKey, set);
            }
            if (set.isEmpty()) return false;
            String keyStr = Utils.bytesToString(key);
            for (String prefix : set) {
                if (keyStr.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            ErrorLogCollector.collect(PrefixMatchHotKeyCacheKeyChecker.class, "check key prefix error", e);
            return false;
        }
    }

    private Set<String> hotKeyCacheKeyPrefix(Long bid, String bgroup) {
        String conf = ProxyDynamicConf.getString("hot.key.cache.key.prefix", bid, bgroup, "");
        if (conf == null || conf.trim().length() == 0) {
            return new HashSet<>();
        }
        Set<String> set = new HashSet<>();
        try {
            JSONArray array = JSONArray.parseArray(conf);
            for (Object o : array) {
                set.add(String.valueOf(o));
            }
        } catch (Exception ignore) {
        }
        return set;
    }
}
```

当然，你也可以自己定制一个，只要实现`HotKeyCacheKeyChecker`接口，就可以。

### LoggingHotKeyCacheStatsCallback

热keyCache状态回调接口。

#### 作用

后台线程每隔`"hot.key.cache.stats.callback.interval.seconds"`秒，对状态进行回调执行。

#### LoggingHotKeyCacheStatsCallback

```java
public class LoggingHotKeyCacheStatsCallback implements HotKeyCacheStatsCallback {

    private static final Logger logger = LoggerFactory.getLogger("camellia.redis.proxy.hotKeyCacheStats");

    @Override
    public void callback(IdentityInfo identityInfo, HotKeyCacheInfo hotKeyCacheStats, long checkMillis, long checkThreshold) {
        try {
            if (hotKeyCacheStats == null) return;
            List<HotKeyCacheInfo.Stats> list = hotKeyCacheStats.getStatsList();
            if (list == null) return;
            logger.warn("====hot-key-cache-stats====");
            logger.warn("identify.info = {}", identityInfo);
            for (HotKeyCacheInfo.Stats stats : list) {
                logger.warn("hot-key-cache-stats, key = {}, hitCount = {}, checkMillis = {}, checkThreshold = {}",
                        Utils.bytesToString(stats.getKey()), stats.getHitCount(), checkMillis, checkThreshold);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
```

比如LoggingHotKey打印实现，其作用就是将hotkeyCache状态信息打印出来。

### <span id="HotKeyCacheManager">HotKeyCacheManager</span>

#### 作用

以租户为key创建出HotKeyCache。

```java
/**
 * 工厂模式
 * Created by caojiajun on 2020/11/8
 */
public class HotKeyCacheManager {
    /**
     * 针对不同租户缓存的map，key: bid + "|" + bgroup, value: {@link HotKeyCache}
     */
    private final ConcurrentHashMap<String, HotKeyCache> map = new ConcurrentHashMap<>();
    /**
     * 锁map，锁粒度是不同的key；为了对 {@link HotKeyCacheManager#map} 并发加载的时候不同key之间互不影响
     */
    private final LockMap lockMap = new LockMap();
    /**
     * 如果不是多租户，就只有一个{@link HotKeyCache}
     */
    private final HotKeyCache hotKeyCache;

    /**
     * 配置
     */
    private final HotKeyCacheConfig hotKeyCacheConfig;

    public HotKeyCacheManager(HotKeyCacheConfig hotKeyCacheConfig) {
        this.hotKeyCacheConfig = hotKeyCacheConfig;
        this.hotKeyCache = new HotKeyCache(new IdentityInfo(null, null), hotKeyCacheConfig);
    }

    /**
     * 懒加载，保证单例也就是一个key对应唯一一个cache，用DCL双重检测去优化并发。
     *
     * @param bid    多租户的id
     * @param bgroup 多租户的组
     * @return {@link HotKeyCache} 热keyCache本地缓存
     */
    public HotKeyCache get(Long bid, String bgroup) {
        if (bid == null || bgroup == null) {
            return hotKeyCache;
        } else {
            String key = Utils.getCacheKey(bid, bgroup);
            HotKeyCache hotKeyCache = map.get(key);
            if (hotKeyCache == null) {
                synchronized (lockMap.getLockObj(key)) {
                    hotKeyCache = map.get(key);
                    if (hotKeyCache == null) {
                        hotKeyCache = new HotKeyCache(new IdentityInfo(bid, bgroup), hotKeyCacheConfig);
                        map.put(key, hotKeyCache);
                    }
                }
            }
            return hotKeyCache;
        }
    }
}
```

值得注意的是，这里的DCL锁对象是以key为单位的，也就是说，不同租户之间的cache并发互不影响。

### 初始化插件

```java
public void init(ProxyBeanFactory factory) {
    HotKeyCacheConfig hotKeyCacheConfig = new HotKeyCacheConfig();

    // Set hotKeyChecker and the default is PrefixMatchHotKeyCacheKeyChecker. Also, you can implement HotKeyCacheKeyChecker to customize a checker.
    String hotKeyCacheKeyCheckerClassName = ProxyDynamicConf.getString("hot.key.cache.key.checker.className", PrefixMatchHotKeyCacheKeyChecker.class.getName());
    HotKeyCacheKeyChecker hotKeyCacheKeyChecker = (HotKeyCacheKeyChecker) factory.getBean(BeanInitUtils.parseClass(hotKeyCacheKeyCheckerClassName));
    hotKeyCacheConfig.setHotKeyCacheKeyChecker(hotKeyCacheKeyChecker);

    String hotKeyCacheStatsCallbackClassName = ProxyDynamicConf.getString("hot.key.cache.stats.callback.className", DummyHotKeyCacheStatsCallback.class.getName());
    HotKeyCacheStatsCallback hotKeyCacheStatsCallback = (HotKeyCacheStatsCallback) factory.getBean(BeanInitUtils.parseClass(hotKeyCacheStatsCallbackClassName));
    hotKeyCacheConfig.setHotKeyCacheStatsCallback(hotKeyCacheStatsCallback);
    manager = new HotKeyCacheManager(hotKeyCacheConfig);
}
```

分别设置好，上述的三个部分。

### 命令未到达redis，执行Request插件

在命令未到达redis的时候会先执行request插件。

```java
public ProxyPluginResponse executeRequest(ProxyRequest proxyRequest) {
    Command command = proxyRequest.getCommand();
    RedisCommand redisCommand = command.getRedisCommand();
    // 只对get命令做缓存
    if (redisCommand == RedisCommand.GET) {
        byte[][] objects = command.getObjects();
        if (objects.length > 1) {
            CommandContext commandContext = command.getCommandContext();
            HotKeyCache hotKeyCache = manager.get(commandContext.getBid(), commandContext.getBgroup());
            byte[] key = objects[1];
            HotValue value = hotKeyCache.getCache(key);
            if (value != null) {
                BulkReply bulkReply = new BulkReply(value.getValue());
                return new ProxyPluginResponse(false, bulkReply);
            }
        }
    }
    return ProxyPluginResponse.SUCCESS;
}
```

### LRUCounter

key的命中次数的机制：需要时间窗口内达到一定的次数。

LRUCounter 中有一个map，代表每一个key都会有一次计数。整体思想用的**惰性删除**策略，当调用方法的时候才会去判断计数器是否过期。

#### Counter

内部计数类Counter如下：

```java
private static class Counter {
    private volatile long timestamp = TimeCache.currentMillis;
    private final LongAdder count = new LongAdder();
    private final AtomicBoolean lock = new AtomicBoolean();

    /**
         * Check whether the counter has expired, if it is expired, refresh it.
         *
         * @param expireMillis expireMillis
         */
    void checkExpireAndReset(long expireMillis) {
        if (TimeCache.currentMillis - timestamp > expireMillis) {
            if (lock.compareAndSet(false, true)) {
                try {
                    if (TimeCache.currentMillis - timestamp > expireMillis) {
                        timestamp = TimeCache.currentMillis;
                        count.reset();
                    }
                } finally {
                    lock.compareAndSet(true, false);
                }
            }
        }
    }
}
```

这里用了一个CAS，为了防止并发访问key的时候，查看是否过期。LongAdder，也是用来并发计数用的。

#### increment

计数器对key进行自增的代码如下：

```java
public void increment(BytesKey bytesKey) {
    Counter counter = cache.get(bytesKey);
    if (counter != null) {
        counter.checkExpireAndReset(expireMillis);
    }
    if (counter == null) {
        counter = new Counter();
        Counter old = cache.putIfAbsent(bytesKey, counter);
        if (old != null) {
            counter = old;
        }
    }
    counter.count.increment();
}
```

同样考虑到了并发计数的问题，先判断是否过期，如果是null，新增一个计数器。

#### get

获取key的计数代码如下：

```java
public Long get(BytesKey bytesKey) {
    Counter counter = cache.get(bytesKey);
    if (counter != null) {
        if (TimeCache.currentMillis - counter.timestamp > expireMillis) {
            cache.remove(bytesKey);
            return null;
        }
        return counter.count.sum();
    }
    return null;
}
```

判断过期，过期了就不用考虑了。再次强调下热key的判断：一段时间间隔内的key，命中次数超过阈值才是一个热key。

#### getSortedCacheValue

获取对key的命中排序的代码如下：

```java
    /**
     * 如果key的计数超过了threshold，并且key没有过期，就会按照计数的大小值进行排序，
     * If the count of the key exceeds the threshold and the key has not expired, it will be sorted according to the count.
     *
     * @param threshold threshold 阈值
     * @return TreeSet
     */
    public TreeSet<SortedBytesKey> getSortedCacheValue(long threshold) {
        if (cache.isEmpty()) return null;
        TreeSet<SortedBytesKey> treeSet = new TreeSet<>();
        for (Map.Entry<BytesKey, Counter> entry : cache.entrySet()) {
            if (TimeCache.currentMillis - entry.getValue().timestamp > expireMillis) {
                cache.remove(entry.getKey());
                continue;
            }
            long count = entry.getValue().count.sum();
            if (count >= threshold) {
                byte[] key = entry.getKey().getKey();
                treeSet.add(new SortedBytesKey(key, count));
            }
        }
        return treeSet;
    }

    public static class SortedBytesKey implements Comparable<SortedBytesKey> {
        private final byte[] key;
        private final long count;

        public SortedBytesKey(byte[] key, long count) {
            this.key = key;
            this.count = count;
        }

        public byte[] getKey() {
            return key;
        }

        public long getCount() {
            return count;
        }

        @Override
        public int compareTo(SortedBytesKey o) {
            return Long.compare(o.count, count);
        }
    }
```

利用TreeSet做的一个排序，同时去除过期项，主要作用是为了统计TOPN。

### <span id="HotKeyCache">HotKeyCache</span>

#### getCache

从cache中获取代码如下：

```java
public HotValue getCache(byte[] key) {
    if (!enable) return null;
    if (keyChecker != null && !keyChecker.needCache(identityInfo, key)) {
        return null;
    }
    BytesKey bytesKey = new BytesKey(key);
    this.hotKeyCounter.increment(bytesKey);
    HotValueWrapper wrapper = cache.get(bytesKey);
    if (wrapper != null) {
        // 过期删除
        if (TimeCache.currentMillis - wrapper.timestamp > cacheExpireMillis) {
            cache.remove(bytesKey);
            return null;
        }
        HotValue value = wrapper.hotValue;
        if (value != null) {
            Long lastRefreshTime = lastRefreshTimeMap.get(bytesKey);
            // 当cache的过期时间已经达到一半值，打一个tag，穿透到redis进行本地缓存刷新
            if (lastRefreshTime != null && TimeCache.currentMillis - lastRefreshTime > cacheExpireMillis / 2) {
                Object old = refreshLockMap.putIfAbsent(bytesKey, lockObj);
                if (old == null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("try refresh hotKey's value, key = {}", Utils.bytesToString(bytesKey.getKey()));
                    }
                    return null;
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("getCache of hotKey = {}", Utils.bytesToString(bytesKey.getKey()));
            }
            // 计算命中
            if (callback != null) {
                AtomicLong hit = CamelliaMapUtils.computeIfAbsent(statsMap, bytesKey, k -> new AtomicLong());
                hit.incrementAndGet();
            }
        }
        return value;
    }
    return null;
}
```

先通过keyChecker判断是否需要缓存。之后对key进行自增。如果cache获取到了值，判断是否过期。从lastRefreshTimeMap中获得key最后一次更新的时间，同时判断是不是快过期了，快过期了就在refreshLockMap设置一个标识位。如果这个时候并发，有其他线程线程刚好判断出要过期了，old就不是null，正常把value返回。这么做的目的是，只允许一个请求发送给redis，再通过replyPlugin实现重建本地缓存。

#### tryBuildHotKeyCache

建立缓存代码如下：

```java
    /**
     * 重建缓存
     * @param key key
     * @param value value
     */
    public void tryBuildHotKeyCache(byte[] key, byte[] value) {
        if (!enable) return;
        if (value == null && !cacheNull) {
            return;
        }
        // 是否需要缓存
        if (keyChecker != null && !keyChecker.needCache(identityInfo, key)) {
            return;
        }
        BytesKey bytesKey = new BytesKey(key);
        // 计数器判断有没有到达阈值
        Long count = this.hotKeyCounter.get(bytesKey);
        if (count == null || count < hotKeyCheckThreshold) {
            return;
        }
        // 建立缓存
        cache.put(bytesKey, new HotValueWrapper(new HotValue(value)));
        lastRefreshTimeMap.put(bytesKey, TimeCache.currentMillis);
        refreshLockMap.remove(bytesKey);
        if (logger.isDebugEnabled()) {
            logger.debug("refresh hotKey's value success, key = {}", Utils.bytesToString(bytesKey.getKey()));
        }
    }
```

#### 状态统计

在hot-key-cache中有属于自己的本地缓存命中率统计。

```java
private ConcurrentHashMap<BytesKey, AtomicLong> statsMap = new ConcurrentHashMap<>();
```

在`getCache`中有这样一段代码。

```java
// 计算命中
if (callback != null) {
    AtomicLong hit = CamelliaMapUtils.computeIfAbsent(statsMap, bytesKey, k -> new AtomicLong());
    hit.incrementAndGet();
}
```

这段代码就是记录cache的命中率，同样用的是ConcurrentHashMap来实现的。实现状态统计在HotKey构造器中，代码如下：

```java
ExecutorUtils.scheduleAtFixedRate(() -> {
    try {
        if (HotKeyCache.this.statsMap.isEmpty()) return;
        ConcurrentHashMap<BytesKey, AtomicLong> statsMap = HotKeyCache.this.statsMap;
        HotKeyCache.this.statsMap = new ConcurrentHashMap<>();

        List<HotKeyCacheInfo.Stats> list = new ArrayList<>();
        for (Map.Entry<BytesKey, AtomicLong> entry : statsMap.entrySet()) {
            HotKeyCacheInfo.Stats stats = new HotKeyCacheInfo.Stats();
            stats.setKey(entry.getKey().getKey());
            stats.setHitCount(entry.getValue().get());
            list.add(stats);
        }
        HotKeyCacheInfo hotKeyCacheStats = new HotKeyCacheInfo();
        hotKeyCacheStats.setStatsList(list);
        HotKeyCacheMonitor.hotKeyCache(identityInfo, hotKeyCacheStats, counterCheckMillis, hotKeyCheckThreshold);
        ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> callback.callback(identityInfo, hotKeyCacheStats, counterCheckMillis, hotKeyCheckThreshold));
    } catch (Exception e) {
        logger.error("hot key cache stats callback error", e);
    }
}, callbackIntervalSeconds, callbackIntervalSeconds, TimeUnit.SECONDS);
logger.info("HotKeyCache init success, identityInfo = {}", identityInfo);
```

线程以固定的速度，对statsMap的状态进行收集，并且提交到线程池中运行。

### 命令到达redis，执行Reply插件

在Reply的时候会尝试建立缓存，代码如下：

```java
    public ProxyPluginResponse executeReply(ProxyReply proxyReply) {
        if (proxyReply.isFromPlugin()) return ProxyPluginResponse.SUCCESS;
        Command command = proxyReply.getCommand();
        if (command == null) return ProxyPluginResponse.SUCCESS;
        RedisCommand redisCommand = command.getRedisCommand();
        if (redisCommand == RedisCommand.GET) {
            Reply reply = proxyReply.getReply();
            if (reply instanceof BulkReply) {
                CommandContext commandContext = proxyReply.getCommandContext();
                HotKeyCache hotKeyCache = manager.get(commandContext.getBid(), commandContext.getBgroup());
                byte[] key = command.getObjects()[1];
                byte[] value = ((BulkReply) reply).getRaw();
                hotKeyCache.tryBuildHotKeyCache(key, value);
            }
        }
        return ProxyPluginResponse.SUCCESS;
    }
```

## Hot-key plugin

热key插件主要是，在命令尚未到达后端redis时对key进行监控的效果，少了cache的功能，代码如下。

```java
package com.netease.nim.camellia.redis.proxy.plugin.hotkey;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;

import java.util.List;

/**
 * Created by caojiajun on 2022/9/13
 */
public class HotKeyProxyPlugin implements ProxyPlugin {

    private HotKeyHunterManager manager;

    @Override
    public void init(ProxyBeanFactory factory) {
        String callbackClassName = ProxyDynamicConf.getString("hot.key.monitor.callback.className", DummyHotKeyMonitorCallback.class.getName());
        Class<?> clazz = BeanInitUtils.parseClass(callbackClassName);
        HotKeyMonitorCallback callback = (HotKeyMonitorCallback) factory.getBean(clazz);
        manager = new HotKeyHunterManager(callback);
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return BuildInProxyPluginEnum.HOT_KEY_PLUGIN.getRequestOrder();
            }

            @Override
            public int reply() {
                return BuildInProxyPluginEnum.HOT_KEY_PLUGIN.getReplyOrder();
            }
        };
    }

    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest request) {
        //属于监控类plugin，因此也受isMonitorEnable控制
        if (!ProxyMonitorCollector.isMonitorEnable()) return ProxyPluginResponse.SUCCESS;
        Command command = request.getCommand();
        CommandContext commandContext = command.getCommandContext();
        HotKeyHunter hotKeyHunter = manager.get(commandContext.getBid(), commandContext.getBgroup());
        List<byte[]> keys = command.getKeys();
        hotKeyHunter.incr(keys);
        return ProxyPluginResponse.SUCCESS;
    }
}

```

### HotKeyHunterManager

见[HotKeyCacheManager](#HotKeyCacheManager), 一样的单例思路。

### HotKeyHunter

一个时间窗口的key，命中次数超过阈值才是一个热key。

代码如下：

```java
public class HotKeyHunter {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyHunter.class);

    private final String CALLBACK_NAME;

    private boolean enable;
    private final HotKeyMonitorCallback callback;
    private final LRUCounter counter;
    private final IdentityInfo identityInfo;
    /**
     * 热key监控统计的时间窗口，默认1000ms
     */
    private final long checkMillis;

    /**
     * 热key监控统计在时间窗口内超过多少阈值，判定为热key，默认500
     */
    private long checkThreshold;
    /**
     * 单个周期内最多上报多少个热key，默认32（取top）
     */
    private int maxHotKeyCount;

    public HotKeyHunter(IdentityInfo identityInfo, HotKeyMonitorCallback callback) {
        this.identityInfo = identityInfo;
        this.enable = true;
        reloadHotKeyConfig();
        // register dynamic config callback
        ProxyDynamicConf.registerCallback(this::reloadHotKeyConfig);
        this.callback = callback;
        this.CALLBACK_NAME = callback.getClass().getName();
        // LRUCounter capacity
        int checkCacheMaxCapacity = ProxyDynamicConf.getInt("hot.key.monitor.cache.max.capacity",
                identityInfo.getBid(), identityInfo.getBgroup(), Constants.Server.hotKeyMonitorCheckCacheMaxCapacity);
        this.checkMillis = ProxyDynamicConf.getLong("hot.key.monitor.counter.check.millis",
                identityInfo.getBid(), identityInfo.getBgroup(), Constants.Server.hotKeyCacheCounterCheckMillis);
        // LRUCounter
        this.counter = new LRUCounter(checkCacheMaxCapacity,
                checkCacheMaxCapacity, checkMillis);
        ExecutorUtils.scheduleAtFixedRate(this::callback, checkMillis,
                checkMillis, TimeUnit.MILLISECONDS);
        logger.info("HotKeyHunter init success, identityInfo = {}", identityInfo);
    }

    private void reloadHotKeyConfig() {
        Long bid = identityInfo.getBid();
        String bgroup = identityInfo.getBgroup();
        this.maxHotKeyCount = ProxyDynamicConf.getInt("hot.key.monitor.max.hot.key.count",
                bid, bgroup, Constants.Server.hotKeyMonitorMaxHotKeyCount);
        this.checkThreshold = ProxyDynamicConf.getLong("hot.key.monitor.counter.check.threshold",
                bid, bgroup, Constants.Server.hotKeyMonitorCheckThreshold);
        this.enable = ProxyDynamicConf.getBoolean("hot.key.monitor.enable", bid, bgroup, true);
    }

    public void incr(byte[]... keys) {
        if (!enable) return;
        for (byte[] key : keys) {
            incr(key);
        }
    }

    public void incr(List<byte[]> keys) {
        if (!enable) return;
        for (byte[] key : keys) {
            incr(key);
        }
    }

    private void incr(byte[] key) {
        BytesKey bytesKey = new BytesKey(key);
        counter.increment(bytesKey);
    }

    private void callback() {
        try {
            TreeSet<LRUCounter.SortedBytesKey> set = counter.getSortedCacheValue(checkThreshold);
            if (set == null || set.isEmpty()) return;
            List<HotKeyInfo> hotKeys = new ArrayList<>(maxHotKeyCount);
            for (LRUCounter.SortedBytesKey sortedBytesKey : set) {
                hotKeys.add(new HotKeyInfo(sortedBytesKey.getKey(), sortedBytesKey.getCount()));
                if (hotKeys.size() >= maxHotKeyCount) {
                    break;
                }
            }
            HotKeyMonitor.hotKey(identityInfo, hotKeys, checkMillis, checkThreshold);
            ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> callback.callback(identityInfo, hotKeys, checkMillis, checkThreshold));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}

```

整体代码相对简单，主要是[LRUCounter](#LRUCounter)计数，以及通过提交回调函数进线程池来实现TOPN 热key统计。内部实现了LoggingHotKeyMonitorCallback回调。
