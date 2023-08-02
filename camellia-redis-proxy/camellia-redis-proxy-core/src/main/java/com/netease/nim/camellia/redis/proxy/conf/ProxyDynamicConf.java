package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.tools.utils.ConfigurationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Refresh the configuration dynamically.
 * Created by caojiajun on 2021/1/5
 */
public class ProxyDynamicConf {

    private static final Logger logger = LoggerFactory.getLogger(ProxyDynamicConf.class);

    private static Map<String, String> conf = new HashMap<>();
    private static final Set<DynamicConfCallback> callbackSet = new HashSet<>();

    private static final AtomicBoolean reloading = new AtomicBoolean(false);

    private static ProxyDynamicConfLoader loader = new FileBasedProxyDynamicConfLoader();

    private static final ConcurrentHashMap<String, Integer> intCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> longCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> booleanCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Double> doubleCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> stringCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();


    /**
     * 初始化ProxyDynamicConf
     *
     * @param initConf 初始配置，来自yml文件
     * @param loader   loader
     */
    public static void init(Map<String, String> initConf, ProxyDynamicConfLoader loader) {
        ProxyDynamicConf.loader = loader;
        if (initConf != null && !initConf.isEmpty()) {
            loader.init(initConf);
        } else {
            loader.init(new HashMap<>());
        }
        reload();
        int reloadIntervalSeconds = ConfigurationUtil.getInteger(conf, "dynamic.conf.reload.interval.seconds", 600);
        ExecutorUtils.scheduleAtFixedRate(ProxyDynamicConf::reload, reloadIntervalSeconds, reloadIntervalSeconds, TimeUnit.SECONDS);
        logger.info("ProxyDynamicConf init, loader = {}, reloadIntervalSeconds = {}", loader.getClass().getName(), reloadIntervalSeconds);
    }

    /**
     * 检查配置文件是否有变更。如果有，则重新加载，清空缓存，触发监听者的回调。因为不止一个地方调用，所以需要用CAS来防止并发。
     * Check the local configuration file for changes.
     * If so, reload, clear the cache, and trigger the callback of the listener.
     * Because more than one method calls this function, CAS is needed to prevent concurrency.
     */
    public static void reload() {
        if (reloading.compareAndSet(false, true)) {
            try {
                Map<String, String> newConf = loader.load();
                if (newConf.equals(new HashMap<>(ProxyDynamicConf.conf))) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("ProxyDynamicConf not modify");
                    }
                } else {
                    ProxyDynamicConf.conf = newConf;
                    if (logger.isInfoEnabled()) {
                        logger.info("ProxyDynamicConf updated, conf.size = {}, conf = {}", newConf.size(), newConf);
                    }
                    clearCache();
                    triggerCallback();
                }
            } catch (Exception e) {
                logger.error("reload error", e);
            } finally {
                reloading.compareAndSet(true, false);
            }
        } else {
            logger.warn("ProxyDynamicConf is reloading, skip current reload");
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
        cache.clear();
    }

    /**
     * 注册回调，对于ProxyDynamicConf的调用者，可以注册该回调，从而当ProxyDynamicConf发生配置变更时可以第一时间重新加载
     *
     * @param callback 回调
     */
    public static void registerCallback(DynamicConfCallback callback) {
        if (callback != null) {
            callbackSet.add(callback);
        }
    }

    // Get value from {@link ProxyDynamicConf#conf}. If value is null , this method will return defaultValue.
    private static Integer _getInt(String key, Integer defaultValue) {
        return ConfigurationUtil.getInteger(conf, key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        return _getInt(key, defaultValue);
    }

    public static int getInt(String key, Long bid, String bgroup, int defaultValue) {
        try {
            if (conf.isEmpty()) return defaultValue;
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
        return ConfigurationUtil.getLong(conf, key, defaultValue);
    }

    public static long getLong(String key, long defaultValue) {
        return _getLong(key, defaultValue);
    }

    public static long getLong(String key, Long bid, String bgroup, long defaultValue) {
        try {
            if (conf.isEmpty()) return defaultValue;
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
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static Double _getDouble(String key, Double defaultValue) {
        return ConfigurationUtil.getDouble(conf, key, defaultValue);
    }

    public static double getDouble(String key, double defaultValue) {
        return _getDouble(key, defaultValue);
    }

    public static double getDouble(String key, Long bid, String bgroup, double defaultValue) {
        try {
            if (conf.isEmpty()) return defaultValue;
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
        return ConfigurationUtil.get(conf, key, defaultValue);
    }

    public static String getString(String key, String defaultValue) {
        return _getString(key, defaultValue);
    }

    public static String getString(String key, Long bid, String bgroup, String defaultValue) {
        try {
            if (conf.isEmpty()) return defaultValue;
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


    private static <T> T get(String key, T defaultValue, Class<T> tClass) {
        return ConfigurationUtil.get(conf, key, defaultValue, tClass);
    }

    /**
     * Get value from {@link ProxyDynamicConf#conf}. Use caching to avoid duplicate creation.
     *
     * @param key          key
     * @param bid          id
     * @param bgroup       group
     * @param defaultValue defaultValue
     * @param tClass       the type of the return value
     * @param <T>          T
     * @return value
     */
    public static <T> T get(String key, Long bid, String bgroup, T defaultValue, Class<T> tClass) {
        try {
            if (conf.isEmpty()) return defaultValue;
            String confKey = buildConfKey(key, bid, bgroup);
            T value;
            T cacheValue = (T) cache.get(confKey);
            if (cacheValue != null) return cacheValue;
            value = get(confKey, null, tClass);
            if (value == null) {
                value = get(key, null, tClass);
            }
            if (value == null) {
                cache.put(confKey, defaultValue);
                return defaultValue;
            }
            cache.put(confKey, value);
            return value;
        } catch (Exception e) {
            return defaultValue;
        }
    }


    /**
     * Build config key from key，bid，bgroup. If bid == null and bgroup == null, use default.default+key
     *
     * @param key    key
     * @param bid    id
     * @param bgroup group
     * @return conf string
     */
    private static String buildConfKey(String key, Long bid, String bgroup) {
        if (bid == null || bgroup == null) {
            return "default.default." + key;
        } else {
            return bid + "." + bgroup + "." + key;
        }
    }
}
