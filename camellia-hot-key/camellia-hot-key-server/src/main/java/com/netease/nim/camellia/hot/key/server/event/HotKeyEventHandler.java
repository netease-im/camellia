package com.netease.nim.camellia.hot.key.server.event;

import com.netease.nim.camellia.hot.key.common.model.*;
import com.netease.nim.camellia.hot.key.server.notify.HotKeyNotifyService;
import com.netease.nim.camellia.hot.key.server.callback.HotKeyCallbackManager;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.tools.cache.NamespaceCamelliaLocalCache;
import com.netease.nim.camellia.tools.executor.CamelliaHashedExecutor;
import com.netease.nim.camellia.tools.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyEventHandler.class);

    private final HotKeyNotifyService hotKeyNotifyService;
    private final NamespaceCamelliaLocalCache hotKeyCache;
    private final NamespaceCamelliaLocalCache keyUpdateCache;
    private final HotKeyCallbackManager callbackManager;
    private final CamelliaHashedExecutor executor;

    public HotKeyEventHandler(HotKeyServerProperties properties, HotKeyNotifyService hotKeyNotifyService, HotKeyCallbackManager callbackManager) {
        this.hotKeyNotifyService = hotKeyNotifyService;
        this.hotKeyCache = new NamespaceCamelliaLocalCache(properties.getMaxNamespace(), properties.getHotKeyCacheCapacity(), false);
        this.keyUpdateCache = new NamespaceCamelliaLocalCache(properties.getMaxNamespace(), properties.getHotKeyCacheCapacity(), false);
        this.callbackManager = callbackManager;
        logger.info("HotKeyEventHandler init success");
        executor = new CamelliaHashedExecutor("hot-key-event", SysUtils.getCpuNum(), 10000);
    }

    /**
     * 检测到热key
     * @param hotKey 热key
     * @param rule rule
     * @param current current
     */
    public void newHotKey(HotKey hotKey, Rule rule, long current) {
        try {
            executor.submit(hotKey.getNamespace(), () -> {
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("newHotKey, namespace = {}, key = {}, action = {}", hotKey.getNamespace(), hotKey.getKey(), hotKey.getAction());
                    }
                    if (hotKey.getAction() == KeyAction.QUERY && hotKey.getExpireMillis() != null) {
                        Long expireMillis = hotKeyCache.get(hotKey.getNamespace(), hotKey.getKey(), Long.class);
                        boolean needNotify = false;
                        if (expireMillis == null) {
                            //之前不属于热key，则需要下发通知
                            needNotify = true;
                        } else {
                            //之前属于热key，但是expireMillis已经过半，需要重新下发
                            long ttl = hotKeyCache.ttl(hotKey.getNamespace(), hotKey.getKey());
                            if (ttl < expireMillis / 2) {
                                needNotify = true;
                            }
                        }
                        if (needNotify) {
                            hotKeyCache.put(hotKey.getNamespace(), hotKey.getKey(), hotKey.getExpireMillis(), hotKey.getExpireMillis());
                            hotKeyNotifyService.notifyHotKey(hotKey);
                        }
                    }
                    callbackManager.newHotkey(hotKey, rule, current);
                } catch (Exception e) {
                    logger.error("newHotKey error");
                }
            });
        } catch (Exception e) {
            logger.error("submit newHotKey error", e);
        }
    }

    /**
     * 如果是热key，需要下发热key更新的通知（如key已经更新了，或者key已经删除了）
     * @param counter counter
     */
    public void hotKeyUpdate(KeyCounter counter) {
        try {
            //只有在热key缓存中，才需要下发变更通知
            Long expireMillis = hotKeyCache.get(counter.getNamespace(), counter.getKey(), Long.class);
            if (expireMillis == null) {
                return;
            }
            executor.submit(counter.getNamespace(), () -> {
                try {
                    //100ms内只下发一次
                    boolean success = keyUpdateCache.putIfAbsent(counter.getNamespace(), counter.getKey() + "|" + counter.getAction(), true, 100L);
                    if (success) {
                        HotKey hotKey = new HotKey(counter.getNamespace(), counter.getKey(), counter.getAction(), null);
                        hotKeyNotifyService.notifyHotKey(hotKey);
                    }
                } catch (Exception e) {
                    logger.error("hotKeyUpdate error", e);
                }
            });
        } catch (Exception e) {
            logger.error("submit hotKeyUpdate error", e);
        }
    }

}
