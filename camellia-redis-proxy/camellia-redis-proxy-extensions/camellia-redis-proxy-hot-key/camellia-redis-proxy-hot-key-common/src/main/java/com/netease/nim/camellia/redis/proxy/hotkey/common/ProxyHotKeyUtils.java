package com.netease.nim.camellia.redis.proxy.hotkey.common;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;

public class ProxyHotKeyUtils {
    public static int getRequestOrder(String alias, int defaultValue) {
        return ProxyDynamicConf.getInt(alias + ".extension.plugin.request.order", defaultValue);
    }

    public static int getReplyOrder(String alias, int defaultValue) {
        return ProxyDynamicConf.getInt(alias + ".extension.plugin.reply.order", defaultValue);
    }
}
