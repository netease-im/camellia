package com.netease.nim.camellia.hot.key.extensions.discovery.zk;

import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyServerAddr;
import com.netease.nim.camellia.zk.ZkRegistry;


/**
 *
 * Created by caojiajun on 2020/8/12
 */
public class ZkHotKeyServerRegistry extends ZkRegistry<HotKeyServerAddr> {

    public ZkHotKeyServerRegistry(String zkUrl, String basePath, String applicationName, HotKeyServerAddr addr) {
        super(zkUrl, basePath, applicationName, addr);
    }

    public ZkHotKeyServerRegistry(String zkUrl, int sessionTimeoutMs, int connectionTimeoutMs,
                                  int baseSleepTimeMs, int maxRetries, String basePath, String applicationName, HotKeyServerAddr addr) {
        super(zkUrl, sessionTimeoutMs, connectionTimeoutMs, baseSleepTimeMs, maxRetries, basePath, applicationName, addr);
    }

}
