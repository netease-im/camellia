package com.netease.nim.camellia.hot.key.server.event;

import com.netease.nim.camellia.hot.key.common.model.*;
import com.netease.nim.camellia.hot.key.server.notify.HotKeyNotifyService;
import com.netease.nim.camellia.hot.key.server.callback.HotKeyCallbackManager;
import com.netease.nim.camellia.hot.key.server.conf.CacheableHotKeyConfigService;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.tools.cache.NamespaceCamelliaLocalCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyEventHandler.class);

    private final CacheableHotKeyConfigService hotKeyConfigService;
    private final HotKeyNotifyService hotKeyNotifyService;
    private final NamespaceCamelliaLocalCache hotKeyCache;
    private final NamespaceCamelliaLocalCache keyUpdateCache;
    private final HotKeyCallbackManager callbackManager;

    public HotKeyEventHandler(HotKeyServerProperties properties, CacheableHotKeyConfigService hotKeyConfigService,
                              HotKeyNotifyService hotKeyNotifyService, HotKeyCallbackManager callbackManager) {
        this.hotKeyConfigService = hotKeyConfigService;
        this.hotKeyNotifyService = hotKeyNotifyService;
        this.hotKeyCache = new NamespaceCamelliaLocalCache(properties.getMaxNamespace(), properties.getHotKeyCacheCapacity());
        this.keyUpdateCache = new NamespaceCamelliaLocalCache(properties.getMaxNamespace(), properties.getHotKeyCacheCapacity());
        this.callbackManager = callbackManager;
        logger.info("HotKeyEventHandler init success");
    }

    /**
     * 检测到热key
     * @param hotKey 热key
     */
    public void newHotKey(HotKey hotKey, Rule rule, ValueGetter getter) {
        if (logger.isDebugEnabled()) {
            logger.debug("newHotKey, namespace = {}, key = {}, action = {}", hotKey.getNamespace(), hotKey.getKey(), hotKey.getAction());
        }
        HotKeyConfig hotKeyConfig = hotKeyConfigService.get(hotKey.getNamespace());
        if (hotKeyConfig.getType() == NamespaceType.cache && hotKey.getAction() == KeyAction.QUERY && hotKey.getExpireMillis() != null) {
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
        callbackManager.newHotkey(hotKey, rule, getter);
    }

    /**
     * 如果是热key，需要下发热key更新的通知（如key已经更新了，或者key已经删除了）
     * @param counter counter
     */
    public void hotKeyUpdate(KeyCounter counter) {
        Long expireMillis = hotKeyCache.get(counter.getNamespace(), counter.getKey(), Long.class);
        //只有在热key缓存中，才需要下发变更通知
        if (expireMillis != null) {
            //100ms内只下发一次
            boolean success = keyUpdateCache.putIfAbsent(counter.getNamespace(), counter.getKey() + "|" + counter.getAction(), true, 100L);
            if (success) {
                HotKey hotKey = new HotKey(counter.getNamespace(), counter.getKey(), counter.getAction(), null);
                hotKeyNotifyService.notifyHotKey(hotKey);
            }
        }
    }

}
