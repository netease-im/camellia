package com.netease.nim.camellia.redis.proxy.hotkey.monitor.plugin;

import com.netease.nim.camellia.hot.key.sdk.ICamelliaHotKeyMonitorSdk;
import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 基于hot-key-server发现机制的HotKeyMonitor
 * <p>HotKeyMonitor based on hot-key-server discovery mechanism
 */
public class HotKeyMonitor {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyMonitor.class);

    private final IdentityInfo identityInfo;

    /**
     * 全局唯一的sdk
     * <p>globally unique sdk
     */
    private final ICamelliaHotKeyMonitorSdk hotKeyMonitorSdk;

    private boolean enable;
    private String namespace;

    /**
     * @param identityInfo tenant identity information，bid + bgroup can represent one tenant.
     */
    public HotKeyMonitor(IdentityInfo identityInfo, HotKeyMonitorConfig config) {
        this.identityInfo = identityInfo;
        ProxyDynamicConf.registerCallback(this::reloadHotKeyCacheConfig);
        reloadHotKeyCacheConfig();

        this.hotKeyMonitorSdk = CamelliaHotKeyMonitorSdkFactory.getSdk(config);
        logger.info("HotKeyMonitor init success, identityInfo = {}", identityInfo);
    }

    public void push(byte[] key) {
        if (!enable) {
            return;
        }
        String keyStr = Utils.bytesToString(key);
        hotKeyMonitorSdk.push(namespace, keyStr, 1);
    }

    public void push(List<byte[]> keys) {
        if (!enable) {
            return;
        }
        keys.forEach(this::push);
    }

    private void reloadHotKeyCacheConfig() {
        Long bid = identityInfo.getBid();
        String bgroup = identityInfo.getBgroup();
        this.enable = ProxyDynamicConf.getBoolean("hot.key.monitor.enable", bid, bgroup, true);
        this.namespace = ProxyDynamicConf.getString("hot.key.server.monitor.namespace",
                identityInfo.getBid(), identityInfo.getBgroup(), Utils.getNamespaceOrSetDefault(identityInfo.getBid(), identityInfo.getBgroup()));
    }

}
