package com.netease.nim.camellia.redis.zk.registry.springboot;

import com.netease.nim.camellia.hot.key.extensions.discovery.zk.ZkHotKeyServerRegistry;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyServerAddr;
import com.netease.nim.camellia.hot.key.server.netty.ServerStatus;
import com.netease.nim.camellia.tools.utils.InetUtils;
import com.netease.nim.camellia.zk.ZkRegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;

/**
 * Created by caojiajun on 2023/5/15
 */
public class CamelliaHotKeyServerZkRegisterBoot {
    private static final Logger logger = LoggerFactory.getLogger(CamelliaHotKeyServerZkRegisterBoot.class);

    private ZkHotKeyServerRegistry registry;

    public CamelliaHotKeyServerZkRegisterBoot(CamelliaHotKeyServerZkRegistryProperties properties, String applicationName, int port) {
        if (!properties.isEnable()) {
            logger.info("camellia hot key server zk registry not enable");
            return;
        }
        if (properties.getZkUrl() == null) {
            logger.warn("zkUrl is null, skip camellia hot key server zk registry");
            return;
        }
        HotKeyServerAddr addr;
        try {
            String host = properties.getHost();
            if (host != null && host.length() > 0) {
                addr = new HotKeyServerAddr(host, port);
            } else {
                boolean preferredHostName = properties.isPreferredHostName();
                if (preferredHostName) {
                    host = InetAddress.getLocalHost().getHostName();
                    addr = new HotKeyServerAddr(host, port);
                } else {
                    String ignoredInterfaces = properties.getIgnoredInterfaces();
                    if (ignoredInterfaces != null) {
                        String[] interfaces = ignoredInterfaces.split(",");
                        Collections.addAll(InetUtils.ignoredInterfaces, interfaces);
                    }
                    String preferredNetworks = properties.getPreferredNetworks();
                    if (preferredNetworks != null) {
                        String[] networks = preferredNetworks.split(",");
                        Collections.addAll(InetUtils.preferredNetworks, networks);
                    }
                    InetAddress inetAddress = InetUtils.findFirstNonLoopbackAddress();
                    if (inetAddress != null) {
                        host = inetAddress.getHostAddress();
                    } else {
                        host = InetAddress.getLocalHost().getHostAddress();
                    }
                    addr = new HotKeyServerAddr(host, port);
                }
            }
        } catch (UnknownHostException e) {
            throw new ZkRegistryException(e);
        }
        registry = new ZkHotKeyServerRegistry(properties.getZkUrl(), properties.getSessionTimeoutMs(),
                properties.getConnectionTimeoutMs(), properties.getBaseSleepTimeMs(), properties.getMaxRetries(),
                properties.getBasePath(), applicationName, addr);

        registry.register();

        ServerStatus.registerOnlineCallback(this::register);
        ServerStatus.registerOfflineCallback(this::deregister);
    }

    public void register() {
        if (registry != null) {
            registry.register();
        }
    }

    public void deregister() {
        if (registry != null) {
            registry.deregister();
        }
    }

    public ZkHotKeyServerRegistry getRegistry() {
        return registry;
    }
}
