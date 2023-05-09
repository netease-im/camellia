package com.netease.nim.camellia.hot.key.sdk;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.model.Rule;
import com.netease.nim.camellia.hot.key.common.utils.RuleUtils;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2023/5/9
 */
public abstract class CamelliaHotKeyAbstractSdk {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaHotKeyAbstractSdk.class);

    private final AtomicBoolean scheduleLock = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, HotKeyConfig> hotKeyConfigCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicBoolean> hotKeyConfigListenerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicBoolean> hotKeyConfigLockMap = new ConcurrentHashMap<>();

    private final CamelliaHotKeySdk sdk;
    private final ThreadPoolExecutor executor;

    public CamelliaHotKeyAbstractSdk(CamelliaHotKeySdk sdk, ThreadPoolExecutor executor,
                                     ScheduledExecutorService scheduler, int hotKeyConfigReloadIntervalSeconds) {
        this.sdk = sdk;
        this.executor = executor;
        scheduler.scheduleAtFixedRate(this::reloadHotKeyConfig, hotKeyConfigReloadIntervalSeconds,
                hotKeyConfigReloadIntervalSeconds, TimeUnit.SECONDS);
    }

    /**
     * 预热namespace的配置
     * 从而避免push方法丢弃初始的一些上报
     * @param namespace 预热
     */
    public void preheat(String namespace) {
        reloadHotKeyConfig(namespace);
    }

    /**
     * 判断是否匹配规则
     * @param namespace namespace
     * @param key key
     * @return 匹配的rule，如果没有通过则返回null
     */
    protected final Rule rulePass(String namespace, String key) {
        return RuleUtils.rulePass(getHotKeyConfig(namespace), key);
    }

    /**
     * 获取HotKeyConfig
     * @param namespace namespace
     * @return HotKeyConfig
     */
    protected final HotKeyConfig getHotKeyConfig(String namespace) {
        HotKeyConfig hotKeyConfig = hotKeyConfigCache.get(namespace);
        if (hotKeyConfig != null) {
            return hotKeyConfig;
        }
        AtomicBoolean lock = CamelliaMapUtils.computeIfAbsent(hotKeyConfigLockMap, namespace, k -> new AtomicBoolean(false));
        if (lock.compareAndSet(false, true)) {
            try {
                executor.submit(() -> {
                    try {
                        reloadHotKeyConfig(namespace);
                    } finally {
                        lock.compareAndSet(true, false);
                    }
                });
            } catch (Exception e) {
                logger.error("submit reloadHotKeyConfig task error, namespace = {}", namespace, e);
                lock.compareAndSet(true, false);
            }
        }
        return hotKeyConfigCache.get(namespace);
    }

    private void reloadHotKeyConfig(String namespace) {
        try {
            HotKeyConfig hotKeyConfig = sdk.getHotKeyConfig(namespace);
            HotKeyConfig old = hotKeyConfigCache.put(namespace, hotKeyConfig);
            if (old == null) {
                logger.info("reloadHotKeyConfig success, namespace = {}, hotKeyConfig = {}", namespace, JSONObject.toJSONString(hotKeyConfig));
            } else {
                if (!JSONObject.toJSONString(old).equals(JSONObject.toJSONString(hotKeyConfig))) {
                    logger.info("reloadHotKeyConfig success, namespace = {}, hotKeyConfig = {}", namespace, JSONObject.toJSONString(hotKeyConfig));
                }
            }
            AtomicBoolean addListener = CamelliaMapUtils.computeIfAbsent(hotKeyConfigListenerCache, namespace, k -> new AtomicBoolean(false));
            if (addListener.compareAndSet(false, true)) {
                //只加一次即可
                sdk.addListener(namespace, (CamelliaHotKeyConfigListener) config -> {
                            hotKeyConfigCache.put(namespace, config);
                            logger.info("HotKeyConfig update, namespace = {}, config = {}", namespace, JSONObject.toJSONString(config));
                        }
                );
            }
        } catch (Exception e) {
            logger.error("reloadHotKeyConfig error, namespace = {}", namespace, e);
        }
    }

    private void reloadHotKeyConfig() {
        if (scheduleLock.compareAndSet(false, true)) {
            try {
                for (Map.Entry<String, HotKeyConfig> entry : hotKeyConfigCache.entrySet()) {
                    reloadHotKeyConfig(entry.getKey());
                }
            } catch (Exception e) {
                logger.error("reloadHotKeyConfig error", e);
            } finally {
                scheduleLock.compareAndSet(true, false);
            }
        }
    }
}
