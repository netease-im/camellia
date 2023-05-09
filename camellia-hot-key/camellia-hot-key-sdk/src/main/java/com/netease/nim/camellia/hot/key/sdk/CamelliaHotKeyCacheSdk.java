package com.netease.nim.camellia.hot.key.sdk;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.common.model.Rule;
import com.netease.nim.camellia.tools.cache.CamelliaLocalCache;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2023/5/6
 */
public class CamelliaHotKeyCacheSdk extends CamelliaHotKeyAbstractSdk implements ICamelliaHotKeyCacheSdk {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaHotKeyCacheSdk.class);

    private final CamelliaHotKeySdk sdk;
    private final CamelliaHotKeyCacheSdkConfig config;

    private final ConcurrentHashMap<String, CamelliaLocalCache> hotKeyValueCacheMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CamelliaLocalCache> hotKeyCacheMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, AtomicBoolean> hotKeyListenerCache = new ConcurrentHashMap<>();

    public CamelliaHotKeyCacheSdk(CamelliaHotKeySdk sdk, CamelliaHotKeyCacheSdkConfig config) {
        super(sdk, config.getExecutor(), config.getScheduler(), config.getHotKeyConfigReloadIntervalSeconds());
        this.sdk = sdk;
        this.config = config;
        logger.info("CamelliaHotKeyCacheSdk init success");
    }

    @Override
    public void preheat(String namespace) {
        super.preheat(namespace);
        addHotKeyListener(namespace);
    }

    @Override
    public <T> T getValue(String namespace, String key, ValueLoader<T> loader, IValueLoaderLock loaderLock) {
        addHotKeyListener(namespace);
        //先看看是否匹配规则
        Rule rule = rulePass(namespace, key);
        if (rule == null) {
            return loader.load(key);
        }
        //提交探测请求
        sdk.push(namespace, key, KeyAction.QUERY);
        //看看是否是热key
        Boolean isHotKey = getHotKeyCache(namespace).get(namespace, key, Boolean.class);
        if (isHotKey == null || !isHotKey) {
            //如果不是热key，直接请求底层
            return loader.load(key);
        }
        //如果是热key，看看有没有本地缓存
        CamelliaLocalCache.ValueWrapper valueWrapper = getHotKeyValueCache(namespace).get(namespace, key);
        if (valueWrapper != null) {
            return (T) valueWrapper.get();
        }
        //没有本地缓存的情况下，看看是否设置了并发锁
        if (loaderLock == null) {
            //如果没有设置并发锁，直接请求底层
            T value = loader.load(key);
            //判断是否缓存null
            if (!config.isCacheNull() && value == null) {
                return null;
            }
            //回填到缓存中
            getHotKeyValueCache(namespace).put(namespace, key, value, rule.getExpireMillis());
            return value;
        }
        //如果设置了并发锁
        int retry = config.getLoadTryLockRetry();
        while (retry --> 0) {
            //尝试获取锁
            if (loaderLock.tryLock()) {
                try {
                    //如果获得了锁，则请求底层
                    T value = loader.load(key);
                    //判断是否缓存null
                    if (!config.isCacheNull() && value == null) {
                        return null;
                    }
                    //回填缓存
                    getHotKeyValueCache(namespace).put(namespace, key, value, rule.getExpireMillis());
                    return value;
                } finally {
                    //释放锁
                    loaderLock.release();
                }
            } else {
                //如果没有获取锁，则等待一下
                try {
                    TimeUnit.MILLISECONDS.sleep(config.getLoadTryLockSleepMs());
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
                //等待之后，再看看是否已经有缓存了
                valueWrapper = getHotKeyValueCache(namespace).get(namespace, key);
                if (valueWrapper != null) {
                    return (T) valueWrapper.get();
                }
            }
        }
        //锁等待超时了，则直接请求底层，并回填缓存
        //如果获得了锁，则请求底层
        T value = loader.load(key);
        //判断是否缓存null
        if (!config.isCacheNull() && value == null) {
            return null;
        }
        //回填缓存
        getHotKeyValueCache(namespace).put(namespace, key, value, rule.getExpireMillis());
        return value;
    }

    @Override
    public void keyUpdate(String namespace, String key) {
        addHotKeyListener(namespace);
        Rule rule = rulePass(config.getNamespace(), key);
        if (rule == null) {
            return;
        }
        sdk.push(config.getNamespace(), key, KeyAction.UPDATE);
    }

    @Override
    public void keyDelete(String namespace, String key) {
        addHotKeyListener(namespace);
        Rule rule = rulePass(config.getNamespace(), key);
        if (rule == null) {
            return;
        }
        sdk.push(config.getNamespace(), key, KeyAction.DELETE);
    }

    private void addHotKeyListener(String namespace) {
        AtomicBoolean lock = CamelliaMapUtils.computeIfAbsent(hotKeyListenerCache, namespace, k -> new AtomicBoolean(false));
        if (lock.get()) return;
        if (lock.compareAndSet(false, true)) {
            //listener 只加一次
            sdk.addListener(namespace, (CamelliaHotKeyListener) event -> {
                try {
                    logger.info("receive HotKeyEvent = {}", JSONObject.toJSONString(event));
                    KeyAction keyAction = event.getKeyAction();
                    if (keyAction == KeyAction.QUERY) {
                        getHotKeyCache(event.getNamespace()).put(event.getNamespace(), event.getKey(), true, event.getExpireMillis());
                    } else if (keyAction == KeyAction.DELETE || keyAction == KeyAction.UPDATE) {
                        getHotKeyCache(event.getNamespace()).evict(event.getNamespace(), event.getKey());
                        getHotKeyValueCache(event.getNamespace()).evict(event.getNamespace(), event.getKey());
                    }
                } catch (Exception e) {
                    logger.error("onHotKeyEvent error, event = {}", JSONObject.toJSONString(event), e);
                }
            });
        }
    }

    private CamelliaLocalCache getHotKeyValueCache(String namespace) {
        return CamelliaMapUtils.computeIfAbsent(hotKeyValueCacheMap, namespace, n -> new CamelliaLocalCache(config.getCapacity()));
    }

    private CamelliaLocalCache getHotKeyCache(String namespace) {
        return CamelliaMapUtils.computeIfAbsent(hotKeyCacheMap, namespace, n -> new CamelliaLocalCache(config.getCapacity()));
    }
}
