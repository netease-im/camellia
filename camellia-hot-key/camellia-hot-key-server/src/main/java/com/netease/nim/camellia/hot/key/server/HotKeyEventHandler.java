package com.netease.nim.camellia.hot.key.server;

import com.netease.nim.camellia.hot.key.common.model.*;
import com.netease.nim.camellia.tools.cache.NamespaceCamelliaLocalCache;


/**
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyEventHandler {

    private final CacheableHotKeyConfigService hotKeyConfigService;
    private final HotKeyNotifyService hotKeyNotifyService;
    private final NamespaceCamelliaLocalCache hotKeyCache;

    public HotKeyEventHandler(HotKeyServerProperties properties, CacheableHotKeyConfigService hotKeyConfigService, HotKeyNotifyService hotKeyNotifyService) {
        this.hotKeyConfigService = hotKeyConfigService;
        this.hotKeyNotifyService = hotKeyNotifyService;
        this.hotKeyCache = new NamespaceCamelliaLocalCache(properties.getMaxNamespace(), properties.getHotKeyCacheCapacity());
    }

    /**
     * 检测到热key
     * @param hotKey 热key
     */
    public void newHotKey(HotKey hotKey) {
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
    }

    /**
     * 如果是热key，需要下发热key更新的通知（如key已经更新了，或者key已经删除了）
     */
    public void hotKeyUpdate(KeyCounter counter) {
        Long expireMillis = hotKeyCache.get(counter.getNamespace(), counter.getKey(), Long.class);
        if (expireMillis != null) {
            //只有在热key缓存中，才需要下发变更通知
            HotKey hotKey = new HotKey(counter.getNamespace(), counter.getKey(), counter.getAction(), null);
            hotKeyNotifyService.notifyHotKey(hotKey);
        }
    }

}
