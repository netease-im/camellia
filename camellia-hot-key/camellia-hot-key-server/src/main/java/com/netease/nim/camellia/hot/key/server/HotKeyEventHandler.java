package com.netease.nim.camellia.hot.key.server;

import com.netease.nim.camellia.hot.key.common.model.HotKey;
import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.model.NamespaceType;

/**
 * Created by caojiajun on 2023/5/9
 */
public class HotKeyEventHandler {

    private final CacheableHotKeyConfigService hotKeyConfigService;

    public HotKeyEventHandler(CacheableHotKeyConfigService hotKeyConfigService) {
        this.hotKeyConfigService = hotKeyConfigService;
    }

    /**
     * 检测到热key
     * @param hotKey 热key
     */
    public void hotKey(HotKey hotKey) {
        HotKeyConfig hotKeyConfig = hotKeyConfigService.get(hotKey.getNamespace());
        if (hotKeyConfig.getType() == NamespaceType.cache) {
            //todo notify
        }
        //todo metrics
        //todo custom export
    }
}
