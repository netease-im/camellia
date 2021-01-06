package com.netease.nim.camellia.redis.proxy.conf;

import com.alibaba.fastjson.JSONArray;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
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

    static {
        reload();
        ExecutorUtils.scheduleAtFixedRate(ProxyDynamicConf::reload, 10, 10, TimeUnit.MINUTES);
    }

    public static void reload() {
        URL url = ProxyDynamicConf.class.getClassLoader().getResource(fileName);
        if (url == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("{} not exists", fileName);
            }
            return;
        }
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(new File(url.getPath())));
            Map<String, String> conf = ConfigurationUtil.propertiesToMap(props);

            if (conf.equals(new HashMap<>(ProxyDynamicConf.conf))) {
                if (logger.isDebugEnabled()) {
                    logger.debug("{} not modify", fileName);
                }
            } else {
                ProxyDynamicConf.conf = conf;
                logger.info("{} reload success", fileName);
                clearCache();
                for (DynamicConfCallback callback : callbackSet) {
                    try {
                        callback.callback();
                    } catch (Exception e) {
                        logger.error("DynamicConfCallback callback error", e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("reload error", e);
        }
    }

    private static void clearCache() {
        longCache.clear();
        intCache.clear();
        booleanCache.clear();
        setCache.clear();
    }

    public static void registerCallback(DynamicConfCallback callback) {
        if (callback != null) {
            callbackSet.add(callback);
        }
    }

    //monitor enable，当前仅当application.yml里的monitor-enable=true，才能通过本配置在进程运行期间进行动态的执行开启关闭的操作
    public static boolean monitorEnable(boolean defaultValue) {
        return ConfigurationUtil.getBoolean(conf, "monitor.enable", defaultValue);
    }

    //command spend time monitor enable，当前仅当application.yml里的command-spend-time-monitor-enable=true，才能通过本配置在进程运行期间进行动态的执行开启关闭的操作
    public static boolean commandSpendTimeMonitorEnable(boolean defaultValue) {
        return ConfigurationUtil.getBoolean(conf, "command.spend.time.monitor.enable", defaultValue);
    }

    //slow command threshold, ms
    public static long slowCommandThresholdMillisTime(long millis) {
        return ConfigurationUtil.getLong(conf, "slow.command.threshold.millis", millis);
    }

    //hot key monitor enable，当前仅当application.yml里的hot-key-monitor-enable=true，才能通过本配置在进程运行期间进行动态的执行开启关闭的操作
    public static boolean hotKeyMonitorEnable(Long bid, String bgroup, boolean defaultValue) {
        return getBoolean("hot.key.monitor.enable", bid, bgroup, defaultValue);
    }

    //hot key monitor threshold
    public static long hotKeyMonitorThreshold(Long bid, String bgroup, long defaultValue) {
        return getLong("hot.key.monitor.threshold", bid, bgroup, defaultValue);
    }

    //hot key cache enable，当前仅当application.yml里的hot-key-cache-enable=true，才能通过本配置在进程运行期间进行动态的执行开启关闭的操作
    public static boolean hotKeyCacheEnable(Long bid, String bgroup, boolean enable) {
        return getBoolean("hot.key.cache.enable", bid, bgroup, enable);
    }

    //hot key cache need cache null
    public static boolean hotKeyCacheNeedCacheNull(Long bid, String bgroup, boolean enable) {
        return getBoolean("hot.key.cache.need.cache.null", bid, bgroup, enable);
    }

    //hot key cache threshold
    public static long hotKeyCacheThreshold(Long bid, String bgroup, long defaultValue) {
        return getLong("hot.key.cache.threshold", bid, bgroup, defaultValue);
    }

    //hot key cache key prefix，当前仅当application.yml里的hot-key-cache-key-checker-class-name设置为com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.PrefixMatchHotKeyCacheKeyChecker才有效
    private static final Map<String, Set<String>> setCache = new HashMap<>();
    public static Set<String> hotKeyCacheKeyPrefix(Long bid, String bgroup) {
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
        return getBoolean("big.key.monitor.enable", bid, bgroup, defaultValue);
    }

    //big key monitor hash threshold
    public static int bigKeyMonitorHashThreshold(Long bid, String bgroup, int defaultValue) {
        return getInt("big.key.monitor.hash.threshold", bid, bgroup, defaultValue);
    }

    //big key monitor string threshold
    public static int bigKeyMonitorStringThreshold(Long bid, String bgroup, int defaultValue) {
        return getInt("big.key.monitor.string.threshold", bid, bgroup, defaultValue);
    }

    //big key monitor set threshold
    public static int bigKeyMonitorSetThreshold(Long bid, String bgroup, int defaultValue) {
        return getInt("big.key.monitor.set.threshold", bid, bgroup, defaultValue);
    }

    //big key monitor zset threshold
    public static int bigKeyMonitorZSetThreshold(Long bid, String bgroup, int defaultValue) {
        return getInt("big.key.monitor.zset.threshold", bid, bgroup, defaultValue);
    }

    //big key monitor list threshold
    public static int bigKeyMonitorListThreshold(Long bid, String bgroup, int defaultValue) {
        return getInt("big.key.monitor.list.threshold", bid, bgroup, defaultValue);
    }

    private static final ConcurrentHashMap<String, Integer> intCache = new ConcurrentHashMap<>();
    private static int getInt(String key, Long bid, String bgroup, int defaultValue) {
        if (conf.isEmpty()) return defaultValue;
        String confKey = buildConfKey(key, bid, bgroup);
        Integer value;
        Integer cacheValue = intCache.get(confKey);
        if (cacheValue != null) return cacheValue;
        value = ConfigurationUtil.getInteger(conf, confKey, null);
        if (value == null) {
            value = ConfigurationUtil.getInteger(conf, key, null);
        }
        if (value == null) {
            intCache.put(confKey, defaultValue);
            return defaultValue;
        }
        intCache.put(confKey, value);
        return value;
    }

    private static final ConcurrentHashMap<String, Long> longCache = new ConcurrentHashMap<>();
    private static long getLong(String key, Long bid, String bgroup, long defaultValue) {
        if (conf.isEmpty()) return defaultValue;
        String confKey = buildConfKey(key, bid, bgroup);
        Long value;
        Long cacheValue = longCache.get(confKey);
        if (cacheValue != null) return cacheValue;
        value = ConfigurationUtil.getLong(conf, confKey, null);
        if (value == null) {
            value = ConfigurationUtil.getLong(conf, key, null);
        }
        if (value == null) {
            longCache.put(confKey, defaultValue);
            return defaultValue;
        }
        longCache.put(confKey, value);
        return value;
    }

    private static final ConcurrentHashMap<String, Boolean> booleanCache = new ConcurrentHashMap<>();
    private static boolean getBoolean(String key, Long bid, String bgroup, boolean defaultValue) {
        if (conf.isEmpty()) return defaultValue;
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
    }

    private static String buildConfKey(String key, Long bid, String bgroup) {
        if (bid == null || bgroup == null) {
            return  "default.default." + key;
        } else {
            return bid + "." + bgroup + "." + key;
        }
    }
}
