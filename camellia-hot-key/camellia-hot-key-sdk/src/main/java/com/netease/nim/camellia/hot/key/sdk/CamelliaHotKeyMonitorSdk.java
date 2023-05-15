package com.netease.nim.camellia.hot.key.sdk;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeyMonitorSdkConfig;
import com.netease.nim.camellia.hot.key.sdk.listener.CamelliaHotKeyListener;
import com.netease.nim.camellia.tools.cache.NamespaceCamelliaLocalCache;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Created by caojiajun on 2023/5/7
 */
public class CamelliaHotKeyMonitorSdk extends CamelliaHotKeyAbstractSdk implements ICamelliaHotKeyMonitorSdk {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaHotKeyMonitorSdk.class);

    private final CamelliaHotKeySdk sdk;
    private final CamelliaHotKeyMonitorSdkConfig config;

    private final ConcurrentHashMap<String, AtomicBoolean> hotKeyListenerCache = new ConcurrentHashMap<>();
    private final NamespaceCamelliaLocalCache hotKeyCacheKeyMap;

    public CamelliaHotKeyMonitorSdk(CamelliaHotKeySdk sdk, CamelliaHotKeyMonitorSdkConfig config) {
        super(sdk, config.getExecutor(), config.getScheduler(), config.getHotKeyConfigReloadIntervalSeconds());
        this.sdk = sdk;
        this.config = config;
        this.hotKeyCacheKeyMap = new NamespaceCamelliaLocalCache(config.getMaxNamespace(), config.getCapacity());
    }

    @Override
    public void preheat(String namespace) {
        super.preheat(namespace);
        addHotKeyListener(namespace);
    }

    @Override
    public Result push(String namespace, String key) {
        try {
            if (rulePass(namespace, key) != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("push {} {}", namespace, key);
                }
                sdk.push(namespace, key, KeyAction.QUERY);
                addHotKeyListener(namespace);
                Long expireMillis = hotKeyCacheKeyMap.get(namespace, key, Long.class);
                if (expireMillis == null) {
                    return Result.NOT_HOT;
                } else {
                    return Result.HOT;
                }
            }
            return Result.NOT_HOT;
        } catch (Exception e) {
            logger.error("push error, namespace = {}, key = {}", namespace, key, e);
            return Result.NOT_HOT;
        }
    }

    @Override
    public CamelliaHotKeyMonitorSdkConfig getConfig() {
        return config;
    }

    private void addHotKeyListener(String namespace) {
        AtomicBoolean lock = CamelliaMapUtils.computeIfAbsent(hotKeyListenerCache, namespace, k -> new AtomicBoolean(false));
        if (lock.get()) return;
        if (lock.compareAndSet(false, true)) {
            //listener 只加一次
            sdk.addListener(namespace, (CamelliaHotKeyListener) event -> {
                try {
                    KeyAction keyAction = event.getKeyAction();
                    if (keyAction == KeyAction.QUERY) {
                        hotKeyCacheKeyMap.put(event.getNamespace(), event.getKey(), event.getExpireMillis(), event.getExpireMillis());
                        logger.info("receive HotKeyEvent = {}", JSONObject.toJSONString(event));
                    }
                } catch (Exception e) {
                    logger.error("onHotKeyEvent error, event = {}", JSONObject.toJSONString(event), e);
                }
            });
        }
    }
}
