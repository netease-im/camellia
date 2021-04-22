package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConfHook;

/**
 * Created by caojiajun on 2021/4/22
 */
public class CustomProxyDynamicConfHook extends ProxyDynamicConfHook {

    @Override
    public Boolean hotKeyCacheEnable(Long bid, String bgroup) {
        return true;
    }

    @Override
    public Long hotKeyMonitorThreshold(Long bid, String bgroup) {
        return 100L;
    }

}
