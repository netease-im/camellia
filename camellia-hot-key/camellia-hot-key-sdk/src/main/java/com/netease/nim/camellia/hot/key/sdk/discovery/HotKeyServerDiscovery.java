package com.netease.nim.camellia.hot.key.sdk.discovery;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyServerAddr;

/**
 * Created by caojiajun on 2023/5/8
 */
public interface HotKeyServerDiscovery extends CamelliaDiscovery<HotKeyServerAddr> {

    /**
     * 每个HotKeyServerDiscovery应该有一个唯一的名字
     * @return 名字
     */
    String getName();

}
