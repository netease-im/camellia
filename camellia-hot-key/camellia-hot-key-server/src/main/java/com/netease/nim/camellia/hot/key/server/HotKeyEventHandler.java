package com.netease.nim.camellia.hot.key.server;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.hot.key.common.model.*;
import com.netease.nim.camellia.tools.cache.CamelliaLocalCache;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;


/**
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyEventHandler {

    private final HotKeyServerProperties properties;
    private final CacheableHotKeyConfigService hotKeyConfigService;
    private final HotKeyNotifyService hotKeyNotifyService;
    private final ConcurrentLinkedHashMap<String, CamelliaLocalCache> hotKeyCacheMap;

    public HotKeyEventHandler(HotKeyServerProperties properties, CacheableHotKeyConfigService hotKeyConfigService, HotKeyNotifyService hotKeyNotifyService) {
        this.properties = properties;
        this.hotKeyConfigService = hotKeyConfigService;
        this.hotKeyNotifyService = hotKeyNotifyService;
        this.hotKeyCacheMap = new ConcurrentLinkedHashMap.Builder<String, CamelliaLocalCache>()
                .initialCapacity(properties.getMaxNamespace())
                .maximumWeightedCapacity(properties.getMaxNamespace())
                .build();
    }

    public CamelliaLocalCache getHotKeyCache(String namespace) {
        return CamelliaMapUtils.computeIfAbsent(hotKeyCacheMap, namespace, k -> new CamelliaLocalCache(properties.getHotKeyCacheCapacity()));
    }

    /**
     * 检测到热key
     * @param hotKey 热key
     */
    public void newHotKey(HotKey hotKey) {
        HotKeyConfig hotKeyConfig = hotKeyConfigService.get(hotKey.getNamespace());
        if (hotKeyConfig.getType() == NamespaceType.cache && hotKey.getAction() == KeyAction.QUERY && hotKey.getExpireMillis() != null) {
            CamelliaLocalCache hotKeyCache = getHotKeyCache(hotKey.getNamespace());
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
        CamelliaLocalCache hotKeyCache = getHotKeyCache(counter.getNamespace());
        Long expireMillis = hotKeyCache.get(counter.getNamespace(), counter.getKey(), Long.class);
        if (expireMillis != null) {
            //只有在热key缓存中，才需要下发变更通知
            HotKey hotKey = new HotKey(counter.getNamespace(), counter.getKey(), counter.getAction(), null);
            hotKeyNotifyService.notifyHotKey(hotKey);
        }
    }

}
