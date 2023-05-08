package com.netease.nim.camellia.hot.key.sdk.netty;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;

/**
 * Created by caojiajun on 2023/5/8
 */
public abstract class HotKeyServerDiscovery implements CamelliaDiscovery<HotKeyServerAddr> {


    public abstract String getName();

}
