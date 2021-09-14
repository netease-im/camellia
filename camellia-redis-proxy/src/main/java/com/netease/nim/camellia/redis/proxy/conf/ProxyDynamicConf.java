package com.netease.nim.camellia.redis.proxy.conf;

import com.alibaba.fastjson.JSONArray;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2021/1/5
 */
public class ProxyDynamicConf {

    private static final Logger logger = LoggerFactory.getLogger(ProxyDynamicConf.class);

    private static Map<String, String> conf = new HashMap<>();
    private static final Set<DynamicConfCallback> callbackSet = new HashSet<>();
    private static final String fileName = "camellia-redis-proxy.properties";

    private static final ConcurrentHashMap<String, Integer> intCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> longCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> booleanCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Double> doubleCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> stringCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Set<String>> setCache = new ConcurrentHashMap<>();

    private static ProxyDynamicConfHook hook;

    static {
        reload();
        int reloadIntervalSeconds = confReloadIntervalSeconds();
        ExecutorUtils.scheduleAtFixedRate(ProxyDynamicConf::reload, reloadIntervalSeconds, reloadIntervalSeconds, TimeUnit.SECONDS);
    }

    public static void updateProxyDynamicConfHook(ProxyDynamicConfHook hook) {
        ProxyDynamicConf.hook = hook;
        logger.info("proxyDynamicConfHook update, hook = {}", hook.getClass().getName());
    }

    /**
     * 检查本地配置文件是否有变更，如果有，则重新加载，并且会清空缓存，并触发监听者的回调
     */
    public static void reload() {
        URL url = ProxyDynamicConf.class.getClassLoader().getResource(fileName);
        if (url == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("{} not exists", fileName);
            }
            clearCache();
            triggerCallback();
            return;
        }
        try {
            Properties props = new Properties();
            try {
                props.load(new FileInputStream(new File(url.getPath())));
            } catch (IOException e) {
                props.load(ProxyDynamicConf.class.getClassLoader().getResourceAsStream(fileName));
            }
            Map<String, String> conf = ConfigurationUtil.propertiesToMap(props);

            //如果想用另外一个文件来配置，可以在camellia-redis-proxy.properties中配置dynamic.conf.file.path=xxx
            //xxx需要是文件的绝对路径
            String filePath = conf.get("dynamic.conf.file.path");
            if (filePath != null) {
                try {
                    Properties props1 = new Properties();
                    props1.load(new FileInputStream(new File(filePath)));
                    Map<String, String> conf1 = ConfigurationUtil.propertiesToMap(props1);
                    if (conf1 != null) {
                        conf.putAll(conf1);
                    }
                } catch (Exception e) {
                    logger.error("dynamic.conf.file.path={} load error, use classpath:{} default", filePath, fileName, e);
                }
            }

            if (conf.equals(new HashMap<>(ProxyDynamicConf.conf))) {
                if (logger.isDebugEnabled()) {
                    if (filePath != null) {
                        logger.debug("classpath:{} and {} not modify", fileName, filePath);
                    } else {
                        logger.debug("classpath:{} not modify", fileName);
                    }
                }
                if (hook != null) {
                    clearCache();
                    triggerCallback();
                    logger.info("dynamic conf reload success");
                }
            } else {
                ProxyDynamicConf.conf = conf;
                if (filePath != null) {
                    logger.info("classpath:{} and {} reload success", fileName, filePath);
                } else {
                    logger.info("classpath:{} reload success", fileName);
                }
                clearCache();
                triggerCallback();
            }
        } catch (Exception e) {
            logger.error("reload error", e);
        }
    }

    /**
     * 直接把配置设置进来（k-v的map）
     */
    public static void reload(Map<String, String> conf) {
        try {
            HashMap<String, String> newConf = new HashMap<>(conf);
            if (ProxyDynamicConf.conf.equals(newConf)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("conf not modify");
                }
            } else {
                ProxyDynamicConf.conf = newConf;
                logger.info("conf reload success");
                clearCache();
                triggerCallback();
            }
        } catch (Exception e) {
            logger.error("reload error");
        }
    }

    // 触发一下监听者的回调
    private static void triggerCallback() {
        for (DynamicConfCallback callback : callbackSet) {
            try {
                callback.callback();
            } catch (Exception e) {
                logger.error("DynamicConfCallback callback error", e);
            }
        }
    }

    //清空缓存
    private static void clearCache() {
        longCache.clear();
        intCache.clear();
        booleanCache.clear();
        doubleCache.clear();
        stringCache.clear();
        setCache.clear();
    }

    /**
     * 注册回调，对于ProxyDynamicConf的调用者，可以注册该回调，从而当ProxyDynamicConf发生配置变更时可以第一时间重新加载
     * @param callback 回调
     */
    public static void registerCallback(DynamicConfCallback callback) {
        if (callback != null) {
            callbackSet.add(callback);
        }
    }

    //配置reload间隔，只有启动时设置有效
    public static int confReloadIntervalSeconds() {
        return ConfigurationUtil.getInteger(conf, "dynamic.conf.reload.interval.seconds", 600);
    }

    //某个redis后端连续几次连不上后触发熔断
    public static int failCountThreshold(int defaultValue) {
        if (hook != null) {
            Integer value = hook.failCountThreshold();
            if (value != null) return value;
        }
        return ConfigurationUtil.getInteger(conf, "redis.client.fail.count.threshold", defaultValue);
    }

    //某个redis后端连不上触发熔断后，熔断多少ms
    public static long failBanMillis(long defaultValue) {
        if (hook != null) {
            Long value = hook.failBanMillis();
            if (value != null) return value;
        }
        return ConfigurationUtil.getLong(conf, "redis.client.fail.ban.millis", defaultValue);
    }

    //monitor enable，当前仅当application.yml里的monitor-enable=true，才能通过本配置在进程运行期间进行动态的执行开启关闭的操作
    public static boolean monitorEnable(boolean defaultValue) {
        if (hook != null) {
            Boolean value = hook.monitorEnable();
            if (value != null) return value;
        }
        return ConfigurationUtil.getBoolean(conf, "monitor.enable", defaultValue);
    }

    //command spend time monitor enable，当前仅当application.yml里的command-spend-time-monitor-enable=true，才能通过本配置在进程运行期间进行动态的执行开启关闭的操作
    public static boolean commandSpendTimeMonitorEnable(boolean defaultValue) {
        if (hook != null) {
            Boolean value = hook.commandSpendTimeMonitorEnable();
            if (value != null) return value;
        }
        return ConfigurationUtil.getBoolean(conf, "command.spend.time.monitor.enable", defaultValue);
    }

    //slow command threshold, ms
    public static long slowCommandThresholdMillisTime(long millis) {
        if (hook != null) {
            Long value = hook.slowCommandThresholdMillisTime();
            if (value != null) return value;
        }
        return ConfigurationUtil.getLong(conf, "slow.command.threshold.millis", millis);
    }

    //hot key monitor enable，当前仅当application.yml里的hot-key-monitor-enable=true，才能通过本配置在进程运行期间进行动态的执行开启关闭的操作
    public static boolean hotKeyMonitorEnable(Long bid, String bgroup, boolean defaultValue) {
        if (hook != null) {
            Boolean value = hook.hotKeyMonitorEnable(bid, bgroup);
            if (value != null) return value;
        }
        return getBoolean("hot.key.monitor.enable", bid, bgroup, defaultValue);
    }

    //hot key monitor threshold
    public static long hotKeyMonitorThreshold(Long bid, String bgroup, long defaultValue) {
        if (hook != null) {
            Long value = hook.hotKeyMonitorThreshold(bid, bgroup);
            if (value != null) return value;
        }
        return getLong("hot.key.monitor.threshold", bid, bgroup, defaultValue);
    }

    //hot key cache enable，当前仅当application.yml里的hot-key-cache-enable=true，才能通过本配置在进程运行期间进行动态的执行开启关闭的操作
    public static boolean hotKeyCacheEnable(Long bid, String bgroup, boolean enable) {
        if (hook != null) {
            Boolean value = hook.hotKeyCacheEnable(bid, bgroup);
            if (value != null) return value;
        }
        return getBoolean("hot.key.cache.enable", bid, bgroup, enable);
    }

    //hot key cache need cache null
    public static boolean hotKeyCacheNeedCacheNull(Long bid, String bgroup, boolean enable) {
        if (hook != null) {
            Boolean value = hook.hotKeyCacheNeedCacheNull(bid, bgroup);
            if (value != null) return value;
        }
        return getBoolean("hot.key.cache.need.cache.null", bid, bgroup, enable);
    }

    //hot key cache threshold
    public static long hotKeyCacheThreshold(Long bid, String bgroup, long defaultValue) {
        if (hook != null) {
            Long value = hook.hotKeyCacheThreshold(bid, bgroup);
            if (value != null) return value;
        }
        return getLong("hot.key.cache.threshold", bid, bgroup, defaultValue);
    }

    //hot key cache key prefix，当前仅当application.yml里的hot-key-cache-key-checker-class-name设置为com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.PrefixMatchHotKeyCacheKeyChecker才有效
    public static Set<String> hotKeyCacheKeyPrefix(Long bid, String bgroup) {
        if (hook != null) {
            Set<String> value = hook.hotKeyCacheKeyPrefix(bid, bgroup);
            if (value != null) return value;
        }
        if (conf.isEmpty()) return Collections.emptySet();
        String key = buildConfKey("hot.key.cache.key.prefix", bid, bgroup);
        Set<String> cache = setCache.get(key);
        if (cache != null) return cache;
        Set<String> set = new HashSet<>();
        String string = ConfigurationUtil.get(conf, key, null);
        if (string != null) {
            try {
                JSONArray array = JSONArray.parseArray(string);
                for (Object o : array) {
                    set.add(String.valueOf(o));
                }
            } catch (Exception ignore) {
            }
        }
        setCache.put(key, set);
        return set;
    }

    //big key monitor enable，当前仅当application.yml里的big-key-monitor-enable=true，才能通过本配置在进程运行期间进行动态的执行开启关闭的操作
    public static boolean bigKeyMonitorEnable(Long bid, String bgroup, boolean defaultValue) {
        if (hook != null) {
            Boolean value = hook.bigKeyMonitorEnable(bid, bgroup);
            if (value != null) return value;
        }
        return getBoolean("big.key.monitor.enable", bid, bgroup, defaultValue);
    }

    //big key monitor hash threshold
    public static int bigKeyMonitorHashThreshold(Long bid, String bgroup, int defaultValue) {
        if (hook != null) {
            Integer value = hook.bigKeyMonitorHashThreshold(bid, bgroup);
            if (value != null) return value;
        }
        return getInt("big.key.monitor.hash.threshold", bid, bgroup, defaultValue);
    }

    //big key monitor string threshold
    public static int bigKeyMonitorStringThreshold(Long bid, String bgroup, int defaultValue) {
        if (hook != null) {
            Integer value = hook.bigKeyMonitorStringThreshold(bid, bgroup);
            if (value != null) return value;
        }
        return getInt("big.key.monitor.string.threshold", bid, bgroup, defaultValue);
    }

    //big key monitor set threshold
    public static int bigKeyMonitorSetThreshold(Long bid, String bgroup, int defaultValue) {
        if (hook != null) {
            Integer value = hook.bigKeyMonitorSetThreshold(bid, bgroup);
            if (value != null) return value;
        }
        return getInt("big.key.monitor.set.threshold", bid, bgroup, defaultValue);
    }

    //big key monitor zset threshold
    public static int bigKeyMonitorZSetThreshold(Long bid, String bgroup, int defaultValue) {
        if (hook != null) {
            Integer value = hook.bigKeyMonitorZSetThreshold(bid, bgroup);
            if (value != null) return value;
        }
        return getInt("big.key.monitor.zset.threshold", bid, bgroup, defaultValue);
    }

    //big key monitor list threshold
    public static int bigKeyMonitorListThreshold(Long bid, String bgroup, int defaultValue) {
        if (hook != null) {
            Integer value = hook.bigKeyMonitorListThreshold(bid, bgroup);
            if (value != null) return value;
        }
        return getInt("big.key.monitor.list.threshold", bid, bgroup, defaultValue);
    }

    private static Integer _getInt(String key, Integer defaultValue) {
        if (hook != null) {
            Integer value = hook.getInt(key);
            if (value != null) return value;
        }
        return ConfigurationUtil.getInteger(conf, key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        return _getInt(key, defaultValue);
    }

    public static int getInt(String key, Long bid, String bgroup, int defaultValue) {
        try {
            if (conf.isEmpty() && hook == null) return defaultValue;
            String confKey = buildConfKey(key, bid, bgroup);
            Integer value;
            Integer cacheValue = intCache.get(confKey);
            if (cacheValue != null) return cacheValue;
            value = _getInt(confKey, null);
            if (value == null) {
                value = _getInt(key, null);
            }
            if (value == null) {
                intCache.put(confKey, defaultValue);
                return defaultValue;
            }
            intCache.put(confKey, value);
            return value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static Long _getLong(String key, Long defaultValue) {
        if (hook != null) {
            Long value = hook.getLong(key);
            if (value != null) return value;
        }
        return ConfigurationUtil.getLong(conf, key, defaultValue);
    }

    public static long getLong(String key, long defaultValue) {
        return _getLong(key, defaultValue);
    }

    public static long getLong(String key, Long bid, String bgroup, long defaultValue) {
        try {
            if (conf.isEmpty() && hook == null) return defaultValue;
            String confKey = buildConfKey(key, bid, bgroup);
            Long value;
            Long cacheValue = longCache.get(confKey);
            if (cacheValue != null) return cacheValue;
            value = _getLong(confKey, null);
            if (value == null) {
                value = _getLong(key, null);
            }
            if (value == null) {
                longCache.put(confKey, defaultValue);
                return defaultValue;
            }
            longCache.put(confKey, value);
            return value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return ConfigurationUtil.getBoolean(conf, key, defaultValue);
    }

    public static boolean getBoolean(String key, Long bid, String bgroup, boolean defaultValue) {
        try {
            if (conf.isEmpty() && hook == null) return defaultValue;
            String confKey = buildConfKey(key, bid, bgroup);
            Boolean value;
            Boolean cacheValue = booleanCache.get(confKey);
            if (cacheValue != null) return cacheValue;
            value = ConfigurationUtil.getBoolean(conf, confKey, null);
            if (value == null) {
                value = ConfigurationUtil.getBoolean(conf, key, null);
            }
            if (value == null) {
                booleanCache.put(confKey, defaultValue);
                return defaultValue;
            }
            booleanCache.put(confKey, value);
            return value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static Double _getDouble(String key, Double defaultValue) {
        if (hook != null) {
            Double value = hook.getDouble(key);
            if (value != null) return value;
        }
        return ConfigurationUtil.getDouble(conf, key, defaultValue);
    }

    public static double getDouble(String key, double defaultValue) {
        return _getDouble(key, defaultValue);
    }

    public static double getDouble(String key, Long bid, String bgroup, double defaultValue) {
        try {
            if (conf.isEmpty() && hook == null) return defaultValue;
            String confKey = buildConfKey(key, bid, bgroup);
            Double value;
            Double cacheValue = doubleCache.get(confKey);
            if (cacheValue != null) return cacheValue;
            value = _getDouble(confKey, null);
            if (value == null) {
                value = _getDouble(key, null);
            }
            if (value == null) {
                doubleCache.put(confKey, defaultValue);
                return defaultValue;
            }
            doubleCache.put(confKey, value);
            return value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String _getString(String key, String defaultValue) {
        if (hook != null) {
            String value = hook.getString(key);
            if (value != null) return value;
        }
        return ConfigurationUtil.get(conf, key, defaultValue);
    }

    public static String getString(String key, String defaultValue) {
        return _getString(key, defaultValue);
    }

    public static String getString(String key, Long bid, String bgroup, String defaultValue) {
        try {
            if (conf.isEmpty() && hook == null) return defaultValue;
            String confKey = buildConfKey(key, bid, bgroup);
            String value;
            String cacheValue = stringCache.get(confKey);
            if (cacheValue != null) return cacheValue;
            value = _getString(confKey, null);
            if (value == null) {
                value = _getString(key, null);
            }
            if (value == null) {
                stringCache.put(confKey, defaultValue);
                return defaultValue;
            }
            stringCache.put(confKey, value);
            return value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String buildConfKey(String key, Long bid, String bgroup) {
        if (bid == null || bgroup == null) {
            return  "default.default." + key;
        } else {
            return bid + "." + bgroup + "." + key;
        }
    }
}
