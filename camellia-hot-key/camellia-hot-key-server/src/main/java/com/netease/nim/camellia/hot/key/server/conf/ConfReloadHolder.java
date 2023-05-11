package com.netease.nim.camellia.hot.key.server.conf;

/**
 * Created by caojiajun on 2023/5/11
 */
public class ConfReloadHolder {

    private static CacheableHotKeyConfigService configService;

    public static void register(CacheableHotKeyConfigService configService) {
        ConfReloadHolder.configService = configService;
    }

    public static void reload() {
        if (configService != null) {
            configService.reload();
        }
    }
}
